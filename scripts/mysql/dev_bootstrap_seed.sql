USE statusscope;

-- Full bootstrap seed for local/dev-style MySQL environments.
-- Purpose:
-- 1. Recreate the core tables this backend actively uses.
-- 2. Seed the same base catalog / RBAC / hospital data used by import.sql.
-- 3. Seed dev-style users analogous to DevSeeder.java.
--
-- Important:
-- - DevSeeder creates Firebase users first and stores the real Firebase UID in users.external_auth_id.
-- - A pure SQL file cannot create Firebase accounts, so this script uses deterministic placeholder
--   external_auth_id values. If you want these seeded users to authenticate through Firebase, create
--   matching Firebase users and update external_auth_id accordingly.
-- - This file intentionally focuses on the schema and rows required by the currently wired app paths:
--   auth, hospitals, doctor/admin operational module, and a minimal disease catalog.

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS supply_requests;
DROP TABLE IF EXISTS operational_notifications;
DROP TABLE IF EXISTS operational_tasks;
DROP TABLE IF EXISTS operational_recommendation_audit;
DROP TABLE IF EXISTS operational_recommendations;
DROP TABLE IF EXISTS hospital_inventory_items;
DROP TABLE IF EXISTS hospital_staffing_profiles;
DROP TABLE IF EXISTS hospital_department_resources;
DROP TABLE IF EXISTS hospital_resource_snapshots;
DROP TABLE IF EXISTS alerts;
DROP TABLE IF EXISTS outbreaks;
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS role_privileges;
DROP TABLE IF EXISTS privileges;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS hospitals;
DROP TABLE IF EXISTS disease_symptoms;
DROP TABLE IF EXISTS disease_specialties;
DROP TABLE IF EXISTS diseases;
DROP TABLE IF EXISTS symptoms;
DROP TABLE IF EXISTS specialties;
DROP TABLE IF EXISTS municipalities;
DROP TABLE IF EXISTS states;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE states (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL
) ENGINE=InnoDB;

CREATE TABLE municipalities (
    id          VARCHAR(36)   NOT NULL PRIMARY KEY,
    code        VARCHAR(32)   NOT NULL UNIQUE,
    name        VARCHAR(128)  NOT NULL,
    state_id    VARCHAR(36)   NOT NULL,
    latitude    DECIMAL(10,7),
    longitude   DECIMAL(10,7),
    created_at  DATETIME(6)   NOT NULL,
    updated_at  DATETIME(6)   NOT NULL,
    CONSTRAINT fk_municipality_state FOREIGN KEY (state_id) REFERENCES states(id)
) ENGINE=InnoDB;

CREATE TABLE specialties (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL
) ENGINE=InnoDB;

CREATE TABLE symptoms (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL UNIQUE,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    INDEX idx_symptoms_code (code)
) ENGINE=InnoDB;

CREATE TABLE diseases (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    code          VARCHAR(32)  NOT NULL UNIQUE,
    name          VARCHAR(128) NOT NULL,
    specialty_id  VARCHAR(36)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT fk_disease_primary_specialty FOREIGN KEY (specialty_id) REFERENCES specialties(id)
) ENGINE=InnoDB;

CREATE TABLE disease_specialties (
    disease_id   VARCHAR(36) NOT NULL,
    specialty_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (disease_id, specialty_id),
    CONSTRAINT fk_ds_disease FOREIGN KEY (disease_id) REFERENCES diseases(id),
    CONSTRAINT fk_ds_specialty FOREIGN KEY (specialty_id) REFERENCES specialties(id)
) ENGINE=InnoDB;

CREATE TABLE disease_symptoms (
    disease_id VARCHAR(36) NOT NULL,
    symptom_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (disease_id, symptom_id),
    INDEX idx_disease_symptoms_disease (disease_id),
    INDEX idx_disease_symptoms_symptom (symptom_id),
    CONSTRAINT fk_dsym_disease FOREIGN KEY (disease_id) REFERENCES diseases(id),
    CONSTRAINT fk_dsym_symptom FOREIGN KEY (symptom_id) REFERENCES symptoms(id)
) ENGINE=InnoDB;

CREATE TABLE roles (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    code        VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(255),
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL
) ENGINE=InnoDB;

CREATE TABLE privileges (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    code        VARCHAR(128) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL
) ENGINE=InnoDB;

CREATE TABLE role_privileges (
    role_id      VARCHAR(36) NOT NULL,
    privilege_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (role_id, privilege_id),
    CONSTRAINT fk_role_privileges_role FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_role_privileges_privilege FOREIGN KEY (privilege_id) REFERENCES privileges(id)
) ENGINE=InnoDB;

CREATE TABLE hospitals (
    id              VARCHAR(36)   NOT NULL PRIMARY KEY,
    code            VARCHAR(32)   NOT NULL UNIQUE,
    name            VARCHAR(255)  NOT NULL,
    address         VARCHAR(512),
    phone           VARCHAR(32),
    invite_code     VARCHAR(64)   UNIQUE,
    active          BIT(1)        NOT NULL,
    postal_code     VARCHAR(16),
    bed_count       INT,
    doctor_count    INT,
    nurse_count     INT,
    municipality_id VARCHAR(36),
    latitude        DECIMAL(10,7),
    longitude       DECIMAL(10,7),
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    CONSTRAINT fk_hospital_municipality FOREIGN KEY (municipality_id) REFERENCES municipalities(id)
) ENGINE=InnoDB;

CREATE TABLE users (
    id               VARCHAR(36)                                  NOT NULL PRIMARY KEY,
    full_name        VARCHAR(255)                                 NOT NULL,
    email            VARCHAR(255)                                 NOT NULL UNIQUE,
    active           BIT(1)                                       NOT NULL,
    external_auth_id VARCHAR(128)                                 NOT NULL UNIQUE,
    hospital_id      VARCHAR(36),
    status           ENUM ('ACTIVE', 'DISABLED', 'PENDING')       NOT NULL,
    last_login_at    DATETIME(6),
    license_number   VARCHAR(64),
    specialty_id     VARCHAR(36),
    created_at       DATETIME(6)                                  NOT NULL,
    updated_at       DATETIME(6)                                  NOT NULL,
    CONSTRAINT fk_user_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    CONSTRAINT fk_user_specialty FOREIGN KEY (specialty_id) REFERENCES specialties(id)
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id)
) ENGINE=InnoDB;

CREATE TABLE outbreaks (
    id                  VARCHAR(36)                           NOT NULL PRIMARY KEY,
    disease_id          VARCHAR(36)                           NOT NULL,
    scope               VARCHAR(16)                           NOT NULL,
    municipality_id     VARCHAR(36),
    state_id            VARCHAR(36),
    case_count          INT                                   NOT NULL,
    confirmation_status VARCHAR(16)                           NOT NULL,
    status              VARCHAR(16)                           NOT NULL,
    started_at          DATETIME(6)                           NOT NULL,
    ended_at            DATETIME(6),
    created_at          DATETIME(6)                           NOT NULL,
    updated_at          DATETIME(6)                           NOT NULL,
    CONSTRAINT fk_outbreak_disease FOREIGN KEY (disease_id) REFERENCES diseases(id),
    CONSTRAINT fk_outbreak_municipality FOREIGN KEY (municipality_id) REFERENCES municipalities(id),
    CONSTRAINT fk_outbreak_state FOREIGN KEY (state_id) REFERENCES states(id)
) ENGINE=InnoDB;

CREATE TABLE alerts (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    outbreak_id     VARCHAR(36)  NOT NULL,
    severity        VARCHAR(16)  NOT NULL,
    message         VARCHAR(512),
    acknowledged_at DATETIME(6),
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_alert_outbreak FOREIGN KEY (outbreak_id) REFERENCES outbreaks(id)
) ENGINE=InnoDB;

CREATE TABLE hospital_resource_snapshots (
    id                        VARCHAR(36) NOT NULL PRIMARY KEY,
    hospital_id               VARCHAR(36) NOT NULL,
    captured_at               DATETIME(6) NOT NULL,
    total_beds                INT         NOT NULL DEFAULT 0,
    available_beds            INT         NOT NULL DEFAULT 0,
    icu_total_beds            INT         NOT NULL DEFAULT 0,
    icu_available_beds        INT         NOT NULL DEFAULT 0,
    isolation_rooms_total     INT         NOT NULL DEFAULT 0,
    isolation_rooms_available INT         NOT NULL DEFAULT 0,
    oxygen_capacity_units     INT         NOT NULL DEFAULT 0,
    oxygen_available_units    INT         NOT NULL DEFAULT 0,
    doctors_on_shift          INT         NOT NULL DEFAULT 0,
    nurses_on_shift           INT         NOT NULL DEFAULT 0,
    specialists_on_shift      INT         NOT NULL DEFAULT 0,
    source                    VARCHAR(16) NOT NULL,
    created_at                DATETIME(6) NOT NULL,
    CONSTRAINT fk_hrs_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
) ENGINE=InnoDB;

CREATE TABLE hospital_department_resources (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    hospital_id     VARCHAR(36)  NOT NULL,
    department_code VARCHAR(32)  NOT NULL,
    department_name VARCHAR(128) NOT NULL,
    level_label     VARCHAR(64),
    total_beds      INT          NOT NULL DEFAULT 0,
    occupied_beds   INT          NOT NULL DEFAULT 0,
    status          VARCHAR(32)  NOT NULL,
    notes           VARCHAR(512),
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT fk_hdr_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
) ENGINE=InnoDB;

CREATE TABLE hospital_staffing_profiles (
    id             VARCHAR(36)  NOT NULL PRIMARY KEY,
    hospital_id    VARCHAR(36)  NOT NULL,
    role_code      VARCHAR(32)  NOT NULL,
    role_name      VARCHAR(128) NOT NULL,
    headcount      INT          NOT NULL DEFAULT 0,
    on_shift_count INT          NOT NULL DEFAULT 0,
    on_call_count  INT          NOT NULL DEFAULT 0,
    standby_count  INT          NOT NULL DEFAULT 0,
    updated_at     DATETIME(6)  NOT NULL,
    CONSTRAINT fk_hsp_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
) ENGINE=InnoDB;

CREATE TABLE hospital_inventory_items (
    id                 VARCHAR(36)  NOT NULL PRIMARY KEY,
    hospital_id        VARCHAR(36)  NOT NULL,
    item_code          VARCHAR(32)  NOT NULL,
    item_name          VARCHAR(128) NOT NULL,
    category           VARCHAR(64),
    location           VARCHAR(128),
    current_quantity   INT          NOT NULL DEFAULT 0,
    capacity_quantity  INT          NOT NULL DEFAULT 0,
    unit               VARCHAR(32),
    critical_threshold INT          NOT NULL DEFAULT 0,
    target_quantity    INT          NOT NULL DEFAULT 0,
    status             VARCHAR(32)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    CONSTRAINT fk_hii_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
) ENGINE=InnoDB;

CREATE TABLE operational_recommendations (
    id                        VARCHAR(36)  NOT NULL PRIMARY KEY,
    hospital_id               VARCHAR(36)  NOT NULL,
    source_alert_id           VARCHAR(36),
    source_outbreak_id        VARCHAR(36),
    type                      VARCHAR(32)  NOT NULL,
    severity                  VARCHAR(16)  NOT NULL,
    status                    VARCHAR(32)  NOT NULL,
    category                  VARCHAR(64),
    title                     VARCHAR(255) NOT NULL,
    description               TEXT,
    expected_impact           VARCHAR(512),
    urgency_window            VARCHAR(128),
    confidence_score          DECIMAL(5,2),
    content_translations_json  TEXT,
    image_mode                VARCHAR(32),
    rationale_json            TEXT,
    recommended_actions_json  TEXT,
    affected_departments_json TEXT,
    affected_resources_json   TEXT,
    model_provider            VARCHAR(64),
    model_version             VARCHAR(64),
    input_context_json        TEXT,
    created_by_mode           VARCHAR(32)  NOT NULL,
    created_at                DATETIME(6)  NOT NULL,
    updated_at                DATETIME(6)  NOT NULL,
    resolved_at               DATETIME(6),
    CONSTRAINT fk_or_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    CONSTRAINT fk_or_alert FOREIGN KEY (source_alert_id) REFERENCES alerts(id),
    CONSTRAINT fk_or_outbreak FOREIGN KEY (source_outbreak_id) REFERENCES outbreaks(id)
) ENGINE=InnoDB;

CREATE TABLE operational_recommendation_audit (
    id                 VARCHAR(36)  NOT NULL PRIMARY KEY,
    recommendation_id  VARCHAR(36)  NOT NULL,
    actor_user_id      VARCHAR(36),
    event_type         VARCHAR(32)  NOT NULL,
    event_label        VARCHAR(255),
    event_payload_json TEXT,
    created_at         DATETIME(6)  NOT NULL,
    CONSTRAINT fk_ora_recommendation FOREIGN KEY (recommendation_id) REFERENCES operational_recommendations(id)
) ENGINE=InnoDB;

CREATE TABLE operational_tasks (
    id                 VARCHAR(36)  NOT NULL PRIMARY KEY,
    recommendation_id  VARCHAR(36)  NOT NULL,
    hospital_id        VARCHAR(36)  NOT NULL,
    owner_user_id      VARCHAR(36),
    owner_label        VARCHAR(255),
    department_label   VARCHAR(255),
    deadline_at        DATETIME(6),
    priority           VARCHAR(16)  NOT NULL,
    notes              TEXT,
    status             VARCHAR(32)  NOT NULL,
    created_by_user_id VARCHAR(36),
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    CONSTRAINT fk_ot_recommendation FOREIGN KEY (recommendation_id) REFERENCES operational_recommendations(id),
    CONSTRAINT fk_ot_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
) ENGINE=InnoDB;

CREATE TABLE operational_notifications (
    id                VARCHAR(36)  NOT NULL PRIMARY KEY,
    recommendation_id VARCHAR(36)  NOT NULL,
    hospital_id       VARCHAR(36)  NOT NULL,
    audience_label    VARCHAR(255),
    message           TEXT,
    status            VARCHAR(32)  NOT NULL,
    sent_by_user_id   VARCHAR(36),
    sent_at           DATETIME(6)  NOT NULL,
    CONSTRAINT fk_on_recommendation FOREIGN KEY (recommendation_id) REFERENCES operational_recommendations(id),
    CONSTRAINT fk_on_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
) ENGINE=InnoDB;

CREATE TABLE supply_requests (
    id                   VARCHAR(36)  NOT NULL PRIMARY KEY,
    recommendation_id    VARCHAR(36)  NOT NULL,
    hospital_id          VARCHAR(36)  NOT NULL,
    inventory_item_id    VARCHAR(36),
    supply_type_label    VARCHAR(255),
    quantity             INT          NOT NULL DEFAULT 0,
    unit                 VARCHAR(32),
    destination          VARCHAR(255),
    suggested_supplier   VARCHAR(255),
    status               VARCHAR(32)  NOT NULL,
    requested_by_user_id VARCHAR(36),
    created_at           DATETIME(6)  NOT NULL,
    updated_at           DATETIME(6)  NOT NULL,
    CONSTRAINT fk_sr_recommendation FOREIGN KEY (recommendation_id) REFERENCES operational_recommendations(id),
    CONSTRAINT fk_sr_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    CONSTRAINT fk_sr_inventory FOREIGN KEY (inventory_item_id) REFERENCES hospital_inventory_items(id)
) ENGINE=InnoDB;

INSERT INTO states (id, code, name, description, created_at, updated_at) VALUES
('40000000-0000-0000-0000-000000000001', 'AGS',  'Aguascalientes', 'Estado de Aguascalientes', NOW(), NOW()),
('40000000-0000-0000-0000-000000000002', 'BC',   'Baja California', 'Estado de Baja California', NOW(), NOW()),
('40000000-0000-0000-0000-000000000003', 'BCS',  'Baja California Sur', 'Estado de Baja California Sur', NOW(), NOW()),
('40000000-0000-0000-0000-000000000004', 'CAM',  'Campeche', 'Estado de Campeche', NOW(), NOW()),
('40000000-0000-0000-0000-000000000005', 'COA',  'Coahuila de Zaragoza', 'Estado de Coahuila de Zaragoza', NOW(), NOW()),
('40000000-0000-0000-0000-000000000006', 'COL',  'Colima', 'Estado de Colima', NOW(), NOW()),
('40000000-0000-0000-0000-000000000007', 'CHP',  'Chiapas', 'Estado de Chiapas', NOW(), NOW()),
('40000000-0000-0000-0000-000000000008', 'CHH',  'Chihuahua', 'Estado de Chihuahua', NOW(), NOW()),
('40000000-0000-0000-0000-000000000009', 'CDMX', 'Ciudad de Mexico', 'Entidad federativa Ciudad de Mexico', NOW(), NOW()),
('40000000-0000-0000-0000-000000000010', 'DUR',  'Durango', 'Estado de Durango', NOW(), NOW()),
('40000000-0000-0000-0000-000000000011', 'GUA',  'Guanajuato', 'Estado de Guanajuato', NOW(), NOW()),
('40000000-0000-0000-0000-000000000012', 'GRO',  'Guerrero', 'Estado de Guerrero', NOW(), NOW()),
('40000000-0000-0000-0000-000000000013', 'HID',  'Hidalgo', 'Estado de Hidalgo', NOW(), NOW()),
('40000000-0000-0000-0000-000000000014', 'JAL',  'Jalisco', 'Estado de Jalisco', NOW(), NOW()),
('40000000-0000-0000-0000-000000000015', 'MEX',  'Mexico', 'Estado de Mexico', NOW(), NOW()),
('40000000-0000-0000-0000-000000000016', 'MIC',  'Michoacan de Ocampo', 'Estado de Michoacan de Ocampo', NOW(), NOW()),
('40000000-0000-0000-0000-000000000017', 'MOR',  'Morelos', 'Estado de Morelos', NOW(), NOW()),
('40000000-0000-0000-0000-000000000018', 'NAY',  'Nayarit', 'Estado de Nayarit', NOW(), NOW()),
('40000000-0000-0000-0000-000000000019', 'NL',   'Nuevo Leon', 'Estado de Nuevo Leon', NOW(), NOW()),
('40000000-0000-0000-0000-000000000020', 'OAX',  'Oaxaca', 'Estado de Oaxaca', NOW(), NOW()),
('40000000-0000-0000-0000-000000000021', 'PUE',  'Puebla', 'Estado de Puebla', NOW(), NOW()),
('40000000-0000-0000-0000-000000000022', 'QRO',  'Queretaro', 'Estado de Queretaro', NOW(), NOW()),
('40000000-0000-0000-0000-000000000023', 'ROO',  'Quintana Roo', 'Estado de Quintana Roo', NOW(), NOW()),
('40000000-0000-0000-0000-000000000024', 'SLP',  'San Luis Potosi', 'Estado de San Luis Potosi', NOW(), NOW()),
('40000000-0000-0000-0000-000000000025', 'SIN',  'Sinaloa', 'Estado de Sinaloa', NOW(), NOW()),
('40000000-0000-0000-0000-000000000026', 'SON',  'Sonora', 'Estado de Sonora', NOW(), NOW()),
('40000000-0000-0000-0000-000000000027', 'TAB',  'Tabasco', 'Estado de Tabasco', NOW(), NOW()),
('40000000-0000-0000-0000-000000000028', 'TAM',  'Tamaulipas', 'Estado de Tamaulipas', NOW(), NOW()),
('40000000-0000-0000-0000-000000000029', 'TLA',  'Tlaxcala', 'Estado de Tlaxcala', NOW(), NOW()),
('40000000-0000-0000-0000-000000000030', 'VER',  'Veracruz de Ignacio de la Llave', 'Estado de Veracruz de Ignacio de la Llave', NOW(), NOW()),
('40000000-0000-0000-0000-000000000031', 'YUC',  'Yucatan', 'Estado de Yucatan', NOW(), NOW()),
('40000000-0000-0000-0000-000000000032', 'ZAC',  'Zacatecas', 'Estado de Zacatecas', NOW(), NOW());

INSERT INTO municipalities (id, code, name, state_id, latitude, longitude, created_at, updated_at) VALUES
('42000000-0000-0000-0000-000000001003', '19039', 'Monterrey', '40000000-0000-0000-0000-000000000019', 25.6866142, -100.3161126, NOW(), NOW()),
('42000000-0000-0000-0000-000000000287', '09013', 'Xochimilco', '40000000-0000-0000-0000-000000000009', 19.2572310, -99.1037420, NOW(), NOW());

INSERT INTO specialties (id, code, name, description, created_at, updated_at) VALUES
('50000000-0000-0000-0000-000000000001', 'GEN', 'General Medicine', 'Primary care and general practice', NOW(), NOW()),
('50000000-0000-0000-0000-000000000002', 'CARD', 'Cardiology', 'Heart and circulatory conditions', NOW(), NOW()),
('50000000-0000-0000-0000-000000000003', 'PULM', 'Pulmonology', 'Respiratory system', NOW(), NOW()),
('50000000-0000-0000-0000-000000000004', 'INF', 'Infectious Diseases', 'Infectious and communicable diseases', NOW(), NOW()),
('50000000-0000-0000-0000-000000000005', 'PED', 'Pediatrics', 'Care of infants, children, and adolescents', NOW(), NOW()),
('50000000-0000-0000-0000-000000000006', 'GASTRO', 'Gastroenterology', 'Digestive system and gastrointestinal conditions', NOW(), NOW()),
('50000000-0000-0000-0000-000000000007', 'NEURO', 'Neurology', 'Brain, nervous system, and neurologic conditions', NOW(), NOW()),
('50000000-0000-0000-0000-000000000008', 'ENT', 'Otolaryngology', 'Ear, nose, throat, head and neck conditions', NOW(), NOW()),
('50000000-0000-0000-0000-000000000009', 'DERM', 'Dermatology', 'Skin, hair, and nail conditions', NOW(), NOW()),
('50000000-0000-0000-0000-000000000010', 'GYN', 'Gynecology', 'Female reproductive health conditions', NOW(), NOW()),
('50000000-0000-0000-0000-000000000011', 'URO', 'Urology', 'Urinary tract and male reproductive conditions', NOW(), NOW()),
('50000000-0000-0000-0000-000000000012', 'FAM', 'Family Medicine', 'Comprehensive primary care across ages', NOW(), NOW()),
('50000000-0000-0000-0000-000000000013', 'OPH', 'Ophthalmology', 'Eye and vision conditions', NOW(), NOW()),
('50000000-0000-0000-0000-000000000014', 'DENT', 'Dentistry', 'Oral and dental conditions', NOW(), NOW());

INSERT INTO symptoms (id, code, name, created_at, updated_at) VALUES
('51000000-0000-0000-0000-000000000001', 'FEVER', 'Fever', NOW(), NOW()),
('51000000-0000-0000-0000-000000000002', 'RASH', 'Rash', NOW(), NOW()),
('51000000-0000-0000-0000-000000000003', 'CONJUNCTIVITIS', 'Conjunctivitis', NOW(), NOW()),
('51000000-0000-0000-0000-000000000004', 'COUGH', 'Cough', NOW(), NOW());

INSERT INTO diseases (id, code, name, specialty_id, created_at, updated_at) VALUES
('60000000-0000-0000-0000-000000000004', 'COVID19', 'COVID-19', '50000000-0000-0000-0000-000000000004', NOW(), NOW()),
('60000000-0000-0000-0000-000000000005', 'MEASLES', 'Measles', '50000000-0000-0000-0000-000000000005', NOW(), NOW());

INSERT INTO disease_specialties (disease_id, specialty_id) VALUES
('60000000-0000-0000-0000-000000000004', '50000000-0000-0000-0000-000000000004'),
('60000000-0000-0000-0000-000000000005', '50000000-0000-0000-0000-000000000005');

INSERT INTO disease_symptoms (disease_id, symptom_id) VALUES
('60000000-0000-0000-0000-000000000004', '51000000-0000-0000-0000-000000000001'),
('60000000-0000-0000-0000-000000000004', '51000000-0000-0000-0000-000000000004'),
('60000000-0000-0000-0000-000000000005', '51000000-0000-0000-0000-000000000001'),
('60000000-0000-0000-0000-000000000005', '51000000-0000-0000-0000-000000000002'),
('60000000-0000-0000-0000-000000000005', '51000000-0000-0000-0000-000000000003');

INSERT INTO roles (id, code, name, description, created_at, updated_at) VALUES
('00000000-0000-0000-0000-000000000001', 'SYSTEM_ADMIN', 'System Administrator', 'Platform-wide administrative access', NOW(), NOW()),
('00000000-0000-0000-0000-000000000002', 'HOSPITAL_ADMIN', 'Hospital Administrator', 'Administrative access scoped to one hospital', NOW(), NOW()),
('00000000-0000-0000-0000-000000000003', 'DOCTOR', 'Doctor', 'Clinical user scoped to one hospital', NOW(), NOW());

INSERT INTO privileges (id, code, description, created_at, updated_at) VALUES
('10000000-0000-0000-0000-000000000001', 'alerts.read', 'Read alerts', NOW(), NOW()),
('10000000-0000-0000-0000-000000000002', 'alerts.manage', 'Manage alerts', NOW(), NOW()),
('10000000-0000-0000-0000-000000000003', 'users.read', 'Read users', NOW(), NOW()),
('10000000-0000-0000-0000-000000000004', 'users.manage', 'Create/update users', NOW(), NOW()),
('10000000-0000-0000-0000-000000000005', 'roles.manage', 'Manage RBAC roles', NOW(), NOW()),
('10000000-0000-0000-0000-000000000006', 'diagnosis.create', 'Create diagnosis entries', NOW(), NOW()),
('10000000-0000-0000-0000-000000000007', 'hospitals.read', 'Read hospitals', NOW(), NOW()),
('10000000-0000-0000-0000-000000000008', 'hospitals.manage', 'Create/update hospitals', NOW(), NOW()),
('10000000-0000-0000-0000-000000000009', 'patients.read', 'Read patients', NOW(), NOW()),
('10000000-0000-0000-0000-000000000010', 'patients.manage', 'Create/update patients', NOW(), NOW()),
('10000000-0000-0000-0000-000000000011', 'diseases.read', 'Read diseases', NOW(), NOW()),
('10000000-0000-0000-0000-000000000012', 'diseases.manage', 'Manage diseases catalog', NOW(), NOW()),
('10000000-0000-0000-0000-000000000013', 'specialties.read', 'Read specialties', NOW(), NOW()),
('10000000-0000-0000-0000-000000000014', 'specialties.manage', 'Manage specialties catalog', NOW(), NOW()),
('10000000-0000-0000-0000-000000000015', 'outbreaks.read', 'Read outbreaks', NOW(), NOW()),
('10000000-0000-0000-0000-000000000016', 'outbreaks.manage', 'Manage outbreaks', NOW(), NOW()),
('10000000-0000-0000-0000-000000000017', 'states.read', 'Read states', NOW(), NOW()),
('10000000-0000-0000-0000-000000000018', 'states.manage', 'Manage states', NOW(), NOW()),
('10000000-0000-0000-0000-000000000019', 'diagnosis.assist', 'Use the AI diagnosis assistant chat', NOW(), NOW()),
('10000000-0000-0000-0000-000000000020', 'admin.operations', 'Access hospital admin operational features', NOW(), NOW()),
('10000000-0000-0000-0000-000000000021', 'isSystemAdmin', 'Access system administrator features', NOW(), NOW());

INSERT INTO role_privileges (role_id, privilege_id) VALUES
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000002'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000003'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000005'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000006'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000007'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000008'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000009'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000010'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000011'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000012'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000013'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000014'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000015'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000016'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000017'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000018'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000019'),
('00000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000021'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000002'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000003'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000004'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000007'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000009'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000010'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000011'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000013'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000015'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000016'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000017'),
('00000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000020'),
('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001'),
('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000006'),
('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000009'),
('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000010'),
('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000011'),
('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000013'),
('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000015'),
('00000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000019');

INSERT INTO hospitals (
    id, code, name, address, phone, invite_code, active, postal_code,
    bed_count, doctor_count, nurse_count, municipality_id, latitude, longitude, created_at, updated_at
) VALUES
('30000000-0000-0000-0000-000000000001', 'HGZ-21', 'Hospital General Zona 21', 'Av. Principal 100, Monterrey', '+52 81 0000 0001', 'INVITE-HGZ21', b'1', '64000', 240, 72, 180, '42000000-0000-0000-0000-000000001003', 25.6866142, -100.3161126, NOW(), NOW()),
('30000000-0000-0000-0000-000000000002', 'HRE-05', 'Hospital Regional Este', 'Calle 5 Ote 200, Xochimilco', '+52 55 0000 0002', 'INVITE-HRE05', b'1', '16000', 320, 96, 240, '42000000-0000-0000-0000-000000000287', 19.2572310, -99.1037420, NOW(), NOW());

INSERT INTO users (
    id, full_name, email, active, external_auth_id, hospital_id, status, last_login_at,
    license_number, specialty_id, created_at, updated_at
) VALUES
('70000000-0000-0000-0000-000000000001', 'System Admin', 'admin@statusscope.local', b'1', 'seed-system-admin', NULL, 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
('70000000-0000-0000-0000-000000000002', 'Admin HGZ-21', 'admin.hgz21@statusscope.local', b'1', 'seed-admin-hgz21', '30000000-0000-0000-0000-000000000001', 'ACTIVE', NULL, NULL, NULL, NOW(), NOW()),
('70000000-0000-0000-0000-000000000003', 'Admin HRE-05', 'admin.hre05@statusscope.local', b'1', 'seed-admin-hre05', '30000000-0000-0000-0000-000000000002', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, NOW(), NOW()),
('70000000-0000-0000-0000-000000000004', 'Dra. Ana Lopez', 'doctor1@statusscope.local', b'1', 'seed-doctor-1', '30000000-0000-0000-0000-000000000001', 'ACTIVE', NULL, NULL, '50000000-0000-0000-0000-000000000001', NOW(), NOW()),
('70000000-0000-0000-0000-000000000005', 'Dr. Luis Perez', 'doctor2@statusscope.local', b'1', 'seed-doctor-2', '30000000-0000-0000-0000-000000000002', 'ACTIVE', DATE_SUB(NOW(), INTERVAL 2 HOUR), NULL, '50000000-0000-0000-0000-000000000001', NOW(), NOW());

INSERT INTO user_roles (user_id, role_id) VALUES
('70000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),
('70000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002'),
('70000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000002'),
('70000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000003'),
('70000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000003');

INSERT INTO outbreaks (
    id, disease_id, scope, municipality_id, state_id, case_count,
    confirmation_status, status, started_at, ended_at, created_at, updated_at
) VALUES
('71000000-0000-0000-0000-000000000501', '60000000-0000-0000-0000-000000000004', 'MUNICIPALITY', '42000000-0000-0000-0000-000000001003', NULL, 72, 'CONFIRMED', 'ACTIVE', NOW() - INTERVAL 2 DAY, NULL, NOW(), NOW());

INSERT INTO hospital_resource_snapshots (
    id, hospital_id, captured_at, total_beds, available_beds, icu_total_beds, icu_available_beds,
    isolation_rooms_total, isolation_rooms_available, oxygen_capacity_units, oxygen_available_units,
    doctors_on_shift, nurses_on_shift, specialists_on_shift, source, created_at
) VALUES
('20000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', NOW(), 240, 24, 20, 3, 10, 3, 500, 150, 42, 130, 15, 'MANUAL', NOW());

INSERT INTO hospital_department_resources (
    id, hospital_id, department_code, department_name, level_label, total_beds, occupied_beds, status, notes, updated_at
) VALUES
('21000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'EMERGENCY', 'Emergency Department', 'Level 1', 40, 35, 'HIGH_LOAD', 'Near capacity', NOW()),
('21000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', 'GENERAL', 'General Ward', 'Level 2', 80, 65, 'NORMAL', NULL, NOW()),
('21000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000001', 'ICU', 'Intensive Care Unit', 'Level 3', 20, 15, 'HIGH_LOAD', 'Surge expected', NOW()),
('21000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000001', 'RESPIRATORY', 'Respiratory Ward', 'Level 2', 30, 25, 'HIGH_LOAD', 'Outbreak isolation active', NOW()),
('21000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000001', 'PEDIATRICS', 'Pediatric Ward', 'Level 2', 25, 15, 'NORMAL', NULL, NOW());

INSERT INTO hospital_staffing_profiles (
    id, hospital_id, role_code, role_name, headcount, on_shift_count, on_call_count, standby_count, updated_at
) VALUES
('22000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'EMERGENCY_PHYSICIAN', 'Emergency Physicians', 10, 4, 3, 3, NOW()),
('22000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', 'ICU_NURSE', 'ICU Nurses', 18, 6, 6, 6, NOW()),
('22000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000001', 'PULMONOLOGIST', 'Pulmonologists', 5, 2, 2, 1, NOW()),
('22000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000001', 'INFECTIOUS_DISEASE', 'Infectious Disease Specialists', 4, 2, 1, 1, NOW()),
('22000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000001', 'GENERAL_PRACTITIONER', 'General Practitioners', 30, 12, 9, 9, NOW()),
('22000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000001', 'NURSING_STAFF', 'Nursing Staff', 80, 30, 25, 25, NOW());

INSERT INTO hospital_inventory_items (
    id, hospital_id, item_code, item_name, category, location, current_quantity, capacity_quantity, unit,
    critical_threshold, target_quantity, status, updated_at
) VALUES
('23000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'N95_MASK', 'N95 Respirator Masks', 'PPE', 'Central Supply', 500, 2000, 'units', 200, 1500, 'LOW', NOW()),
('23000000-0000-0000-0000-000000000002', '30000000-0000-0000-0000-000000000001', 'VENTILATOR', 'Mechanical Ventilators', 'EQUIPMENT', 'ICU', 8, 20, 'units', 3, 15, 'NORMAL', NOW()),
('23000000-0000-0000-0000-000000000003', '30000000-0000-0000-0000-000000000001', 'O2_CYLINDER', 'Oxygen Cylinders', 'SUPPLY', 'Gas Room', 80, 200, 'cylinders', 40, 150, 'NORMAL', NOW()),
('23000000-0000-0000-0000-000000000004', '30000000-0000-0000-0000-000000000001', 'IV_FLUID', 'IV Saline Solution', 'PHARMACEUTICAL', 'Pharmacy', 300, 1000, 'bags', 100, 800, 'NORMAL', NOW()),
('23000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000001', 'ISO_GOWN', 'Isolation Gowns', 'PPE', 'Central Supply', 150, 1000, 'units', 200, 700, 'CRITICAL', NOW()),
('23000000-0000-0000-0000-000000000006', '30000000-0000-0000-0000-000000000001', 'ANTIVIRAL', 'Antiviral Medications', 'PHARMACEUTICAL', 'Pharmacy', 60, 300, 'doses', 50, 250, 'LOW', NOW());

-- Operational recommendations are intentionally not seeded here. They are generated
-- from live hospital signals by the recommendation refresh use case.
