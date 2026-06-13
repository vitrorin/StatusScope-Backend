-- ============================================================================
-- Trigger: trg_validate_inventory_before_update
-- ============================================================================
-- Tipo:              BEFORE UPDATE
-- Tabla afectada:    hospital_inventory_items
-- Proposito:         Recalcular automaticamente el campo status de un item de
--                    inventario cuando se actualiza current_quantity o
--                    critical_threshold, garantizando consistencia entre las
--                    cantidades y el estado reportado sin depender de la app.
-- Logica de negocio:
--   Si current_quantity <= critical_threshold -> status = 'CRITICAL'
--   Si current_quantity <= critical_threshold * 2 -> status = 'LOW'
--   En otro caso -> status = 'ADEQUATE'
-- Casos de prueba:
--   1. UPDATE current_quantity=50 WHERE threshold=200 -> status cambia a CRITICAL
--   2. UPDATE current_quantity=300 WHERE threshold=200 -> status cambia a LOW
--   3. UPDATE current_quantity=1500 WHERE threshold=200 -> status cambia a ADEQUATE
--   4. UPDATE current_quantity sin cambiar threshold -> status se recalcula igual
-- ============================================================================

DROP TRIGGER IF EXISTS trg_validate_inventory_before_update;

DELIMITER //

CREATE TRIGGER trg_validate_inventory_before_update
BEFORE UPDATE ON hospital_inventory_items
FOR EACH ROW
BEGIN
    IF NEW.current_quantity <= NEW.critical_threshold THEN
        SET NEW.status = 'CRITICAL';
    ELSEIF NEW.current_quantity <= NEW.critical_threshold * 2 THEN
        SET NEW.status = 'LOW';
    ELSE
        SET NEW.status = 'ADEQUATE';
    END IF;
END //

DELIMITER ;