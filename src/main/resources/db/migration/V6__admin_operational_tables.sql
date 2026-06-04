-- V5: Hospital Admin Operational Intelligence Module
-- Adds tables for resource state, recommendations, and workflow actions.

CREATE TABLE IF NOT EXISTS hospital_resource_snapshots (
    id                        VARCHAR(36)  NOT NULL PRIMARY KEY,
    hospital_id               VARCHAR(36)  NOT NULL,
    captured_at               TIMESTAMP    NOT NULL,
    total_beds                INT          NOT NULL DEFAULT 0,
    available_beds            INT          NOT NULL DEFAULT 0,
    icu_total_beds            INT          NOT NULL DEFAULT 0,
    icu_available_beds        INT          NOT NULL DEFAULT 0,
    isolation_rooms_total     INT          NOT NULL DEFAULT 0,
    isolation_rooms_available INT          NOT NULL DEFAULT 0,
    oxygen_capacity_units     INT          NOT NULL DEFAULT 0,
    oxygen_available_units    INT          NOT NULL DEFAULT 0,
    doctors_on_shift          INT          NOT NULL DEFAULT 0,
    nurses_on_shift           INT          NOT NULL DEFAULT 0,
    specialists_on_shift      INT          NOT NULL DEFAULT 0,
    source                    VARCHAR(16)  NOT NULL DEFAULT 'MANUAL',
    created_at                TIMESTAMP    NOT NULL,
    CONSTRAINT fk_hrs_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
);

CREATE TABLE IF NOT EXISTS hospital_department_resources (
    id               VARCHAR(36)  NOT NULL PRIMARY KEY,
    hospital_id      VARCHAR(36)  NOT NULL,
    department_code  VARCHAR(32)  NOT NULL,
    department_name  VARCHAR(128) NOT NULL,
    level_label      VARCHAR(64),
    total_beds       INT          NOT NULL DEFAULT 0,
    occupied_beds    INT          NOT NULL DEFAULT 0,
    status           VARCHAR(32)  NOT NULL DEFAULT 'NORMAL',
    notes            VARCHAR(512),
    updated_at       TIMESTAMP    NOT NULL,
    CONSTRAINT fk_hdr_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
);

CREATE TABLE IF NOT EXISTS hospital_staffing_profiles (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    hospital_id     VARCHAR(36)  NOT NULL,
    role_code       VARCHAR(32)  NOT NULL,
    role_name       VARCHAR(128) NOT NULL,
    headcount       INT          NOT NULL DEFAULT 0,
    on_shift_count  INT          NOT NULL DEFAULT 0,
    on_call_count   INT          NOT NULL DEFAULT 0,
    standby_count   INT          NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP    NOT NULL,
    CONSTRAINT fk_hsp_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
);

CREATE TABLE IF NOT EXISTS hospital_inventory_items (
    id                  VARCHAR(36)  NOT NULL PRIMARY KEY,
    hospital_id         VARCHAR(36)  NOT NULL,
    item_code           VARCHAR(32)  NOT NULL,
    item_name           VARCHAR(128) NOT NULL,
    category            VARCHAR(64),
    location            VARCHAR(128),
    current_quantity    INT          NOT NULL DEFAULT 0,
    capacity_quantity   INT          NOT NULL DEFAULT 0,
    unit                VARCHAR(32),
    critical_threshold  INT          NOT NULL DEFAULT 0,
    target_quantity     INT          NOT NULL DEFAULT 0,
    status              VARCHAR(32)  NOT NULL DEFAULT 'ADEQUATE',
    updated_at          TIMESTAMP    NOT NULL,
    CONSTRAINT fk_hii_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
);

CREATE TABLE IF NOT EXISTS operational_recommendations (
    id                        VARCHAR(36)  NOT NULL PRIMARY KEY,
    hospital_id               VARCHAR(36)  NOT NULL,
    source_alert_id           VARCHAR(36),
    source_outbreak_id        VARCHAR(36),
    type                      VARCHAR(32)  NOT NULL,
    severity                  VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM',
    status                    VARCHAR(32)  NOT NULL DEFAULT 'NEW',
    category                  VARCHAR(64),
    title                     VARCHAR(255) NOT NULL,
    description               TEXT,
    expected_impact           VARCHAR(512),
    urgency_window            VARCHAR(128),
    confidence_score          DECIMAL(5,2),
    image_mode                VARCHAR(32),
    rationale_json            TEXT,
    recommended_actions_json  TEXT,
    affected_departments_json TEXT,
    affected_resources_json   TEXT,
    model_provider            VARCHAR(64),
    model_version             VARCHAR(64),
    input_context_json        TEXT,
    created_by_mode           VARCHAR(32)  NOT NULL DEFAULT 'RULE_ENGINE',
    created_at                TIMESTAMP    NOT NULL,
    updated_at                TIMESTAMP    NOT NULL,
    resolved_at               TIMESTAMP,
    CONSTRAINT fk_or_hospital  FOREIGN KEY (hospital_id)        REFERENCES hospitals(id),
    CONSTRAINT fk_or_alert     FOREIGN KEY (source_alert_id)    REFERENCES alerts(id),
    CONSTRAINT fk_or_outbreak  FOREIGN KEY (source_outbreak_id) REFERENCES outbreaks(id)
);

CREATE TABLE IF NOT EXISTS operational_recommendation_audit (
    id                  VARCHAR(36)  NOT NULL PRIMARY KEY,
    recommendation_id   VARCHAR(36)  NOT NULL,
    actor_user_id       VARCHAR(36),
    event_type          VARCHAR(32)  NOT NULL,
    event_label         VARCHAR(255),
    event_payload_json  TEXT,
    created_at          TIMESTAMP    NOT NULL,
    CONSTRAINT fk_ora_recommendation FOREIGN KEY (recommendation_id) REFERENCES operational_recommendations(id)
);

CREATE TABLE IF NOT EXISTS operational_tasks (
    id                  VARCHAR(36)  NOT NULL PRIMARY KEY,
    recommendation_id   VARCHAR(36)  NOT NULL,
    hospital_id         VARCHAR(36)  NOT NULL,
    owner_user_id       VARCHAR(36),
    owner_label         VARCHAR(255),
    department_label    VARCHAR(255),
    deadline_at         TIMESTAMP,
    priority            VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM',
    notes               TEXT,
    status              VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    created_by_user_id  VARCHAR(36),
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL,
    CONSTRAINT fk_ot_recommendation FOREIGN KEY (recommendation_id) REFERENCES operational_recommendations(id),
    CONSTRAINT fk_ot_hospital        FOREIGN KEY (hospital_id)       REFERENCES hospitals(id)
);

CREATE TABLE IF NOT EXISTS operational_notifications (
    id                VARCHAR(36)  NOT NULL PRIMARY KEY,
    recommendation_id VARCHAR(36)  NOT NULL,
    hospital_id       VARCHAR(36)  NOT NULL,
    audience_label    VARCHAR(255),
    message           TEXT,
    status            VARCHAR(32)  NOT NULL DEFAULT 'SENT',
    sent_by_user_id   VARCHAR(36),
    sent_at           TIMESTAMP    NOT NULL,
    CONSTRAINT fk_on_recommendation FOREIGN KEY (recommendation_id) REFERENCES operational_recommendations(id),
    CONSTRAINT fk_on_hospital        FOREIGN KEY (hospital_id)       REFERENCES hospitals(id)
);

CREATE TABLE IF NOT EXISTS supply_requests (
    id                    VARCHAR(36)  NOT NULL PRIMARY KEY,
    recommendation_id     VARCHAR(36),
    hospital_id           VARCHAR(36)  NOT NULL,
    inventory_item_id     VARCHAR(36),
    supply_type_label     VARCHAR(255),
    quantity              INT          NOT NULL DEFAULT 0,
    unit                  VARCHAR(32),
    destination           VARCHAR(255),
    suggested_supplier    VARCHAR(255),
    status                VARCHAR(32)  NOT NULL DEFAULT 'REQUESTED',
    requested_by_user_id  VARCHAR(36),
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP    NOT NULL,
    CONSTRAINT fk_sr_recommendation FOREIGN KEY (recommendation_id) REFERENCES operational_recommendations(id),
    CONSTRAINT fk_sr_hospital        FOREIGN KEY (hospital_id)       REFERENCES hospitals(id),
    CONSTRAINT fk_sr_inventory       FOREIGN KEY (inventory_item_id) REFERENCES hospital_inventory_items(id)
);
