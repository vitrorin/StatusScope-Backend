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

-- NOTE: The new columns and foreign keys for the admin operational workflow
-- are declared on the JPA entities, so Hibernate schema-management creates them
-- (drop-and-create / update) or asserts them (validate) before this migration
-- runs in every profile. The original `ALTER TABLE ... ADD COLUMN/CONSTRAINT
-- IF NOT EXISTS` statements used syntax accepted by H2 (tests/prod) but rejected
-- by MySQL 8 (local dev), which broke startup. They were redundant with the
-- entity-managed schema and have been removed so this migration runs on both
-- MySQL and H2. The data-seeding UPDATEs below remain.

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
