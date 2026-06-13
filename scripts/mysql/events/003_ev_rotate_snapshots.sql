-- ============================================================================
-- Event Scheduler: ev_rotate_snapshots
-- ============================================================================
-- Objetivo:         Rotar snapshots de recursos hospitalarios antiguos para
--                   evitar que la tabla hospital_resource_snapshots crezca sin
--                   control y degrade las consultas de "último snapshot".
-- Proceso:          Elimina snapshots con captured_at anterior a N días.
--                   Los N días se definen como variable para facilitar ajustes.
-- Frecuencia:       Semanal, domingo 04:00 (America/Mexico_City)
-- Tablas afectadas: hospital_resource_snapshots
-- Dependencias:     Ninguna. Los tasks, notifications y supply_requests que
--                   referencien recomendaciones no se ven afectados.
-- Impacto esperado: Mantiene un tamaño acotado de la tabla de snapshots,
--                   acelerando la consulta de último snapshot por hospital.
-- Rollback:         No aplica; los datos son temporales por naturaleza.
-- ============================================================================

DROP EVENT IF EXISTS ev_rotate_snapshots;

DELIMITER //

CREATE EVENT ev_rotate_snapshots
    ON SCHEDULE
        EVERY 1 WEEK
        STARTS TIMESTAMP(CURRENT_DATE + INTERVAL MOD(6 - WEEKDAY(CURRENT_DATE) + 7, 7) DAY, '04:00:00')
    COMMENT 'Elimina snapshots de recursos con más de 30 días'
    DO
BEGIN
    DECLARE retention_days INT DEFAULT 30;

    DELETE FROM hospital_resource_snapshots
    WHERE captured_at < NOW() - INTERVAL retention_days DAY
    LIMIT 500;
END //

DELIMITER ;
