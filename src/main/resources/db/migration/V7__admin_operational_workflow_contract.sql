CREATE TABLE IF NOT EXISTS hospital_operational_contacts (
    id                  VARCHAR(36)  NOT NULL PRIMARY KEY,
    hospital_id         VARCHAR(36)  NOT NULL,
    user_id             VARCHAR(36),
    display_name        VARCHAR(255) NOT NULL,
    role_label          VARCHAR(128) NOT NULL,
    department_code     VARCHAR(32),
    contact_channel     VARCHAR(32),
    contact_value       VARCHAR(255),
    availability_status VARCHAR(32),
    is_assignable       BOOLEAN      NOT NULL DEFAULT FALSE,
    is_notifiable       BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at          TIMESTAMP    NOT NULL,
    CONSTRAINT fk_hoc_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    CONSTRAINT fk_hoc_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS hospital_operational_groups (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    hospital_id     VARCHAR(36)  NOT NULL,
    group_code      VARCHAR(32)  NOT NULL,
    group_name      VARCHAR(128) NOT NULL,
    group_type      VARCHAR(32)  NOT NULL,
    department_code VARCHAR(32),
    is_assignable   BOOLEAN      NOT NULL DEFAULT FALSE,
    is_notifiable   BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at      TIMESTAMP    NOT NULL,
    CONSTRAINT fk_hog_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
);

CREATE TABLE IF NOT EXISTS hospital_operational_group_members (
    id          VARCHAR(36) NOT NULL PRIMARY KEY,
    group_id    VARCHAR(36) NOT NULL,
    contact_id  VARCHAR(36) NOT NULL,
    created_at  TIMESTAMP   NOT NULL,
    CONSTRAINT fk_hogm_group FOREIGN KEY (group_id) REFERENCES hospital_operational_groups(id),
    CONSTRAINT fk_hogm_contact FOREIGN KEY (contact_id) REFERENCES hospital_operational_contacts(id)
);

CREATE TABLE IF NOT EXISTS hospital_inventory_movements (
    id                        VARCHAR(36) NOT NULL PRIMARY KEY,
    hospital_id               VARCHAR(36) NOT NULL,
    inventory_item_id         VARCHAR(36) NOT NULL,
    movement_type             VARCHAR(32) NOT NULL,
    quantity_delta            INT         NOT NULL DEFAULT 0,
    unit                      VARCHAR(32),
    notes                     TEXT,
    related_supply_request_id VARCHAR(36),
    created_at                TIMESTAMP   NOT NULL,
    CONSTRAINT fk_him_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    CONSTRAINT fk_him_inventory FOREIGN KEY (inventory_item_id) REFERENCES hospital_inventory_items(id),
    CONSTRAINT fk_him_supply_request FOREIGN KEY (related_supply_request_id) REFERENCES supply_requests(id)
);

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'primary_department_resource_id') = 0, 'ALTER TABLE operational_recommendations ADD COLUMN primary_department_resource_id VARCHAR(36)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'primary_staffing_profile_id') = 0, 'ALTER TABLE operational_recommendations ADD COLUMN primary_staffing_profile_id VARCHAR(36)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'primary_inventory_item_id') = 0, 'ALTER TABLE operational_recommendations ADD COLUMN primary_inventory_item_id VARCHAR(36)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'presentation_variant') = 0, 'ALTER TABLE operational_recommendations ADD COLUMN presentation_variant VARCHAR(32)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'primary_action_code') = 0, 'ALTER TABLE operational_recommendations ADD COLUMN primary_action_code VARCHAR(32)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'available_actions_json') = 0, 'ALTER TABLE operational_recommendations ADD COLUMN available_actions_json TEXT', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'allowed_status_transitions_json') = 0, 'ALTER TABLE operational_recommendations ADD COLUMN allowed_status_transitions_json TEXT', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'display_category_label') = 0, 'ALTER TABLE operational_recommendations ADD COLUMN display_category_label VARCHAR(64)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'display_severity_label') = 0, 'ALTER TABLE operational_recommendations ADD COLUMN display_severity_label VARCHAR(32)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'display_status_label') = 0, 'ALTER TABLE operational_recommendations ADD COLUMN display_status_label VARCHAR(32)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'expires_at') = 0, 'ALTER TABLE operational_recommendations ADD COLUMN expires_at TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'assigned_owner_user_id') = 0, 'ALTER TABLE operational_recommendations ADD COLUMN assigned_owner_user_id VARCHAR(36)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_tasks' AND column_name = 'owner_contact_id') = 0, 'ALTER TABLE operational_tasks ADD COLUMN owner_contact_id VARCHAR(36)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_tasks' AND column_name = 'owner_group_id') = 0, 'ALTER TABLE operational_tasks ADD COLUMN owner_group_id VARCHAR(36)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_tasks' AND column_name = 'source_action_code') = 0, 'ALTER TABLE operational_tasks ADD COLUMN source_action_code VARCHAR(32)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_tasks' AND column_name = 'recommended_by_recommendation_id') = 0, 'ALTER TABLE operational_tasks ADD COLUMN recommended_by_recommendation_id VARCHAR(36)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_notifications' AND column_name = 'audience_group_id') = 0, 'ALTER TABLE operational_notifications ADD COLUMN audience_group_id VARCHAR(36)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_notifications' AND column_name = 'audience_contact_id') = 0, 'ALTER TABLE operational_notifications ADD COLUMN audience_contact_id VARCHAR(36)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_notifications' AND column_name = 'delivery_channel') = 0, 'ALTER TABLE operational_notifications ADD COLUMN delivery_channel VARCHAR(32)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_notifications' AND column_name = 'delivery_status_detail') = 0, 'ALTER TABLE operational_notifications ADD COLUMN delivery_status_detail VARCHAR(255)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'operational_notifications' AND column_name = 'source_action_code') = 0, 'ALTER TABLE operational_notifications ADD COLUMN source_action_code VARCHAR(32)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'supply_requests' AND column_name = 'source_action_code') = 0, 'ALTER TABLE supply_requests ADD COLUMN source_action_code VARCHAR(32)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'supply_requests' AND column_name = 'priority') = 0, 'ALTER TABLE supply_requests ADD COLUMN priority VARCHAR(16)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'supply_requests' AND column_name = 'requested_needed_by') = 0, 'ALTER TABLE supply_requests ADD COLUMN requested_needed_by TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'supply_requests' AND column_name = 'linked_recommendation_inventory_item_id') = 0, 'ALTER TABLE supply_requests ADD COLUMN linked_recommendation_inventory_item_id VARCHAR(36)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.key_column_usage WHERE constraint_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'primary_department_resource_id' AND referenced_table_name = 'hospital_department_resources') = 0, 'ALTER TABLE operational_recommendations ADD CONSTRAINT fk_or_department_resource FOREIGN KEY (primary_department_resource_id) REFERENCES hospital_department_resources(id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.key_column_usage WHERE constraint_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'primary_staffing_profile_id' AND referenced_table_name = 'hospital_staffing_profiles') = 0, 'ALTER TABLE operational_recommendations ADD CONSTRAINT fk_or_staffing_profile FOREIGN KEY (primary_staffing_profile_id) REFERENCES hospital_staffing_profiles(id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.key_column_usage WHERE constraint_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'primary_inventory_item_id' AND referenced_table_name = 'hospital_inventory_items') = 0, 'ALTER TABLE operational_recommendations ADD CONSTRAINT fk_or_inventory_item FOREIGN KEY (primary_inventory_item_id) REFERENCES hospital_inventory_items(id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.key_column_usage WHERE constraint_schema = DATABASE() AND table_name = 'operational_recommendations' AND column_name = 'assigned_owner_user_id' AND referenced_table_name = 'users') = 0, 'ALTER TABLE operational_recommendations ADD CONSTRAINT fk_or_assigned_owner FOREIGN KEY (assigned_owner_user_id) REFERENCES users(id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.key_column_usage WHERE constraint_schema = DATABASE() AND table_name = 'operational_tasks' AND column_name = 'owner_contact_id' AND referenced_table_name = 'hospital_operational_contacts') = 0, 'ALTER TABLE operational_tasks ADD CONSTRAINT fk_ot_owner_contact FOREIGN KEY (owner_contact_id) REFERENCES hospital_operational_contacts(id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.key_column_usage WHERE constraint_schema = DATABASE() AND table_name = 'operational_tasks' AND column_name = 'owner_group_id' AND referenced_table_name = 'hospital_operational_groups') = 0, 'ALTER TABLE operational_tasks ADD CONSTRAINT fk_ot_owner_group FOREIGN KEY (owner_group_id) REFERENCES hospital_operational_groups(id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.key_column_usage WHERE constraint_schema = DATABASE() AND table_name = 'operational_tasks' AND column_name = 'recommended_by_recommendation_id' AND referenced_table_name = 'operational_recommendations') = 0, 'ALTER TABLE operational_tasks ADD CONSTRAINT fk_ot_recommended_by FOREIGN KEY (recommended_by_recommendation_id) REFERENCES operational_recommendations(id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.key_column_usage WHERE constraint_schema = DATABASE() AND table_name = 'operational_notifications' AND column_name = 'audience_group_id' AND referenced_table_name = 'hospital_operational_groups') = 0, 'ALTER TABLE operational_notifications ADD CONSTRAINT fk_on_audience_group FOREIGN KEY (audience_group_id) REFERENCES hospital_operational_groups(id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.key_column_usage WHERE constraint_schema = DATABASE() AND table_name = 'operational_notifications' AND column_name = 'audience_contact_id' AND referenced_table_name = 'hospital_operational_contacts') = 0, 'ALTER TABLE operational_notifications ADD CONSTRAINT fk_on_audience_contact FOREIGN KEY (audience_contact_id) REFERENCES hospital_operational_contacts(id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.key_column_usage WHERE constraint_schema = DATABASE() AND table_name = 'supply_requests' AND column_name = 'linked_recommendation_inventory_item_id' AND referenced_table_name = 'hospital_inventory_items') = 0, 'ALTER TABLE supply_requests ADD CONSTRAINT fk_sr_linked_recommendation_inventory_item FOREIGN KEY (linked_recommendation_inventory_item_id) REFERENCES hospital_inventory_items(id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE operational_recommendations
SET primary_department_resource_id = '21000000-0000-0000-0000-000000000003',
    primary_staffing_profile_id = '22000000-0000-0000-0000-000000000002',
    primary_inventory_item_id = '23000000-0000-0000-0000-000000000002',
    presentation_variant = 'alert',
    primary_action_code = 'ASSIGN_TASK',
    available_actions_json = '[{"code":"ASSIGN_TASK","label":"Assign task","style":"primary","enabled":true},{"code":"NOTIFY_STAFF","label":"Notify staff","style":"secondary","enabled":true},{"code":"ORDER_SUPPLIES","label":"Order supplies","style":"secondary","enabled":true}]',
    allowed_status_transitions_json = '["ACCEPTED","ASSIGNED","REJECTED"]',
    display_category_label = 'Bed Capacity',
    display_severity_label = 'Critical',
    display_status_label = 'New',
    expires_at = CURRENT_TIMESTAMP
WHERE id = '24000000-0000-0000-0000-000000000001';

UPDATE operational_recommendations
SET primary_department_resource_id = '21000000-0000-0000-0000-000000000001',
    primary_staffing_profile_id = '22000000-0000-0000-0000-000000000001',
    presentation_variant = 'urgent',
    primary_action_code = 'ASSIGN_TASK',
    available_actions_json = '[{"code":"ASSIGN_TASK","label":"Assign task","style":"primary","enabled":true},{"code":"NOTIFY_STAFF","label":"Notify staff","style":"secondary","enabled":true},{"code":"ORDER_SUPPLIES","label":"Order supplies","style":"secondary","enabled":false,"disabledReason":"No inventory item linked"}]',
    allowed_status_transitions_json = '["ASSIGNED","COMPLETED","REJECTED"]',
    display_category_label = 'Staffing',
    display_severity_label = 'High',
    display_status_label = 'Accepted',
    expires_at = CURRENT_TIMESTAMP
WHERE id = '24000000-0000-0000-0000-000000000002';

UPDATE operational_recommendations
SET primary_department_resource_id = '21000000-0000-0000-0000-000000000004',
    primary_staffing_profile_id = '22000000-0000-0000-0000-000000000004',
    primary_inventory_item_id = '23000000-0000-0000-0000-000000000005',
    presentation_variant = 'standard',
    primary_action_code = 'ORDER_SUPPLIES',
    available_actions_json = '[{"code":"ASSIGN_TASK","label":"Assign task","style":"secondary","enabled":true},{"code":"NOTIFY_STAFF","label":"Notify staff","style":"secondary","enabled":true},{"code":"ORDER_SUPPLIES","label":"Order supplies","style":"primary","enabled":true}]',
    allowed_status_transitions_json = '["ACCEPTED","ASSIGNED","REJECTED"]',
    display_category_label = 'Isolation',
    display_severity_label = 'Medium',
    display_status_label = 'New',
    expires_at = CURRENT_TIMESTAMP
WHERE id = '24000000-0000-0000-0000-000000000003';
