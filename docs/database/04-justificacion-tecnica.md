# Justificación técnica − Decisiones de implementación

## Arquitectura global (GCP + Contenedor + GitHub Actions + Cloud SQL)

```
GitHub Actions (cron diario)
  |
  +-- ETL brotes (Python) → CSV → commit
  |
  v
Artifact Registry (imagen Quarkus)
  |
  v
Cloud Run / GKE
  |
  v
Cloud SQL for MySQL (event_scheduler=ON)
  ├── Event Scheduler (purga, KPIs, snapshots)
  ├── Índices compuestos
  └── Performance Schema (observabilidad)
```

### ¿Por qué Event Scheduler de MySQL y no el scheduler de Quarkus?

| Aspecto | MySQL Event Scheduler | Quarkus Scheduler |
|---------|----------------------|-------------------|
| **Dependencia** | Solo DB | App running + conexión DB |
| **Latencia de ejecución** | Milisegundos | Latencia de red + JVM |
| **Disponibilidad** | Siempre que Cloud SQL está up | Solo si el contenedor está activo (mínimo 1 réplica) |
| **Caso de uso ideal** | Limpieza, agregados DB-internos | Lógica que requiere contexto externo (API calls, IA) |

**Decisión:** Tareas que operan 100% dentro de MySQL (DELETE, INSERT
precalculado, mantenimiento) → Event Scheduler. Tareas que requieren
llamadas a APIs externas (OpenAI, Gemini) o lógica Java → Quarkus Scheduler.

### ¿Por qué GitHub Actions para ETL y no un servicio 24/7?

| Aspecto | GitHub Actions | Servicio dedicado |
|---------|---------------|-------------------|
| **Costo** | Incluido en el plan, ~2000 min/mes gratis | Instancia 24/7 en Cloud Run mínimo $15/mes |
| **Trigger** | Cron + workflow_dispatch manual | Siempre activo (overkill para tarea diaria de 2 min) |
| **Aprovisionamiento** | Zero mantenimiento de infra | Mantener contenedor, logs, monitoreo |

**Decisión:** Para una tarea diaria de 2-5 minutos de ejecución, GitHub
Actions es la opción más costo-eficiente y simple.

### ¿Por qué estos índices y no otros?

**Principio rector:** "equality first, then range/sort"
1. Columnas de filtro exacto (WHERE col = ?) van primero en el índice.
2. Columnas de rango/orden (ORDER BY, BETWEEN) van después.
3. El índice compuesto evita filesort y reduce rows examined.

**Validación requerida:**
- Ejecutar `scripts/mysql/benchmark/001_critical_queries_explain.sql` antes y
  después de aplicar `scripts/mysql/indexes/001_optimization_indexes.sql`.
- Registrar `rows examined`, uso de índice, presencia de filesort y latencia real
  en `docs/database/02-optimizacion-consultas.md`.

### ¿Por qué la tabla `outbreak_daily_kpis` en lugar de usar la vista?

- Una vista ejecuta los JOINs y agregaciones en cada SELECT.
- Una tabla materializada (actualizada 1 vez al día por Event Scheduler)
  permite lectura directa con latencia < 5 ms.
- Cloud SQL no soporta `CREATE MATERIALIZED VIEW` nativo; el Event Scheduler
  es el mecanismo más simple para lograrlo.

### Gestión de secretos y conexión a Cloud SQL

| Secreto | Gestión | Acceso |
|---------|---------|--------|
| DB_USER / DB_PASS | Secret Manager | Cloud Run via mount / env |
| Firebase SA | Secret Manager | Montado como archivo JSON |
| OpenAI / Gemini API keys | Secret Manager | Cloud Run env vars |
| DB conexión | Cloud SQL Auth Proxy | Sidecar en Cloud Run |

---

## Resumen de archivos generados

```
scripts/mysql/
  events/
    001_ev_purge_audit_logs.sql
    002_ev_snapshot_daily_kpis.sql
    003_ev_rotate_snapshots.sql
    004_enable_events.sql
  indexes/
    001_optimization_indexes.sql
  benchmark/
    001_critical_queries_explain.sql
    002_load_test_procedures.sql
  faker/
    README-faker.md
    002_extend_faker_seed.sql
docs/database/
  01-automatizacion-event-scheduler.md
  02-optimizacion-consultas.md
  03-observabilidad.md
    04-justificacion-tecnica.md           ← este archivo
```

Total actual: 13 archivos revisados en este paquete (8 SQL + 5 Markdown).
