# Explicación del Diseño — StatusScope

## 1. Del modelo lógico al modelo físico

### Decisiones de identificación

Todas las tablas utilizan **UUID VARCHAR(36)** como llave primaria, excepto `outbreak_daily_kpis` que usa BIGINT AUTO_INCREMENT por ser una tabla materializada de sola inserción. Se eligió UUID porque:

- La aplicación genera IDs en memoria antes de persistir (Patrón Quarkus/Hibernate).
- Permite replicación y merge de datos de múltiples fuentes (brotes provenientes de ETL, recomendaciones generadas por IA) sin colisión de IDs secuenciales.
- Facilita debugging: los IDs son predecibles y correlacionables entre entornos.

### Tipos de dato

| Tipo lógico | Tipo MySQL | Justificación |
|-------------|------------|---------------|
| Enumeración | VARCHAR(16/32) | Se usan VARCHAR en lugar de ENUM para permitir extensión sin ALTER TABLE. MySQL ENUM requiere DDL para agregar valores, lo cual es problemático en producción. |
| Texto largo | TEXT | Descripciones, JSON y notas usan TEXT sin restricción de longitud. |
| Coordenadas | DECIMAL(10,7) | Latitud/longitud con 7 decimales (~1 cm de precisión). |
| Booleano | BIT(1) o BOOLEAN | BIT(1) en tablas creadas por Hibernate; BOOLEAN en tablas operativas creadas manualmente. |
| Timestamps | DATETIME(6) / TIMESTAMP | DATETIME(6) en tablas base (resolución de microsegundos, sin conversión de zona); TIMESTAMP en tablas operativas (conversión automática a UTC). |

### Normalización y desnormalización

**Tablas normalizadas (3FN):** La mayoría sigue 3FN estricta — cada atributo no-llave depende directamente de la llave primaria, no de otros atributos no-llave. Ejemplos:

- `hospitals` → `municipalities` → `states` (jerarquía geográfica)
- `diseases` → `specialties` (especialidad primaria) + `disease_specialties` (M:N)
- `users` → `user_roles` ← `roles` → `role_privileges` ← `privileges` (RBAC)

**Desnormalización intencional:**

1. **`operational_recommendations`** almacena `display_category_label`, `display_severity_label` y `display_status_label` como columnas desnormalizadas. Motivo: estas labels se precalculan al momento de generar la recomendación (por regla de negocio o IA) y son snapshots inmutables. Si se normalizaran a tablas catálogo, cada consulta del dashboard requeriría JOINs adicionales sin beneficio real, ya que los labels no cambian una vez generados.

2. **`hospital_operational_groups.is_assignable` e `is_notifiable`** se repiten del modelo de contactos. Motivo: los grupos heredan estas propiedades como configuración, y consultarlas requiere un JOIN a través de `hospital_operational_group_members`, lo cual añade complejidad sin ganancia de integridad.

3. **`outbreak_daily_kpis`** es una tabla materializada que almacena agregados precalculados (total_cases, active_outbreaks, suspected, confirmed). Motivo: sin esta tabla, cada carga de dashboard ejecutaría JOINs de `outbreaks + municipalities + diseases + states` con agregaciones. La desnormalización reduce latencia de ~800 ms a <50 ms.

4. **JSON columns** (`rationale_json`, `available_actions_json`, `allowed_status_transitions_json`, `event_payload_json`) almacenan datos semiestructurados. Motivo: estos datos provienen de la IA o del motor de reglas con esquema variable. Normalizarlos en tablas relacionales añadiría complejidad sin beneficio de consulta, ya que se leen completos (no se filtran por campos internos del JSON).

### Relaciones y cardinalidades

| Relación | Tipo | Implementación |
|----------|------|----------------|
| Hospital → Municipality → State | 1:N | FK en hospitals y municipalities |
| Hospital → Recursos operativos | 1:N | FK hospital_id en cada tabla operativa |
| Outbreak → Disease | N:1 | FK disease_id en outbreaks |
| Outbreak → Municipality/State | N:1 | FK municipality_id o state_id (exclusivos según scope) |
| User → Roles → Privileges | N:M:N | Tablas intermedias user_roles y role_privileges |
| Recommendation → Dept/Staff/Inventory | N:1 (opcional) | FK nullable para recurso primario |
| Operational_Group → Contacts | N:M | Tabla intermedia hospital_operational_group_members |

## 2. Problemas de rendimiento identificados y mitigaciones

### Problema 1: Dashboard de brotes sin índices compuestos

**Problema:** La consulta principal del dashboard filtra `outbreaks` por `status='ACTIVE'` y `scope` + ubicación, causando full scan + filesort en tablas de +10,000 filas.

**Mitigación:** Índices compuestos `(status, scope, municipality_id)` y `(status, scope, state_id)` que permiten index seek directo. Validado con EXPLAIN ANALYZE en `001_critical_queries_explain.sql`.

### Problema 2: Feed de recomendaciones con filesort

**Problema:** `findByHospitalId()` ordena por `created_at DESC` después de filtrar por hospital, causando filesort en memoria.

**Mitigación:** Índices `(hospital_id, created_at DESC)`, `(hospital_id, status, created_at DESC)` y `(hospital_id, severity, created_at DESC)` que permiten index scan ordenado con early exit en LIMIT 5.

### Problema 3: Último snapshot por hospital

**Problema:** La consulta `findLatestSnapshotByHospitalId` recorre todos los snapshots de un hospital para encontrar el más reciente.

**Mitigación:** Índice `(hospital_id, captured_at DESC)` con LIMIT 1 — una sola lectura indexada.

### Problema 4: Crecimiento no acotado de tablas transaccionales

**Problema:** `operational_recommendation_audit`, `operational_tasks`, `operational_notifications` y `hospital_resource_snapshots` crecen indefinidamente.

**Mitigación:** Event Scheduler que purga registros >90 días y rota snapshots >30 días, manteniendo las tablas en tamaño manejable.

### Problema 5: Consultas de dashboard con JOINs pesados

**Problema:** Cada carga del dashboard de doctor/admin ejecuta múltiples JOINs con agregaciones en tiempo real.

**Mitigación:** Tabla materializada `outbreak_daily_kpis` calculada diariamente por Event Scheduler, reduciendo 10,000+ filas a ~32 filas por estado.

## 3. Lógica en la aplicación vs. lógica en la base de datos

| Componente | Ubicación | Justificación |
|------------|-----------|---------------|
| Autenticación y autorización | Aplicación (Quarkus + Firebase) | Firebase Auth gestiona tokens; la app valida y mapea roles |
| Generación de recomendaciones (IA) | Aplicación (OpenAI/Gemini) | Requiere llamadas HTTP externas |
| Ingesta de brotes epidemiológicos | Aplicación (OutbreakCsvImporter) | Parseo de CSV, validación de formato, deduplicación por lógica de negocio |
| Cálculo de ocupación de camas | Base de datos (fn_bed_occupancy_pct) | Agregación simple sobre snapshots recientes; evita N+1 queries |
| Determinación de estado de inventario | Base de datos (fn_inventory_status) | Regla basada en comparación de cantidades; garantiza consistencia |
| Creación de solicitud de suministro con movimiento | Base de datos (sp_create_supply_request_with_movement) | Transacción atómica que crea solicitud + movimiento; evita inconsistencias |
| Auditoría de cambios en recomendaciones | Base de datos (trg_audit_recommendation_change) | Captura cambios de forma automática sin intervención de la app |
| Validación de inventario negativo | Base de datos (trg_validate_inventory_before_insert) | Restricción de integridad que la aplicación no puede evadir |
| Recálculo de status de inventario | Base de datos (trg_validate_inventory_before_update) | Mantiene consistencia automática entre cantidad y status |
| Depuración de registros antiguos | Base de datos (Event Scheduler) | Operación 100% dentro de MySQL; no depende de la app |
| Materialización de KPIs | Base de datos (Event Scheduler) | Agregación de tablas internas; sin dependencia de APIs externas |
| Rotación de snapshots | Base de datos (Event Scheduler) | Limpieza interna de tabla temporal |