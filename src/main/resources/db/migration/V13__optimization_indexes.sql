-- ==========================================================================
-- V9: MySQL optimization indexes used by backend endpoints.
-- ==========================================================================
-- These indexes match the filters/orderings used by dashboard, resources,
-- recommendations, diagnosis context and movement-history endpoints.

DELIMITER //

DROP PROCEDURE IF EXISTS add_index_if_missing //

CREATE PROCEDURE add_index_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
    IN p_create_sql TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table_name
          AND index_name = p_index_name
    ) THEN
        SET @ddl = p_create_sql;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //

DELIMITER ;

CALL add_index_if_missing(
    'outbreaks',
    'idx_outbreaks_status_scope_municipality',
    'CREATE INDEX idx_outbreaks_status_scope_municipality ON outbreaks (status, scope, municipality_id)'
);

CALL add_index_if_missing(
    'outbreaks',
    'idx_outbreaks_status_scope_state',
    'CREATE INDEX idx_outbreaks_status_scope_state ON outbreaks (status, scope, state_id)'
);

CALL add_index_if_missing(
    'operational_recommendations',
    'idx_recs_hospital_created',
    'CREATE INDEX idx_recs_hospital_created ON operational_recommendations (hospital_id, created_at DESC)'
);

CALL add_index_if_missing(
    'operational_recommendations',
    'idx_recs_hospital_status_created',
    'CREATE INDEX idx_recs_hospital_status_created ON operational_recommendations (hospital_id, status, created_at DESC)'
);

CALL add_index_if_missing(
    'operational_recommendations',
    'idx_recs_hospital_severity_created',
    'CREATE INDEX idx_recs_hospital_severity_created ON operational_recommendations (hospital_id, severity, created_at DESC)'
);

CALL add_index_if_missing(
    'hospital_resource_snapshots',
    'idx_snapshots_hospital_captured',
    'CREATE INDEX idx_snapshots_hospital_captured ON hospital_resource_snapshots (hospital_id, captured_at DESC)'
);

CALL add_index_if_missing(
    'hospital_department_resources',
    'idx_dept_hospital_status',
    'CREATE INDEX idx_dept_hospital_status ON hospital_department_resources (hospital_id, status)'
);

CALL add_index_if_missing(
    'hospital_inventory_items',
    'idx_inventory_hospital_category',
    'CREATE INDEX idx_inventory_hospital_category ON hospital_inventory_items (hospital_id, category, item_name)'
);

CALL add_index_if_missing(
    'hospital_inventory_items',
    'idx_inventory_hospital_status',
    'CREATE INDEX idx_inventory_hospital_status ON hospital_inventory_items (hospital_id, status)'
);

CALL add_index_if_missing(
    'hospital_staffing_profiles',
    'idx_staff_hospital_role',
    'CREATE INDEX idx_staff_hospital_role ON hospital_staffing_profiles (hospital_id, role_name)'
);

CALL add_index_if_missing(
    'patients',
    'idx_patients_hospital_name',
    'CREATE INDEX idx_patients_hospital_name ON patients (hospital_id, full_name)'
);

CALL add_index_if_missing(
    'operational_recommendation_audit',
    'idx_audit_recommendation_created',
    'CREATE INDEX idx_audit_recommendation_created ON operational_recommendation_audit (recommendation_id, created_at ASC)'
);

CALL add_index_if_missing(
    'hospital_inventory_movements',
    'idx_movements_item_created',
    'CREATE INDEX idx_movements_item_created ON hospital_inventory_movements (inventory_item_id, created_at DESC)'
);

DROP PROCEDURE IF EXISTS add_index_if_missing;
