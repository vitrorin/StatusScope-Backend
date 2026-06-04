-- ============================================================================
-- Stored Procedure: sp_generate_hospital_operational_summary
-- ============================================================================
-- Objetivo:         Generar un resumen operativo consolidado por hospital,
--                   calculando ocupacion de camas, estado de inventario critico
--                   y cantidad de recomendaciones activas en una sola llamada.
-- Problema que resuelve: El dashboard admin necesita datos de multiples tablas
--                   (snapshots, inventario, recomendaciones) para cada hospital.
--                   Sin este SP, se emiten 3-4 queries separadas, causando
--                   latencia y posible inconsistencia por lecturas no atomicas.
-- Logica:
--   1. Consultar el ultimo snapshot de recursos del hospital.
--   2. Contar items de inventario en estado CRITICAL o LOW.
--   3. Contar recomendaciones activas (status NOT IN COMPLETED, REJECTED).
--   4. Insertar/actualizar resultado en tabla temporal de sesion.
-- Ejemplo de ejecucion:
--   CALL sp_generate_hospital_operational_summary('30000000-0000-0000-0000-000000000001');
--   SELECT * FROM hospital_operational_summary_result;
-- Pruebas:
--   1. Ejecutar con hospital_id valido -> devuelve resumen con datos.
--   2. Ejecutar con hospital_id sin snapshots -> campos de snapshot en NULL.
--   3. Ejecutar con hospital_id inexistente -> resultado vacio.
-- ============================================================================

DROP PROCEDURE IF EXISTS sp_generate_hospital_operational_summary;

DELIMITER //

CREATE PROCEDURE sp_generate_hospital_operational_summary(
    IN p_hospital_id VARCHAR(36)
)
BEGIN
    DROP TEMPORARY TABLE IF EXISTS hospital_operational_summary_result;
    CREATE TEMPORARY TABLE hospital_operational_summary_result (
        hospital_id                       VARCHAR(36),
        hospital_name                     VARCHAR(255),
        total_beds                        INT,
        available_beds                    INT,
        bed_occupancy_pct                DECIMAL(5,2),
        icu_total_beds                   INT,
        icu_available_beds               INT,
        icu_occupancy_pct                DECIMAL(5,2),
        critical_inventory_count         INT,
        low_inventory_count              INT,
        active_recommendations_count      INT,
        last_snapshot_captured_at         TIMESTAMP
    );

    INSERT INTO hospital_operational_summary_result (
        hospital_id, hospital_name,
        total_beds, available_beds, bed_occupancy_pct,
        icu_total_beds, icu_available_beds, icu_occupancy_pct,
        critical_inventory_count, low_inventory_count,
        active_recommendations_count, last_snapshot_captured_at
    )
    SELECT
        h.id,
        h.name,
        s.total_beds,
        s.available_beds,
        fn_bed_occupancy_pct(p_hospital_id),
        s.icu_total_beds,
        s.icu_available_beds,
        CASE
            WHEN s.icu_total_beds > 0
            THEN ROUND((s.icu_total_beds - s.icu_available_beds) * 100.0 / s.icu_total_beds, 2)
            ELSE NULL
        END,
        (SELECT COUNT(*) FROM hospital_inventory_items ii
         WHERE ii.hospital_id = p_hospital_id AND ii.status = 'CRITICAL'),
        (SELECT COUNT(*) FROM hospital_inventory_items ii
         WHERE ii.hospital_id = p_hospital_id AND ii.status = 'LOW'),
        (SELECT COUNT(*) FROM operational_recommendations or2
         WHERE or2.hospital_id = p_hospital_id
           AND or2.status NOT IN ('COMPLETED', 'REJECTED')),
        s.captured_at
    FROM hospitals h
    LEFT JOIN (
        SELECT *
        FROM hospital_resource_snapshots hrs
        WHERE hrs.hospital_id = p_hospital_id
        ORDER BY hrs.captured_at DESC
        LIMIT 1
    ) s ON 1=1
    WHERE h.id = p_hospital_id;

    SELECT * FROM hospital_operational_summary_result;

    DROP TEMPORARY TABLE IF EXISTS hospital_operational_summary_result;
END //

DELIMITER ;