-- ============================================================================
-- Funcion almacenada: fn_bed_occupancy_pct
-- ============================================================================
-- Proposito:        Calcular el porcentaje de ocupacion de camas de un hospital
--                   basandose en el ultimo snapshot de recursos disponible.
-- Valor retornado:  DECIMAL(5,2) — porcentaje de ocupacion (0-100).
--                   Retorna NULL si no hay snapshots para el hospital.
-- Logica:
--   1. Buscar el ultimo snapshot del hospital (ORDER BY captured_at DESC LIMIT 1).
--   2. Calcular (total_beds - available_beds) * 100.0 / total_beds.
--   3. Si total_beds es 0 o no existe snapshot, retornar NULL.
-- Ejemplo de uso:
--   SELECT fn_bed_occupancy_pct('30000000-0000-0000-0000-000000000001');
--   -- Resultado: 75.00
-- Pruebas:
--   1. Hospital con snapshot (total=240, available=60) -> 75.00
--   2. Hospital sin snapshot -> NULL
--   3. Hospital con total_beds=0 -> NULL
-- ============================================================================

DROP FUNCTION IF EXISTS fn_bed_occupancy_pct;

DELIMITER //

CREATE FUNCTION fn_bed_occupancy_pct(p_hospital_id VARCHAR(36))
RETURNS DECIMAL(5,2)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_total_beds INT;
    DECLARE v_available_beds INT;

    SELECT total_beds, available_beds
    INTO v_total_beds, v_available_beds
    FROM hospital_resource_snapshots
    WHERE hospital_id = p_hospital_id
    ORDER BY captured_at DESC
    LIMIT 1;

    IF v_total_beds IS NULL OR v_total_beds = 0 THEN
        RETURN NULL;
    END IF;

    RETURN ROUND((v_total_beds - v_available_beds) * 100.0 / v_total_beds, 2);
END //

DELIMITER ;