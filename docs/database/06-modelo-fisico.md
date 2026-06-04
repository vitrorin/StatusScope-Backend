# Modelo Físico — StatusScope en MySQL 8.0+ / Cloud SQL

## 1. Tablas implementadas

### Seguridad y autenticación

#### users
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| full_name | VARCHAR(255) | NO | — | |
| email | VARCHAR(255) | NO | — | UNIQUE |
| active | BIT(1) | NO | — | |
| external_auth_id | VARCHAR(128) | NO | — | UNIQUE |
| hospital_id | VARCHAR(36) | YES | NULL | FK → hospitals(id) |
| status | ENUM('ACTIVE','DISABLED','PENDING') | NO | — | |
| last_login_at | DATETIME(6) | YES | NULL | |
| license_number | VARCHAR(64) | YES | NULL | |
| specialty_id | VARCHAR(36) | YES | NULL | FK → specialties(id) |
| created_at | DATETIME(6) | NO | — | |
| updated_at | DATETIME(6) | NO | — | |

#### roles
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| code | VARCHAR(64) | NO | — | UNIQUE |
| name | VARCHAR(128) | NO | — | |
| description | VARCHAR(255) | YES | NULL | |
| created_at | DATETIME(6) | NO | — | |
| updated_at | DATETIME(6) | NO | — | |

#### privileges
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| code | VARCHAR(128) | NO | — | UNIQUE |
| description | VARCHAR(255) | YES | NULL | |
| created_at | DATETIME(6) | NO | — | |
| updated_at | DATETIME(6) | NO | — | |

#### user_roles
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| user_id | VARCHAR(36) | NO | — | FK → users(id) |
| role_id | VARCHAR(36) | NO | — | FK → roles(id) |

PK compuesta: (user_id, role_id)

#### role_privileges
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| role_id | VARCHAR(36) | NO | — | FK → roles(id) |
| privilege_id | VARCHAR(36) | NO | — | FK → privileges(id) |

PK compuesta: (role_id, privilege_id)

---

### Geografía

#### states
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| code | VARCHAR(32) | NO | — | UNIQUE |
| name | VARCHAR(128) | NO | — | |
| description | VARCHAR(255) | YES | NULL | |
| created_at | DATETIME(6) | NO | — | |
| updated_at | DATETIME(6) | NO | — | |

#### municipalities
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| code | VARCHAR(32) | NO | — | UNIQUE |
| name | VARCHAR(128) | NO | — | |
| state_id | VARCHAR(36) | NO | — | FK → states(id) |
| latitude | DECIMAL(10,7) | YES | NULL | |
| longitude | DECIMAL(10,7) | YES | NULL | |
| created_at | DATETIME(6) | NO | — | |
| updated_at | DATETIME(6) | NO | — | |

---

### Hospitales

#### hospitals
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| code | VARCHAR(32) | NO | — | UNIQUE |
| name | VARCHAR(255) | NO | — | |
| address | VARCHAR(512) | YES | NULL | |
| phone | VARCHAR(32) | YES | NULL | |
| invite_code | VARCHAR(64) | YES | NULL | UNIQUE |
| active | BIT(1) | NO | — | |
| postal_code | VARCHAR(16) | YES | NULL | |
| bed_count | INT | YES | NULL | |
| doctor_count | INT | YES | NULL | |
| nurse_count | INT | YES | NULL | |
| municipality_id | VARCHAR(36) | YES | NULL | FK → municipalities(id) |
| latitude | DECIMAL(10,7) | YES | NULL | |
| longitude | DECIMAL(10,7) | YES | NULL | |
| created_at | DATETIME(6) | NO | — | |
| updated_at | DATETIME(6) | NO | — | |

---

### Epidemiología

#### diseases
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| code | VARCHAR(32) | NO | — | UNIQUE |
| name | VARCHAR(128) | NO | — | |
| specialty_id | VARCHAR(36) | NO | — | FK → specialties(id) |
| created_at | DATETIME(6) | NO | — | |
| updated_at | DATETIME(6) | NO | — | |

#### symptoms
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| code | VARCHAR(32) | NO | — | UNIQUE |
| name | VARCHAR(255) | NO | — | UNIQUE |
| created_at | DATETIME(6) | NO | — | |
| updated_at | DATETIME(6) | NO | — | |

#### disease_symptoms
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| disease_id | VARCHAR(36) | NO | — | FK → diseases(id) |
| symptom_id | VARCHAR(36) | NO | — | FK → symptoms(id) |

PK compuesta: (disease_id, symptom_id)

#### outbreaks
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| disease_id | VARCHAR(36) | NO | — | FK → diseases(id) |
| scope | VARCHAR(16) | NO | — | |
| municipality_id | VARCHAR(36) | YES | NULL | FK → municipalities(id) |
| state_id | VARCHAR(36) | YES | NULL | FK → states(id) |
| case_count | INT | NO | — | |
| confirmation_status | VARCHAR(16) | NO | — | |
| status | VARCHAR(16) | NO | — | |
| started_at | DATETIME(6) | NO | — | |
| ended_at | DATETIME(6) | YES | NULL | |
| created_at | DATETIME(6) | NO | — | |
| updated_at | DATETIME(6) | NO | — | |

#### alerts
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| outbreak_id | VARCHAR(36) | NO | — | FK → outbreaks(id) |
| severity | VARCHAR(16) | NO | — | |
| message | VARCHAR(512) | YES | NULL | |
| acknowledged_at | DATETIME(6) | YES | NULL | |
| created_at | DATETIME(6) | NO | — | |

---

### Operación hospitalaria

#### hospital_resource_snapshots
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| hospital_id | VARCHAR(36) | NO | — | FK → hospitals(id) |
| captured_at | TIMESTAMP | NO | — | |
| total_beds | INT | NO | 0 | |
| available_beds | INT | NO | 0 | |
| icu_total_beds | INT | NO | 0 | |
| icu_available_beds | INT | NO | 0 | |
| isolation_rooms_total | INT | NO | 0 | |
| isolation_rooms_available | INT | NO | 0 | |
| oxygen_capacity_units | INT | NO | 0 | |
| oxygen_available_units | INT | NO | 0 | |
| doctors_on_shift | INT | NO | 0 | |
| nurses_on_shift | INT | NO | 0 | |
| specialists_on_shift | INT | NO | 0 | |
| source | VARCHAR(16) | NO | 'MANUAL' | |
| created_at | TIMESTAMP | NO | — | |

#### hospital_department_resources
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| hospital_id | VARCHAR(36) | NO | — | FK → hospitals(id) |
| department_code | VARCHAR(32) | NO | — | |
| department_name | VARCHAR(128) | NO | — | |
| level_label | VARCHAR(64) | YES | NULL | |
| total_beds | INT | NO | 0 | |
| occupied_beds | INT | NO | 0 | |
| status | VARCHAR(32) | NO | 'NORMAL' | |
| notes | VARCHAR(512) | YES | NULL | |
| updated_at | TIMESTAMP | NO | — | |

#### hospital_staffing_profiles
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| hospital_id | VARCHAR(36) | NO | — | FK → hospitals(id) |
| role_code | VARCHAR(32) | NO | — | |
| role_name | VARCHAR(128) | NO | — | |
| headcount | INT | NO | 0 | |
| on_shift_count | INT | NO | 0 | |
| on_call_count | INT | NO | 0 | |
| standby_count | INT | NO | 0 | |
| updated_at | TIMESTAMP | NO | — | |

#### hospital_inventory_items
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| hospital_id | VARCHAR(36) | NO | — | FK → hospitals(id) |
| item_code | VARCHAR(32) | NO | — | |
| item_name | VARCHAR(128) | NO | — | |
| category | VARCHAR(64) | YES | NULL | |
| location | VARCHAR(128) | YES | NULL | |
| current_quantity | INT | NO | 0 | |
| capacity_quantity | INT | NO | 0 | |
| unit | VARCHAR(32) | YES | NULL | |
| critical_threshold | INT | NO | 0 | |
| target_quantity | INT | NO | 0 | |
| status | VARCHAR(32) | NO | 'ADEQUATE' | |
| updated_at | TIMESTAMP | NO | — | |

#### hospital_inventory_movements
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| hospital_id | VARCHAR(36) | NO | — | FK → hospitals(id) |
| inventory_item_id | VARCHAR(36) | NO | — | FK → hospital_inventory_items(id) |
| movement_type | VARCHAR(32) | NO | — | |
| quantity_delta | INT | NO | 0 | |
| unit | VARCHAR(32) | YES | NULL | |
| notes | TEXT | YES | NULL | |
| related_supply_request_id | VARCHAR(36) | YES | NULL | FK → supply_requests(id) |
| created_at | TIMESTAMP | NO | — | |

#### operational_recommendations
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| hospital_id | VARCHAR(36) | NO | — | FK → hospitals(id) |
| source_alert_id | VARCHAR(36) | YES | NULL | FK → alerts(id) |
| source_outbreak_id | VARCHAR(36) | YES | NULL | FK → outbreaks(id) |
| type | VARCHAR(32) | NO | — | |
| severity | VARCHAR(16) | NO | 'MEDIUM' | |
| status | VARCHAR(32) | NO | 'NEW' | |
| category | VARCHAR(64) | YES | NULL | |
| title | VARCHAR(255) | NO | — | |
| description | TEXT | YES | NULL | |
| expected_impact | VARCHAR(512) | YES | NULL | |
| urgency_window | VARCHAR(128) | YES | NULL | |
| confidence_score | DECIMAL(5,2) | YES | NULL | |
| image_mode | VARCHAR(32) | YES | NULL | |
| rationale_json | TEXT | YES | NULL | |
| recommended_actions_json | TEXT | YES | NULL | |
| affected_departments_json | TEXT | YES | NULL | |
| affected_resources_json | TEXT | YES | NULL | |
| primary_department_resource_id | VARCHAR(36) | YES | NULL | FK → hospital_department_resources(id) |
| primary_staffing_profile_id | VARCHAR(36) | YES | NULL | FK → hospital_staffing_profiles(id) |
| primary_inventory_item_id | VARCHAR(36) | YES | NULL | FK → hospital_inventory_items(id) |
| presentation_variant | VARCHAR(32) | YES | NULL | |
| primary_action_code | VARCHAR(32) | YES | NULL | |
| available_actions_json | TEXT | YES | NULL | |
| allowed_status_transitions_json | TEXT | YES | NULL | |
| display_category_label | VARCHAR(64) | YES | NULL | |
| display_severity_label | VARCHAR(32) | YES | NULL | |
| display_status_label | VARCHAR(32) | YES | NULL | |
| expires_at | TIMESTAMP | YES | NULL | |
| assigned_owner_user_id | VARCHAR(36) | YES | NULL | FK → users(id) |
| model_provider | VARCHAR(64) | YES | NULL | |
| model_version | VARCHAR(64) | YES | NULL | |
| input_context_json | TEXT | YES | NULL | |
| created_by_mode | VARCHAR(32) | NO | 'RULE_ENGINE' | |
| created_at | TIMESTAMP | NO | — | |
| updated_at | TIMESTAMP | NO | — | |
| resolved_at | TIMESTAMP | YES | NULL | |

#### operational_recommendation_audit
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| recommendation_id | VARCHAR(36) | NO | — | FK → operational_recommendations(id) |
| actor_user_id | VARCHAR(36) | YES | NULL | FK → users(id) |
| event_type | VARCHAR(32) | NO | — | |
| event_label | VARCHAR(255) | YES | NULL | |
| event_payload_json | TEXT | YES | NULL | |
| created_at | TIMESTAMP | NO | — | |

#### operational_tasks
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| recommendation_id | VARCHAR(36) | NO | — | FK → operational_recommendations(id) |
| hospital_id | VARCHAR(36) | NO | — | FK → hospitals(id) |
| owner_user_id | VARCHAR(36) | YES | NULL | FK → users(id) |
| owner_label | VARCHAR(255) | YES | NULL | |
| department_label | VARCHAR(255) | YES | NULL | |
| deadline_at | TIMESTAMP | YES | NULL | |
| priority | VARCHAR(16) | NO | 'MEDIUM' | |
| notes | TEXT | YES | NULL | |
| status | VARCHAR(32) | NO | 'PENDING' | |
| created_by_user_id | VARCHAR(36) | YES | NULL | |
| created_at | TIMESTAMP | NO | — | |
| updated_at | TIMESTAMP | NO | — | |

#### operational_notifications
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| recommendation_id | VARCHAR(36) | NO | — | FK → operational_recommendations(id) |
| hospital_id | VARCHAR(36) | NO | — | FK → hospitals(id) |
| audience_label | VARCHAR(255) | YES | NULL | |
| message | TEXT | YES | NULL | |
| status | VARCHAR(32) | NO | 'SENT' | |
| sent_by_user_id | VARCHAR(36) | YES | NULL | |
| sent_at | TIMESTAMP | NO | — | |

#### supply_requests
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| recommendation_id | VARCHAR(36) | NO | — | FK → operational_recommendations(id) |
| hospital_id | VARCHAR(36) | NO | — | FK → hospitals(id) |
| inventory_item_id | VARCHAR(36) | YES | NULL | FK → hospital_inventory_items(id) |
| supply_type_label | VARCHAR(255) | YES | NULL | |
| quantity | INT | NO | 0 | |
| unit | VARCHAR(32) | YES | NULL | |
| destination | VARCHAR(255) | YES | NULL | |
| suggested_supplier | VARCHAR(255) | YES | NULL | |
| status | VARCHAR(32) | NO | 'REQUESTED' | |
| requested_by_user_id | VARCHAR(36) | YES | NULL | |
| created_at | TIMESTAMP | NO | — | |
| updated_at | TIMESTAMP | NO | — | |

#### hospital_operational_contacts
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| hospital_id | VARCHAR(36) | NO | — | FK → hospitals(id) |
| user_id | VARCHAR(36) | YES | NULL | FK → users(id) |
| display_name | VARCHAR(255) | NO | — | |
| role_label | VARCHAR(128) | NO | — | |
| department_code | VARCHAR(32) | YES | NULL | |
| contact_channel | VARCHAR(32) | YES | NULL | |
| contact_value | VARCHAR(255) | YES | NULL | |
| availability_status | VARCHAR(32) | YES | NULL | |
| is_assignable | BOOLEAN | NO | FALSE | |
| is_notifiable | BOOLEAN | NO | FALSE | |
| updated_at | TIMESTAMP | NO | — | |

#### hospital_operational_groups
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| hospital_id | VARCHAR(36) | NO | — | FK → hospitals(id) |
| group_code | VARCHAR(32) | NO | — | |
| group_name | VARCHAR(128) | NO | — | |
| group_type | VARCHAR(32) | NO | — | |
| department_code | VARCHAR(32) | YES | NULL | |
| is_assignable | BOOLEAN | NO | FALSE | |
| is_notifiable | BOOLEAN | NO | FALSE | |
| updated_at | TIMESTAMP | NO | — | |

#### hospital_operational_group_members
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | VARCHAR(36) | NO | — | PK |
| group_id | VARCHAR(36) | NO | — | FK → hospital_operational_groups(id) |
| contact_id | VARCHAR(36) | NO | — | FK → hospital_operational_contacts(id) |
| created_at | TIMESTAMP | NO | — | |

---

## 2. Índices compuestos implementados

| Índice | Tabla | Columnas | Justificación |
|--------|-------|----------|---------------|
| idx_outbreaks_status_scope_municipality | outbreaks | (status, scope, municipality_id) | Filtro por brotes activos municipales |
| idx_outbreaks_status_scope_state | outbreaks | (status, scope, state_id) | Filtro por brotes activos estatales |
| idx_recs_hospital_created | operational_recommendations | (hospital_id, created_at DESC) | Feed de recomendaciones por hospital |
| idx_recs_hospital_status_created | operational_recommendations | (hospital_id, status, created_at DESC) | Feed filtrado por status |
| idx_recs_hospital_severity_created | operational_recommendations | (hospital_id, severity, created_at DESC) | Feed filtrado por severidad |
| idx_snapshots_hospital_captured | hospital_resource_snapshots | (hospital_id, captured_at DESC) | Último snapshot por hospital |
| idx_dept_hospital_status | hospital_department_resources | (hospital_id, status) | Departamentos por hospital y estado |
| idx_inventory_hospital_category | hospital_inventory_items | (hospital_id, category, item_name) | Inventario por hospital y categoría |
| idx_inventory_hospital_status | hospital_inventory_items | (hospital_id, status) | Items críticos por hospital |
| idx_staff_hospital_role | hospital_staffing_profiles | (hospital_id, role_name) | Personal por hospital y rol |
| idx_patients_hospital_name | patients | (hospital_id, full_name) | Búsqueda de pacientes |
| idx_audit_recommendation_created | operational_recommendation_audit | (recommendation_id, created_at ASC) | Auditoría cronológica |
| idx_movements_item_created | hospital_inventory_movements | (inventory_item_id, created_at DESC) | Historial de movimientos por item |

---

## 3. Tabla generada por Event Scheduler

### outbreak_daily_kpis
| Columna | Tipo | Nulable | Default | Restricción |
|---------|------|---------|---------|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | PK |
| snapshot_date | DATE | NO | — | |
| scope | VARCHAR(16) | NO | — | |
| state_id | VARCHAR(36) | YES | NULL | |
| state_name | VARCHAR(64) | YES | NULL | |
| municipality_id | VARCHAR(36) | YES | NULL | |
| municipality_name | VARCHAR(128) | YES | NULL | |
| total_cases | INT | NO | 0 | |
| active_outbreaks | INT | NO | 0 | |
| suspected | INT | NO | 0 | |
| confirmed | INT | NO | 0 | |
| top_disease | VARCHAR(128) | YES | NULL | |
| calculated_at | TIMESTAMP | NO | CURRENT_TIMESTAMP | |

UNIQUE: (snapshot_date, scope, state_id, municipality_id)

---

## 4. Elementos programados agregados por V8

### Stored Procedures

| Nombre | Propósito |
|--------|-----------|
| sp_create_supply_request_with_movement | Crear solicitud de suministro y movimiento de inventario en una transacción |
| sp_generate_hospital_operational_summary | Generar resumen operativo consolidado por hospital |

### Funciones almacenadas

| Nombre | Propósito | Retorna |
|--------|-----------|---------|
| fn_bed_occupancy_pct | Calcular porcentaje de ocupación de camas de un hospital | DECIMAL(5,2) |
| fn_inventory_status | Determinar estado de un item de inventario según cantidades | VARCHAR(32) |

### Triggers

| Nombre | Tipo | Tabla | Propósito |
|--------|------|-------|-----------|
| trg_validate_inventory_before_insert | BEFORE INSERT | hospital_inventory_movements | Validar que el movimiento no deje cantidad negativa |
| trg_validate_inventory_before_update | BEFORE UPDATE | hospital_inventory_items | Recalcular status según current_quantity vs critical_threshold |
| trg_audit_recommendation_change | AFTER UPDATE | operational_recommendations | Registrar cambios de status en auditoría |

Script: `src/main/resources/db/migration/V8__advanced_database_module.sql`
Scripts individuales: `scripts/mysql/stored_procedures/`, `scripts/mysql/functions/`, `scripts/mysql/triggers/`