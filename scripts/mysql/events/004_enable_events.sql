-- ============================================================================
-- Habilitación de Event Scheduler en MySQL / Cloud SQL
-- ============================================================================
-- En MySQL local, ejecutar con usuario con privilegio SYSTEM_VARIABLES_ADMIN:
--   SET GLOBAL event_scheduler = ON;

-- En Cloud SQL for MySQL:
-- Verificar estado actual:
--   SELECT @@event_scheduler;
-- Si está OFF, activar vía gcloud:
--   gcloud sql instances patch INSTANCE_NAME --database-flags event_scheduler=on

-- Verificar eventos creados:
--   SELECT * FROM information_schema.events ORDER BY event_name;

-- Verificar última ejecución:
--   SELECT event_name, last_executed, status
--   FROM information_schema.events
--   WHERE event_schema = DATABASE();

-- ============================================================================
-- Rollback completo (desactivar todos los eventos)
-- ============================================================================
-- DROP EVENT IF EXISTS ev_purge_audit_logs;
-- DROP EVENT IF EXISTS ev_snapshot_daily_kpis;
-- DROP EVENT IF EXISTS ev_rotate_snapshots;
-- En local, si aplica:
--   SET GLOBAL event_scheduler = OFF;
