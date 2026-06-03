-- ============================================================================
-- V8: Advanced Database Module — Stored Procedures, Functions, Triggers
-- ============================================================================
-- Requisitos del Modulo 4:
--   2 Stored Procedures: sp_create_supply_request_with_movement,
--                        sp_generate_hospital_operational_summary
--   2 Funciones almacenadas: fn_bed_occupancy_pct, fn_inventory_status
--   3 Triggers: trg_validate_inventory_before_insert,
--               trg_validate_inventory_before_update,
--               trg_audit_recommendation_change
-- ============================================================================
-- Nota: Este archivo se ejecuta como migracion Flyway. Los scripts SQL
-- individuales con documentacion completa estan en:
--   scripts/mysql/stored_procedures/
--   scripts/mysql/functions/
--   scripts/mysql/triggers/
-- ============================================================================

-- ============================================================================
-- 1. Stored Procedure: sp_create_supply_request_with_movement
-- ============================================================================
DELIMITER //
DROP PROCEDURE IF EXISTS sp_create_supply_request_with_movement //
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

-- ============================================================================
-- 2. Stored Procedure: sp_generate_hospital_operational_summary
-- ============================================================================
DELIMITER //
DROP PROCEDURE IF EXISTS sp_generate_hospital_operational_summary //
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

-- ============================================================================
-- 3. Funcion: fn_bed_occupancy_pct
-- ============================================================================
DELIMITER //
DROP FUNCTION IF EXISTS fn_bed_occupancy_pct //
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

-- ============================================================================
-- 4. Funcion: fn_inventory_status
-- ============================================================================
DELIMITER //
DROP FUNCTION IF EXISTS fn_inventory_status //
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

-- ============================================================================
-- 5. Trigger: trg_validate_inventory_before_insert
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

-- ============================================================================
-- 6. Trigger: trg_validate_inventory_before_update
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

-- ============================================================================
-- 7. Trigger: trg_audit_recommendation_change
-- ============================================================================
DROP TRIGGER IF EXISTS trg_audit_recommendation_change;
DELIMITER //
CREATE TRIGGER trg_audit_recommendation_change
AFTER UPDATE ON operational_recommendations
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO operational_recommendation_audit (
            id, recommendation_id, actor_user_id, event_type, event_label,
            event_payload_json, created_at
        ) VALUES (
            UUID(),
            NEW.id,
            NEW.assigned_owner_user_id,
            'STATUS_CHANGE',
            NEW.status,
            JSON_OBJECT(
                'from_status', OLD.status,
                'to_status', NEW.status,
                'severity', NEW.severity,
                'changed_at', NOW()
            ),
            NOW()
        );
    END IF;
END //
DELIMITER ;
