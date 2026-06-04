# Automatización mediante Event Scheduler — MySQL / Cloud SQL

## 4.1 Propósito

Identificar procesos del sistema que se benefician de automatización periódica
para mantener la base de datos ligera, los dashboards rápidos y la información
histórica depurada sin intervención manual.

## 4.2 Eventos implementados

### Evento 1: `ev_purge_audit_logs`

| Campo | Valor |
|-------|-------|
| **Nombre** | `ev_purge_audit_logs` |
| **Objetivo** | Depurar registros de auditoría operativa, tareas completadas, notificaciones enviadas y solicitudes de suministro con más de 90 días |
| **Proceso** | DELETE acotado (1000 filas/evento) en 4 tablas transaccionales; conserva solicitudes con movimientos de inventario relacionados |
| **Frecuencia** | Diario a las 03:00 (America/Mexico_City) |
| **Tablas afectadas** | `operational_recommendation_audit`, `operational_tasks`, `operational_notifications`, `supply_requests` |
| **Script** | `scripts/mysql/events/001_ev_purge_audit_logs.sql` |
| **Impacto esperado** | Reduce crecimiento de tablas transaccionales y acota el costo de backups/consultas de auditoría. Debe validarse con métricas reales de tamaño y latencia. |

### Evento 2: `ev_snapshot_daily_kpis`

| Campo | Valor |
|-------|-------|
| **Nombre** | `ev_snapshot_daily_kpis` |
| **Objetivo** | Materializar KPIs diarios de brotes epidemiológicos agrupados por estado y municipio para acelerar dashboards |
| **Proceso** | Agrega outbreaks activos en tabla `outbreak_daily_kpis` con caso total, sospechosos, confirmados y enfermedad principal |
| **Frecuencia** | Diario a las 05:00 (America/Mexico_City) |
| **Tablas origen** | `outbreaks`, `diseases`, `states`, `municipalities` |
| **Tabla destino** | `outbreak_daily_kpis` (se crea si no existe) |
| **Script** | `scripts/mysql/events/002_ev_snapshot_daily_kpis.sql` |
| **Impacto esperado** | Permite leer agregados diarios desde una tabla materializada en lugar de recalcular JOINs/agregaciones en cada request. La reducción exacta de latencia depende del volumen real. |

### Evento 3: `ev_rotate_snapshots`

| Campo | Valor |
|-------|-------|
| **Nombre** | `ev_rotate_snapshots` |
| **Objetivo** | Rotar snapshots de recursos hospitalarios anteriores a 30 días |
| **Proceso** | DELETE con límite de 500 filas por ejecución para evitar lock prolongado |
| **Frecuencia** | Semanal (domingo 04:00) |
| **Tablas afectadas** | `hospital_resource_snapshots` |
| **Script** | `scripts/mysql/events/003_ev_rotate_snapshots.sql` |
| **Impacto esperado** | Mantiene tabla de snapshots acotada y reduce el trabajo necesario para consultar snapshots recientes por hospital. |

## 4.3 Habilitación en Cloud SQL

En Cloud SQL for MySQL el Event Scheduler debe activarse mediante flag:

```bash
gcloud sql instances patch statusscope-db --database-flags event_scheduler=on
```

Verificar:
```sql
SELECT @@event_scheduler;
SELECT * FROM information_schema.events ORDER BY event_name;
```

## 4.4 Gráficas de impacto

> **Nota:** Las gráficas se generan después de 30+ días de operación continua.
> Se capturan las siguientes métricas antes/después:
> - Tamaño en MB de `operational_recommendation_audit` (semanal)
> - Tiempo de respuesta del dashboard admin p95 (diario)
> - Número de filas en `hospital_resource_snapshots` (semanal)

### Tabla de resultados esperados para validar

| Métrica | Sin Event Scheduler | Con Event Scheduler | Mejora |
|---------|-------------------|-------------------|--------|
| Tamaño audit (90 días) | Medir en `information_schema.tables` | Medir después de activar eventos | Esperado: reducción progresiva |
| Latencia dashboard admin p95 | Medir con Query Insights | Medir después de usar KPIs precalculados | Esperado: reducción |
| Filas snapshots por hospital | Contar por hospital | Contar después de rotación semanal | Esperado: estabilización |
