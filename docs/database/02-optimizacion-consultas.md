# Optimización de consultas y rendimiento

## 5.1 Identificación de tablas críticas

| Tabla | ¿Por qué es crítica? | Riesgos de rendimiento | Estrategia de mitigación |
|-------|---------------------|-----------------------|--------------------------|
| **users** | Se consulta en **cada request autenticado** vía `external_auth_id`. JOINs con roles y privilegios en cada llamada. | Sin índice en `external_auth_id` → full scan. JOINs Eager (`left join fetch`) traen toda la jerarquía RBAC aunque no se use. | Índice UNIQUE en `external_auth_id` (ya existe). Cambiar JOINs a lazy loading controlado donde sea posible. |
| **outbreaks** | Base del radar epidemiológico. Filtrada por `status`, `scope`, `municipality_id` y `state_id` en consultas de dashboard, mapas y reportes. | Sin índices compuestos para filtros combinados. Full scan y posible filesort en volúmenes altos. | Índices compuestos `(status, scope, municipality_id)` y `(status, scope, state_id)`. Validar `ORDER BY case_count` con `EXPLAIN`; puede requerir índice adicional si domina la latencia. |
| **operational_recommendations** | Feed principal del dashboard admin. Consultada por hospital + filtros de estado/severidad + ORDER BY fecha. | Filesort en ORDER BY created_at. Index scan completo por hospital aunque solo se necesiten 5 filas. | Índices `(hospital_id, created_at DESC)`, `(hospital_id, status, created_at DESC)` y `(hospital_id, severity, created_at DESC)`. `status NOT IN` puede seguir requiriendo filesort; validar con `EXPLAIN ANALYZE`. |
| **hospital_resource_snapshots** | Consulta de último snapshot por hospital en cada carga de dashboard admin. | Sin índice descendente en `captured_at` → recorre todos los snapshots del hospital. | Índice `(hospital_id, captured_at DESC)` + LIMIT 1. |

## 5.2 Análisis de consultas representativas

### Consulta A: Autenticación de usuarios

```sql
-- Frecuencia: cada request API autenticado
EXPLAIN ANALYZE
SELECT u.*, r.*, p.*
FROM users u
LEFT JOIN user_roles ur ON ur.user_id = u.id
LEFT JOIN roles r ON r.id = ur.role_id
LEFT JOIN role_privileges rp ON rp.role_id = r.id
LEFT JOIN privileges p ON p.id = rp.privilege_id
WHERE u.external_auth_id = ?;
```

**Análisis:** El `left join fetch` de Panache trae toda la jerarquía de roles
y privilegios (4 JOINs) para construir el `CurrentUser`. Con el UNIQUE index
en `external_auth_id` es rápido (`type: const`), pero el volumen de datos
transferido es innecesario para requests que solo verifican autenticación.

**Propuesta:** Crear una consulta ligera de solo verificación (`SELECT 1 FROM users WHERE external_auth_id = ? AND status = 'ACTIVE'`) para el filtro de autenticación, y mantener la consulta completa solo para el endpoint `/auth/me`.

### Consulta B: Brotes activos por estado

```sql
-- Frecuencia: dashboard de doctor y admin
EXPLAIN ANALYZE
SELECT o.*, m.name, d.name
FROM outbreaks o
JOIN municipalities m ON m.id = o.municipality_id
JOIN diseases d ON d.id = o.disease_id
WHERE o.status = 'ACTIVE'
  AND o.scope = 'MUNICIPALITY'
  AND m.state_id = ?
ORDER BY o.case_count DESC;
```

**Resultado esperado sin índice compuesto:**
- `type: ALL` en outbreaks
- `rows: ~10,000`
- `Extra: Using where; Using filesort`

**Resultado esperado con `idx_outbreaks_status_scope_municipality`:**
- `type: ref` en outbreaks
- `rows: ~50`
- `Extra: Using index condition`; el orden por `case_count` puede requerir filesort

### Consulta C: Feed de recomendaciones (dashboard admin)

```sql
-- Frecuencia: cada carga del dashboard admin
EXPLAIN ANALYZE
SELECT id, type, severity, status, title, description,
       confidence_score, created_at
FROM operational_recommendations
WHERE hospital_id = ?
  AND status NOT IN ('COMPLETED', 'REJECTED')
ORDER BY created_at DESC
LIMIT 5;
```

**Resultado esperado con índices de recomendaciones:**
- `type: range`
- Menos filas examinadas para hospital/status/severidad
- `Extra: Using index condition; Using where`

## 5.3 Estrategias de optimización

### Índices implementados

| Índice | Columnas | Tipo | Justificación |
|--------|----------|------|---------------|
| `idx_outbreaks_status_scope_municipality` | `(status, scope, municipality_id)` | BTREE | Filtro por `status='ACTIVE' AND scope='MUNICIPALITY' AND municipality_id IN (...)` |
| `idx_outbreaks_status_scope_state` | `(status, scope, state_id)` | BTREE | Filtro por `status='ACTIVE' AND scope='STATE' AND state_id=?` |
| `idx_recs_hospital_status_created` | `(hospital_id, status, created_at DESC)` | BTREE | Feed admin por hospital + estado + orden descendente |
| `idx_recs_hospital_severity_created` | `(hospital_id, severity, created_at DESC)` | BTREE | Feed admin por hospital + severidad |
| `idx_snapshots_hospital_captured` | `(hospital_id, captured_at DESC)` | BTREE | Último snapshot por hospital con LIMIT 1 |
| `idx_dept_hospital_status` | `(hospital_id, status)` | BTREE | Departamentos por hospital y estado de carga |
| `idx_inventory_hospital_category` | `(hospital_id, category, item_name)` | BTREE | Inventario por hospital ordenado por categoría |
| `idx_staff_hospital_role` | `(hospital_id, role_name)` | BTREE | Personal por hospital ordenado por rol |
| `idx_patients_hospital_name` | `(hospital_id, full_name)` | BTREE | Búsqueda de pacientes por hospital + nombre |
| `idx_audit_recommendation_created` | `(recommendation_id, created_at ASC)` | BTREE | Auditoría por recomendación orden cronológico |

### Optimización de JOINs

Se identificaron dos patrones de JOIN que pueden optimizarse:

1. **UserRepositoryImpl — LEFT JOIN FETCH en listados**: todas las consultas
   usan `left join fetch u.roles r left join fetch r.privileges` incluso cuando
   solo se necesita el email o el nombre. Propuesta: añadir métodos específicos
   sin fetch para listados de solo lectura.

2. **OutbreakRepositoryImpl — JOIN con diseases**: actualmente hace
   `join fetch o.disease d left join fetch d.symptoms`. Si los síntomas no
   se necesitan en la respuesta (dashboard), cambiar a `join fetch` simple.

### Reducción de columnas

| Consulta original | Problema | Optimización |
|------------------|----------|-------------|
| `GetDoctorDashboardSummaryUseCase` | Carga outbreaks completos con todas las columnas | Proyectar solo `id, case_count, disease, scope, status, started_at` |
| `GetAdminDashboardSummaryUseCase` | Carga recomendaciones completas | Proyectar solo `id, type, severity, status, title, created_at, confidence_score` |

### Paginación

| Consulta | Antes | Después |
|----------|-------|---------|
| Feed recomendaciones admin | `findByHospitalId()` sin límite (40+ filas) | `findByHospitalIdWithStatus()` con `status NOT IN (...)` + `LIMIT 5` |
| Dashboard doctor disease breakdown | `List<Outbreak>` completo | `LIMIT 10` con orden por `case_count DESC` |
| Tabla de enfermedades (diagnosis assistant) | Sin límite | `LIMIT 12` (configurable hasta 50) |

## 5.4 Evidencia técnica

### Tabla comparativa a completar con mediciones reales

| Consulta | Sin índices | Con índices | Mejora |
|----------|-------------|-------------|--------|
| Brotes por estado | Medir con `EXPLAIN ANALYZE` | Medir tras índices | Calcular |
| Feed recomendaciones | Medir con `EXPLAIN ANALYZE` | Medir tras índices | Calcular |
| Último snapshot | Medir con `EXPLAIN ANALYZE` | Medir tras índices | Calcular |
| Búsqueda pacientes | Medir con `EXPLAIN ANALYZE` | Medir tras índices | Calcular |

### Pruebas de carga (cliente externo requerido)

| Nivel concurrencia | Consulta | p50 (sin índice) | p50 (con índice) | p95 (sin índice) | p95 (con índice) |
|-------------------|----------|-----------------|-----------------|-----------------|-----------------|
| 1 usuario | Brotes por estado | Pendiente | Pendiente | Pendiente | Pendiente |
| 10 usuarios | Brotes por estado | Pendiente | Pendiente | Pendiente | Pendiente |
| 25 usuarios | Feed recomendaciones | Pendiente | Pendiente | Pendiente | Pendiente |
| 50 usuarios | Último snapshot | Pendiente | Pendiente | Pendiente | Pendiente |
| 100 usuarios | Feed recomendaciones | Pendiente | Pendiente | Pendiente | Pendiente |

### Justificación de decisiones

1. **Índices compuestos vs individuales**: Los índices compuestos con
   `(equality_column, range/order_column)` son más eficientes que índices
   separados porque InnoDB puede hacer index seek + index scan en una sola
   estructura, evitando "index merge union".

2. **DESC en índices de fecha**: MySQL 8 puede recorrer índices en orden
   inverso, pero tener el índice físicamente DESC evita la sobrecarga del
   optimizador y garantiza que `ORDER BY col DESC LIMIT N` sea un simple
   recorrido hacia adelante.

3. **Límite de 3 columnas por índice**: Minimiza write amplification en
   tablas de alta inserción como `hospital_resource_snapshots` y
   `operational_recommendations`.

4. **No sobre-indexar tablas pequeñas**: Las tablas catálogo
   (`states`, `specialties`, `roles`) tienen pocas filas y no necesitan
   índices adicionales más allá de su PK.
