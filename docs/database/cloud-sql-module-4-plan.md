# Modulo 4 - Base de Datos en Google Cloud SQL

## Alcance

Este plan adapta los requisitos de Bases de Datos Avanzadas al despliegue previsto del backend de StatusScope en un contenedor de Google Cloud, usando Cloud SQL for MySQL como base de datos persistente.

El despliegue real no se ejecuta en esta etapa. La pipeline queda preparada para ejecucion manual y para documentar el flujo esperado de entrega.

## Arquitectura Objetivo

```text
GitHub Actions - ingesta programada
  -> descarga y procesa datos epidemiologicos
  -> actualiza CSVs versionados en src/main/resources/data/outbreaks
  -> commit automatico si hay cambios

GitHub Actions - pipeline de despliegue manual
  -> ejecuta pruebas del backend
  -> construye imagen Quarkus/Jib
  -> publica imagen en Artifact Registry
  -> despliega contenedor a Cloud Run
  -> Cloud Run se conecta a Cloud SQL MySQL
  -> Flyway aplica migraciones
  -> OutbreakIngestionRunner importa CSVs actualizados a MySQL
```

## Servicios de Google Cloud

| Servicio | Uso |
| --- | --- |
| Cloud SQL for MySQL | Base de datos persistente del proyecto final |
| Cloud Run | Ejecucion del backend Quarkus en contenedor |
| Artifact Registry | Repositorio de imagenes del backend |
| Secret Manager | Credenciales de MySQL, Firebase y llaves externas |
| IAM Workload Identity Federation | Autenticacion segura desde GitHub Actions sin llaves JSON largas |

## Cambios Requeridos Para Produccion

La configuracion productiva debe usar MySQL y no H2. El perfil de produccion recomendado es:

```properties
%prod.quarkus.datasource.db-kind=mysql
%prod.quarkus.datasource.username=${QUARKUS_DATASOURCE_USERNAME}
%prod.quarkus.datasource.password=${QUARKUS_DATASOURCE_PASSWORD}
%prod.quarkus.datasource.jdbc.url=${QUARKUS_DATASOURCE_JDBC_URL}
%prod.quarkus.hibernate-orm.schema-management.strategy=validate
%prod.quarkus.hibernate-orm.sql-load-script=no-file
%prod.quarkus.flyway.migrate-at-start=false
```

`drop-and-create` no debe usarse en Cloud SQL porque destruiria datos reales. Las migraciones deben controlarse con Flyway.

## Variables Para Cloud Run

| Variable | Proposito |
| --- | --- |
| `QUARKUS_DATASOURCE_USERNAME` | Usuario de Cloud SQL |
| `QUARKUS_DATASOURCE_PASSWORD` | Password de Cloud SQL desde Secret Manager |
| `QUARKUS_DATASOURCE_JDBC_URL` | URL JDBC de Cloud SQL MySQL |
| `FIREBASE_SA_PATH` | Ruta o configuracion de credenciales Firebase |
| `OUTBREAK_INGESTION_IMPORT_AT_START` | Activa importacion de CSVs al iniciar |
| `ADMIN_RECOMMENDATIONS_SCHEDULER_ENABLED` | Controla scheduler interno de recomendaciones |

Para Cloud Run con Cloud SQL, la URL JDBC puede usar conexion TCP privada o el conector/socket configurado por Cloud Run. La decision depende de la red del proyecto.

## Proceso Actual de Recoleccion

El workflow `.github/workflows/update-outbreaks.yml` ya cubre la recoleccion semanal:

| Paso | Descripcion |
| --- | --- |
| Schedule | Jueves 14:00 UTC |
| Runner | `windows-latest` |
| Entrada | Fuentes oficiales de datos epidemiologicos |
| Salida | CSVs en `src/main/resources/data/outbreaks` |
| Persistencia | Commit automatico si los CSVs cambian |

Los CSVs no escriben directamente en Cloud SQL. La aplicacion los importa al arrancar mediante `OutbreakIngestionRunner` y `OutbreakCsvImporter`.

## Riesgo Operativo de la Ingesta

Cloud Run puede levantar mas de una instancia. Si varias instancias arrancan al mismo tiempo, todas podrian intentar importar los CSVs.

Mitigaciones actuales:

- El importer usa llaves deterministicas para brotes gestionados.
- El proceso actualiza registros existentes cuando encuentra la misma llave logica.
- Los datos fuente estan versionados en Git, por lo que el contenedor siempre contiene una foto reproducible.

Mejora recomendada:

- Mantener la importacion idempotente.
- Documentar que `OUTBREAK_INGESTION_IMPORT_AT_START=true` es aceptable para demo y primera entrega.
- Para produccion madura, mover la importacion a un Cloud Run Job disparado por la pipeline de ingesta.

## Requisitos del Modulo 4

### Modelo Logico

Documentar entidades y cardinalidades principales:

| Area | Entidades |
| --- | --- |
| Seguridad | `users`, `roles`, `privileges`, `user_roles`, `role_privileges` |
| Geografia | `states`, `municipalities`, `hospitals` |
| Epidemiologia | `diseases`, `symptoms`, `disease_symptoms`, `outbreaks`, `alerts` |
| Diagnostico | `patients`, `patient_evaluations`, `diagnosis_assistant_threads`, `diagnosis_assistant_messages`, `diagnosis_assistant_suggestions` |
| Operacion hospitalaria | `hospital_resource_snapshots`, `hospital_inventory_items`, `hospital_inventory_movements`, `operational_recommendations`, `operational_tasks`, `supply_requests` |

### Modelo Fisico

El modelo fisico debe extraerse de:

- Entidades JPA en `src/main/java/com/itesm/infrastructure/persistence/entity`.
- Migraciones SQL en `src/main/resources/db/migration`.
- Migraciones Java Flyway en `src/main/java/db/migration`.
- Script de bootstrap MySQL en `scripts/mysql/dev_bootstrap_seed.sql`.

### Programacion Dentro de MySQL

Se debe agregar una migracion nueva, por ejemplo `V8__advanced_database_module.sql`, con:

| Requisito | Implementacion propuesta |
| --- | --- |
| 2 Stored Procedures | Crear solicitud de insumos con movimiento; generar resumen operativo por hospital |
| 2 funciones almacenadas | Calcular ocupacion de camas; calcular estado de inventario |
| 3 triggers | Validar inventario antes de insertar; validar inventario antes de actualizar; auditar cambios de recomendaciones |
| Event Scheduler | Marcar recomendaciones expiradas o generar auditoria diaria |

## Indices Recomendados Para Cloud SQL

Estos indices atacan consultas reales del backend y ayudan cuando Cloud Run incrementa concurrencia:

```sql
CREATE INDEX idx_outbreaks_status_scope_municipality
ON outbreaks (status, scope, municipality_id);

CREATE INDEX idx_outbreaks_status_scope_state
ON outbreaks (status, scope, state_id);

CREATE INDEX idx_outbreaks_disease_status_started
ON outbreaks (disease_id, status, started_at);

CREATE INDEX idx_operational_recommendations_hospital_status_created
ON operational_recommendations (hospital_id, status, created_at);

CREATE INDEX idx_operational_recommendations_hospital_severity_created
ON operational_recommendations (hospital_id, severity, created_at);

CREATE INDEX idx_hospital_resource_snapshots_hospital_captured
ON hospital_resource_snapshots (hospital_id, captured_at);

CREATE INDEX idx_inventory_movements_item_created
ON hospital_inventory_movements (inventory_item_id, created_at);
```

## Iteraciones de Mejora

Las 3 iteraciones pueden demostrarse contra Cloud SQL o un MySQL local equivalente.

| Iteracion | Prueba | Mejora | Evidencia |
| --- | --- | --- | --- |
| 1 | 10 usuarios consultando dashboard y brotes | Indices compuestos en `outbreaks` | `EXPLAIN`, tiempo promedio antes/despues |
| 2 | 50 usuarios consultando recursos y recomendaciones | Indices por hospital, estado y fecha | Grafica de latencia p95 antes/despues |
| 3 | 100 usuarios ejecutando flujos de inventario/recomendaciones | Stored procedures, functions y triggers | Comparacion de operaciones por segundo y errores |

Herramientas sugeridas:

- `k6` para carga HTTP contra Cloud Run o backend local.
- `EXPLAIN ANALYZE` en MySQL 8 para consultas criticas.
- Logs de Cloud Run para latencia y errores.
- Metricas de Cloud SQL para CPU, conexiones y consultas lentas.

## Pipeline de Despliegue Requerida

La pipeline debe existir, pero puede quedar manual para evitar despliegues accidentales durante desarrollo.

Se agrego el workflow manual `.github/workflows/deploy-cloud-run.yml`. No se ejecuta con `push`; solo corre desde `workflow_dispatch`.

Requisitos de la pipeline:

- Autenticarse a Google Cloud por Workload Identity Federation.
- Ejecutar `mvnw test`.
- Construir imagen con Quarkus Jib usando el perfil `container-image`.
- Publicar imagen a Artifact Registry.
- Ejecutar `gcloud run deploy` solo cuando se dispare manualmente.
- Conectar Cloud Run a la instancia Cloud SQL.
- Inyectar variables y secretos de entorno.

Variables de repositorio requeridas para activar la pipeline:

| Variable de GitHub | Uso |
| --- | --- |
| `GCP_PROJECT_ID` | Proyecto de Google Cloud |
| `GCP_REGION` | Region de Cloud Run y Artifact Registry |
| `ARTIFACT_REGISTRY_REPOSITORY` | Repositorio de imagenes |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | Proveedor de Workload Identity Federation |
| `GCP_SERVICE_ACCOUNT` | Service account usado por GitHub Actions |
| `CLOUD_SQL_INSTANCE_CONNECTION_NAME` | Nombre de conexion de Cloud SQL |
| `QUARKUS_DATASOURCE_JDBC_URL` | URL JDBC hacia Cloud SQL MySQL |
| `DB_USERNAME_SECRET` | Nombre del secreto con usuario MySQL |
| `DB_PASSWORD_SECRET` | Nombre del secreto con password MySQL |
| `FIREBASE_SA_SECRET` | Nombre del secreto con el JSON de Firebase Service Account |

La pipeline monta el secreto de Firebase como archivo en `/secrets/firebase/firebase-service-account.json` y configura `FIREBASE_SA_PATH` con esa ruta, porque el backend actual espera leer la credencial desde archivo.

## Orden Recomendado

1. Crear migracion `V8__advanced_database_module.sql` con indices y elementos programados.
2. Cambiar `%prod` a MySQL/Cloud SQL cuando ya exista la instancia.
3. Completar diccionario de datos y modelo logico/fisico.
4. Mantener workflow de ingesta actual.
5. Usar pipeline manual de despliegue cuando el equipo tenga Artifact Registry, Cloud Run y Cloud SQL configurados.
6. Ejecutar pruebas de carga y documentar 3 iteraciones con graficas.
