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

ALTER TABLE operational_recommendations ADD COLUMN IF NOT EXISTS primary_department_resource_id VARCHAR(36);
ALTER TABLE operational_recommendations ADD COLUMN IF NOT EXISTS primary_staffing_profile_id VARCHAR(36);
ALTER TABLE operational_recommendations ADD COLUMN IF NOT EXISTS primary_inventory_item_id VARCHAR(36);
ALTER TABLE operational_recommendations ADD COLUMN IF NOT EXISTS presentation_variant VARCHAR(32);
ALTER TABLE operational_recommendations ADD COLUMN IF NOT EXISTS primary_action_code VARCHAR(32);
ALTER TABLE operational_recommendations ADD COLUMN IF NOT EXISTS available_actions_json TEXT;
ALTER TABLE operational_recommendations ADD COLUMN IF NOT EXISTS allowed_status_transitions_json TEXT;
ALTER TABLE operational_recommendations ADD COLUMN IF NOT EXISTS display_category_label VARCHAR(64);
ALTER TABLE operational_recommendations ADD COLUMN IF NOT EXISTS display_severity_label VARCHAR(32);
ALTER TABLE operational_recommendations ADD COLUMN IF NOT EXISTS display_status_label VARCHAR(32);
ALTER TABLE operational_recommendations ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;
ALTER TABLE operational_recommendations ADD COLUMN IF NOT EXISTS assigned_owner_user_id VARCHAR(36);

ALTER TABLE operational_tasks ADD COLUMN IF NOT EXISTS owner_contact_id VARCHAR(36);
ALTER TABLE operational_tasks ADD COLUMN IF NOT EXISTS owner_group_id VARCHAR(36);
ALTER TABLE operational_tasks ADD COLUMN IF NOT EXISTS source_action_code VARCHAR(32);
ALTER TABLE operational_tasks ADD COLUMN IF NOT EXISTS recommended_by_recommendation_id VARCHAR(36);

ALTER TABLE operational_notifications ADD COLUMN IF NOT EXISTS audience_group_id VARCHAR(36);
ALTER TABLE operational_notifications ADD COLUMN IF NOT EXISTS audience_contact_id VARCHAR(36);
ALTER TABLE operational_notifications ADD COLUMN IF NOT EXISTS delivery_channel VARCHAR(32);
ALTER TABLE operational_notifications ADD COLUMN IF NOT EXISTS delivery_status_detail VARCHAR(255);
ALTER TABLE operational_notifications ADD COLUMN IF NOT EXISTS source_action_code VARCHAR(32);

ALTER TABLE supply_requests ADD COLUMN IF NOT EXISTS source_action_code VARCHAR(32);
ALTER TABLE supply_requests ADD COLUMN IF NOT EXISTS priority VARCHAR(16);
ALTER TABLE supply_requests ADD COLUMN IF NOT EXISTS requested_needed_by TIMESTAMP;
ALTER TABLE supply_requests ADD COLUMN IF NOT EXISTS linked_recommendation_inventory_item_id VARCHAR(36);

ALTER TABLE operational_recommendations
    ADD FOREIGN KEY (primary_department_resource_id) REFERENCES hospital_department_resources(id);
ALTER TABLE operational_recommendations
    ADD FOREIGN KEY (primary_staffing_profile_id) REFERENCES hospital_staffing_profiles(id);
ALTER TABLE operational_recommendations
    ADD FOREIGN KEY (primary_inventory_item_id) REFERENCES hospital_inventory_items(id);
ALTER TABLE operational_recommendations
    ADD FOREIGN KEY (assigned_owner_user_id) REFERENCES users(id);

ALTER TABLE operational_tasks
    ADD FOREIGN KEY (owner_contact_id) REFERENCES hospital_operational_contacts(id);
ALTER TABLE operational_tasks
    ADD FOREIGN KEY (owner_group_id) REFERENCES hospital_operational_groups(id);
ALTER TABLE operational_tasks
    ADD FOREIGN KEY (recommended_by_recommendation_id) REFERENCES operational_recommendations(id);

ALTER TABLE operational_notifications
    ADD FOREIGN KEY (audience_group_id) REFERENCES hospital_operational_groups(id);
ALTER TABLE operational_notifications
    ADD FOREIGN KEY (audience_contact_id) REFERENCES hospital_operational_contacts(id);

ALTER TABLE supply_requests
    ADD FOREIGN KEY (linked_recommendation_inventory_item_id) REFERENCES hospital_inventory_items(id);

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
