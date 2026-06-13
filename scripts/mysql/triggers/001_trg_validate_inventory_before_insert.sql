-- ============================================================================
-- Trigger: trg_validate_inventory_before_insert
-- ============================================================================
-- Tipo:              BEFORE INSERT
-- Tabla afectada:    hospital_inventory_movements
-- Proposito:         Validar que un movimiento de inventario no resulte en
--                    cantidad negativa para el item. Si el movimiento dejaria
--                    current_quantity < 0, se lanza un error y se rechaza la
--                    insercion, protegiendo la integridad de los datos.
-- Logica de negocio: Los movimientos de tipo CONSUMPTION o MANUAL_ADJUSTMENT
--                    con quantity_delta negativo representan salidas de inventario.
--                    El trigger verifica que el item tenga suficiente cantidad.
-- Casos de prueba:
--   1. Insertar movimiento CONSUMPTION con delta=-50 e item con current=100 -> OK
--   2. Insertar movimiento CONSUMPTION con delta=-200 e item con current=100 -> ERROR
--   3. Insertar movimiento REPLENISHMENT con delta=+500 -> siempre OK
-- ============================================================================

DROP TRIGGER IF EXISTS trg_validate_inventory_before_insert;

DELIMITER //

CREATE TRIGGER trg_validate_inventory_before_insert
BEFORE INSERT ON hospital_inventory_movements
FOR EACH ROW
BEGIN
    DECLARE v_current_quantity INT;

    IF NEW.quantity_delta < 0 THEN
        SELECT current_quantity INTO v_current_quantity
        FROM hospital_inventory_items
        WHERE id = NEW.inventory_item_id;

        IF v_current_quantity + NEW.quantity_delta < 0 THEN
            SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Inventario insuficiente: el movimiento dejaria cantidad negativa',
                MYSQL_ERRNO = 1644;
        END IF;
    END IF;
END //

DELIMITER ;