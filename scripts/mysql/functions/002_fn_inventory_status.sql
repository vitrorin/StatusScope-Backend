-- ============================================================================
-- Funcion almacenada: fn_inventory_status
-- ============================================================================
-- Proposito:        Determinar el estado de un item de inventario basandose en
--                   su cantidad actual y umbral critico. Replica la logica de
--                   la aplicacion para garantizar consistencia en la base de datos.
-- Valor retornado:  VARCHAR(32) — 'CRITICAL', 'LOW' o 'ADEQUATE'.
--                   Retorna NULL si el item no existe.
-- Logica:
--   1. Si current_quantity <= critical_threshold -> CRITICAL
--   2. Si current_quantity <= critical_threshold * 2 -> LOW
--   3. En otro caso -> ADEQUATE
-- Ejemplo de uso:
--   SELECT fn_inventory_status('23000000-0000-0000-0000-000000000005');
--   -- Si current_quantity=150 y threshold=200 -> CRITICAL
-- Pruebas:
--   1. Item con current=50, threshold=200 -> CRITICAL
--   2. Item con current=300, threshold=200 -> LOW
--   3. Item con current=1000, threshold=200 -> ADEQUATE
--   4. Item inexistente -> NULL
-- ============================================================================

DROP FUNCTION IF EXISTS fn_inventory_status;

DELIMITER //

CREATE FUNCTION fn_inventory_status(p_inventory_item_id VARCHAR(36))
RETURNS VARCHAR(32)
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_current INT;
    DECLARE v_threshold INT;

    SELECT current_quantity, critical_threshold
    INTO v_current, v_threshold
    FROM hospital_inventory_items
    WHERE id = p_inventory_item_id;

    IF v_current IS NULL THEN
        RETURN NULL;
    END IF;

    IF v_current <= v_threshold THEN
        RETURN 'CRITICAL';
    ELSEIF v_current <= v_threshold * 2 THEN
        RETURN 'LOW';
    ELSE
        RETURN 'ADEQUATE';
    END IF;
END //

DELIMITER ;