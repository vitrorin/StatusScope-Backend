# Documentación del Faker y Datos Sintéticos

## 6.1 Propósito del Faker y tablas alimentadas

**Propósito:** Generar datos de prueba realistas para el entorno de desarrollo
y demostración de StatuScope. La generación controlada permite validar flujos
completos (autenticación, dashboards, recomendaciones, diagnóstico asistido)
sin depender de producción ni de fuentes externas.

**Alcance:** Todo lo que NO son brotes epidemiológicos se genera sintéticamente.
Los brotes provienen del ETL real (gob.mx + GitHub Actions).

### Tablas alimentadas y volumen

| Tabla | Volumen actual (seed) | Regla de generación |
|-------|----------------------|---------------------|
| `hospitals` | 2 filas | 2 hospitales ficticios con datos realistas de Monterrey y CDMX |
| `users` | 5 filas | 1 system admin + 2 hospital admins + 2 doctores por hospital |
| `hospital_resource_snapshots` | 1 fila | Snapshot inicial del hospital 1 |
| `hospital_department_resources` | 5 filas | 5 departamentos con métricas de capacidad |
| `hospital_staffing_profiles` | 6 filas | Roles de personal con headcount y turnos |
| `hospital_inventory_items` | 6 filas | Insumos/equipos con cantidades y umbrales críticos |
| `operational_recommendations` | 3 filas | Recomendaciones con distintos niveles de severidad |
| `hospital_operational_contacts` | 3 filas | Contactos clave por departamento |
| `hospital_operational_groups` | 3 filas | Grupos de respuesta |
| `hospital_operational_group_members` | 3 filas | Asociación grupo-contacto |
| `hospital_inventory_movements` | 3 filas | Historial de movimientos de inventario |
| `diagnosis_assistant_fixtures` | 1-8 filas | Paciente, evaluación, archivo, tests, diferencial, brote, evento (opcional) |

El script adicional `scripts/mysql/faker/002_extend_faker_seed.sql` escala el
entorno de prueba con hospitales `HOSP-003` a `HOSP-010` y datos operativos
asociados. Está diseñado para ser re-ejecutable: evita insertar duplicados por
claves naturales como `hospital.code`, `department_code`, `role_code`,
`item_code`, rol de contacto y tipo/título de recomendación.

## 6.2 Flujo de carga y transformación

### Generación de datos sintéticos

El sistema usa dos mecanismos:

1. **import.sql** (carga inicial vía Hibernate `sql-load-script`):
   - Inserciones directas SQL con IDs UUID fijos y predecibles
   - Se ejecuta en desarrollo local destructivo y en `test`; el perfil `prod`
     usa MySQL/Cloud SQL, valida el esquema y no carga `import.sql`
   - Contiene: estados, especialidades, roles, privilegios, hospitales,
     datos operativos de ejemplo

2. **DevSeeder.java** (bootstrap condicional vía evento `StartupEvent`):
   - Ejecuta SOLO en perfiles `dev` y `test`
   - Crea usuarios reales en Firebase Auth (contraseña: "Password123!")
   - Inserta registros en `users` y `user_roles`
   - Si `seedDiagnosisAssistantDemoData=true`, crea fixtures completos
     para el asistente de diagnóstico (paciente, evaluación, archivos,
     tests, diagnósticos diferenciales)

### Validación de calidad

- **Idempotencia:** DevSeeder verifica si el usuario semilla ya existe
   (`userRepository.findByEmail("admin@statusscope.local").isPresent()`)
   antes de ejecutar, permitiendo reinicios sin duplicados.
- **Idempotencia del Faker SQL extendido:** `002_extend_faker_seed.sql` usa
   filtros `NOT EXISTS` para evitar duplicados al re-ejecutarse en desarrollo.
- **Firebase rollback:** Si el insert en DB falla tras crear el usuario
  en Firebase, se elimina el usuario de Firebase (compensación).
- **IDs fijos:** Todos los IDs semilla son UUIDs constantes con prefijos
  por tipo de entidad, facilitando depuración.

## 6.3 Limpieza de datos

### Inconsistencias y valores nulos

| Problema | Estrategia aplicada | Localización |
|----------|---------------------|--------------|
| Filas sin `municipality_id` | Skip con contador | `combinar_outbreaks_municipales.py:73-75` |
| Fechas nulas o "9999-99-99" | Tratadas como `None` | `outbreaks.py:35-37` |
| Formato de fechas inconsistente | Prueba múltiples formatos (`%Y-%m-%d`, `%d/%m/%Y`) | `combinar_outbreaks_municipales.py:147-149` |
| Municipios sin coordenadas | Filtrados en generación de mapas | `GetDoctorDashboardSummaryUseCase.java:107-108` |
| Nombres de enfermedad sin match en DB | Matching por equivalencias exactas, alias y categorías amplias | `filtrar_enfermedades_relevantes.py:244-288` |

### Homogeneización de formatos

- **Unicode:** Normalización NFD + eliminación de diacríticos
  (`outbreaks.py:21-24`)
- **Mayúsculas:** Normalización a mayúsculas sin acentos para matching
  (`outbreaks.py:24`)
- **Alias geográficos:** Mapa de correcciones para nombres de municipio
  con variantes ortográficas (`outbreaks.py:13-18`)
- **Códigos INEGI:** Relleno con ceros a la izquierda (state_code.zfill(2),
  municipality_code.zfill(3)) (`outbreaks.py:89-90`)

### Eliminación de redundancias

- Los brotes municipales se **agrupan por clave compuesta**
  `(municipality_id, disease_key, confirmation_status)` sumando casos
  en lugar de insertar filas duplicadas (`combinar_outbreaks_municipales.py:80-104`)
- Los brotes estatales se agrupan por `(estado, disease_name)` sumando
  casos del boletín semanal (`filtrar_enfermedades_relevantes.py:291-319`)
- Las enfermedades del boletín que matchean con la misma enfermedad en DB
  se consolidan bajo un solo nombre normalizado

### Documentación del pipeline ETL (brotes reales)

Ver: `tools/ingesta-datos/README.md`

```
GitHub Actions (cron semanal jueves 8:00 MX)
  |
  v
actualizar_outbreaks.ps1
  |
  +-- municipal: descargar_datos_abiertos.py
  |                -> extraer_respiratorias.py / extraer_febriles_exantematicas.py / extraer_dengue.py
  |                -> combinar_outbreaks_municipales.py (agrupación)
  |
  +-- estatal: descargar_boletin_semanal.py
  |              -> extraer_pdf_boletin.py (pypdf)
  |              -> filtrar_enfermedades_relevantes.py (matching + agrupación)
  |
  +-- Publicar CSVs a src/main/resources/data/outbreaks/
  +-- Commit automático si hay cambios
```

Los CSV resultado se cargan en MySQL mediante Flyway data migration
(`V4__seed_outbreaks.java`) que usa `PreparedStatement.executeBatch()`
para inserción eficiente.
