-- ============================================================================
-- Stored Procedure: sp_create_supply_request_with_movement
-- ============================================================================
-- Objetivo:         Crear una solicitud de suministro y el movimiento de
--                   inventario correspondiente en una sola transaccion atomica.
-- Fuente de verdad: Replica la firma y comportamiento de
--                   V12__advanced_database_module.sql.
-- Logica:
--   1. Insertar supply_requests con status REQUESTED.
--   2. Insertar hospital_inventory_movements con quantity_delta positivo.
--   3. Si alguna operacion falla, rollback automatico por estar dentro del SP.
-- Pruebas:
--   1. Ejecutar con item_id valido -> crea solicitud + movimiento.
--   2. Ejecutar con item_id inexistente -> falla por FK.
--   3. Verificar que no hay solicitud sin movimiento ni viceversa.
-- ============================================================================

DROP PROCEDURE IF EXISTS sp_create_supply_request_with_movement;

DELIMITER //

CREATE PROCEDURE sp_create_supply_request_with_movement(
    IN p_request_id VARCHAR(36),
    IN p_movement_id VARCHAR(36),
    IN p_recommendation_id VARCHAR(36),
    IN p_hospital_id VARCHAR(36),
    IN p_inventory_item_id VARCHAR(36),
    IN p_supply_type_label VARCHAR(255),
    IN p_quantity INT,
    IN p_unit VARCHAR(32),
    IN p_destination VARCHAR(255),
    IN p_suggested_supplier VARCHAR(255),
    IN p_source_action_code VARCHAR(32),
    IN p_priority VARCHAR(16),
    IN p_requested_needed_by TIMESTAMP,
    IN p_linked_recommendation_inventory_item_id VARCHAR(36),
    IN p_requested_by_user_id VARCHAR(36),
    IN p_notes TEXT
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    INSERT INTO supply_requests (
        id, recommendation_id, hospital_id, inventory_item_id,
        supply_type_label, quantity, unit, destination, suggested_supplier,
        status, source_action_code, priority, requested_needed_by,
        linked_recommendation_inventory_item_id, requested_by_user_id,
        created_at, updated_at
    ) VALUES (
        p_request_id, p_recommendation_id, p_hospital_id, p_inventory_item_id,
        p_supply_type_label, p_quantity, p_unit, p_destination, p_suggested_supplier,
        'REQUESTED', p_source_action_code, p_priority, p_requested_needed_by,
        p_linked_recommendation_inventory_item_id, p_requested_by_user_id,
        NOW(), NOW()
    );

    INSERT INTO hospital_inventory_movements (
        id, hospital_id, inventory_item_id, movement_type,
        quantity_delta, unit, notes, related_supply_request_id, created_at
    ) VALUES (
        p_movement_id, p_hospital_id, p_inventory_item_id, 'REPLENISHMENT',
        p_quantity, p_unit, p_notes, p_request_id, NOW()
    );

    COMMIT;
END //

DELIMITER ;
