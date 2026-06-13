-- ============================================================================
-- Event Scheduler: ev_purge_audit_logs
-- ============================================================================
-- Objetivo:         Depurar registros de auditoría operativa, tareas completadas,
--                   notificaciones enviadas y solicitudes de suministro con más
--                   de 90 días de antigüedad, manteniendo la tabla principal de
--                   recomendaciones intacta.
-- Proceso:          DELETE acotado (1000 filas por ejecución) para minimizar
--                   lock contention y presión sobre el binlog.
-- Frecuencia:       Diario a las 03:00 (America/Mexico_City)
-- Tablas afectadas: operational_recommendation_audit
--                   operational_tasks
--                   operational_notifications
--                   supply_requests sin movimientos de inventario relacionados
-- Dependencias:     Ninguna; las FK apuntan a operational_recommendations que
--                   NO se modifica.
-- Impacto esperado: Reduce el tamaño de tablas transaccionales, acelera
--                   backups y consultas de auditoría.
-- Rollback:         Los datos eliminados son prescindibles. No hay rollback.
-- ============================================================================

DROP EVENT IF EXISTS ev_purge_audit_logs;

DELIMITER //

CREATE EVENT ev_purge_audit_logs
    ON SCHEDULE
        EVERY 1 DAY
        STARTS TIMESTAMP(CURRENT_DATE, '03:00:00')
    COMMENT 'Purga registros operativos mayores a 90 días'
    DO
BEGIN
    DECLARE cutoff DATETIME;
    SET cutoff = NOW() - INTERVAL 90 DAY;

    -- Tabla 1: auditoría de recomendaciones
    DELETE FROM operational_recommendation_audit
    WHERE created_at < cutoff
    LIMIT 1000;

    -- Tabla 2: tareas completadas/rechazadas antiguas
    DELETE FROM operational_tasks
    WHERE status IN ('COMPLETED', 'REJECTED')
      AND updated_at < cutoff
    LIMIT 1000;

    -- Tabla 3: notificaciones enviadas antiguas
    DELETE FROM operational_notifications
    WHERE status = 'SENT'
      AND sent_at < cutoff
    LIMIT 1000;

    -- Tabla 4: solicitudes de suministro completadas/rechazadas antiguas.
    -- Se conservan las que tienen movimientos relacionados para respetar FK.
    DELETE FROM supply_requests
    WHERE status IN ('FULFILLED', 'CANCELLED')
      AND updated_at < cutoff
      AND NOT EXISTS (
          SELECT 1
          FROM hospital_inventory_movements him
          WHERE him.related_supply_request_id = supply_requests.id
      )
    LIMIT 1000;
END //

DELIMITER ;
