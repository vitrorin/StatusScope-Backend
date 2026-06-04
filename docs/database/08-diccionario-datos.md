# Diccionario de Datos — StatusScope

## Tablas del sistema

---

### states

**Propósito:** Catálogo de estados (entidades federativas) de México, utilizado como referencia geográfica para brotes estatales y municipales.

**Relaciones:** Un estado tiene N municipios (1:N). Un estado tiene N brotes estatales (1:N).

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | Identificador UUID |
| code | VARCHAR(32) | NO | — | — | — | UNIQUE; código INEGI (ej. NL) |
| name | VARCHAR(128) | NO | — | — | — | Nombre del estado |
| description | VARCHAR(255) | YES | NULL | — | — | |
| created_at | DATETIME(6) | NO | — | — | — | |
| updated_at | DATETIME(6) | NO | — | — | — | |

---

### municipalities

**Propósito:** Catálogo de municipios de México, referencia geográfica para brotes municipales y ubicación de hospitales.

**Relaciones:** Un municipio pertenece a 1 estado (N:1). Un municipio tiene N brotes (1:N). Un municipio tiene N hospitales (1:N).

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | Identificador UUID |
| code | VARCHAR(32) | NO | — | — | — | UNIQUE; código INEGI |
| name | VARCHAR(128) | NO | — | — | — | |
| state_id | VARCHAR(36) | NO | — | — | states(id) | |
| latitude | DECIMAL(10,7) | YES | NULL | — | — | |
| longitude | DECIMAL(10,7) | YES | NULL | — | — | |
| created_at | DATETIME(6) | NO | — | — | — | |
| updated_at | DATETIME(6) | NO | — | — | — | |

---

### specialties

**Propósito:** Catálogo de especialidades médicas, referencia para enfermedades y doctores.

**Relaciones:** Una especialidad cubre N enfermedades (1:N). Una especialidad pertenece a N doctores (1:N).

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| code | VARCHAR(32) | NO | — | — | — | UNIQUE |
| name | VARCHAR(128) | NO | — | — | — | |
| description | VARCHAR(255) | YES | NULL | — | — | |
| created_at | DATETIME(6) | NO | — | — | — | |
| updated_at | DATETIME(6) | NO | — | — | — | |

---

### diseases

**Propósito:** Catálogo de enfermedades rastreadas, referencia para brotes y diagnóstico asistido.

**Relaciones:** Una enfermedad tiene 1 especialidad primaria (N:1). Una enfermedad tiene N síntomas (N:M vía disease_symptoms). Una enfermedad tiene N brotes (1:N).

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| code | VARCHAR(32) | NO | — | — | — | UNIQUE |
| name | VARCHAR(128) | NO | — | — | — | |
| specialty_id | VARCHAR(36) | NO | — | — | specialties(id) | |
| created_at | DATETIME(6) | NO | — | — | — | |
| updated_at | DATETIME(6) | NO | — | — | — | |

---

### symptoms

**Propósito:** Catálogo de síntomas, referencia para el diagnóstico asistido.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| code | VARCHAR(32) | NO | — | — | — | UNIQUE |
| name | VARCHAR(255) | NO | — | — | — | UNIQUE |
| created_at | DATETIME(6) | NO | — | — | — | |
| updated_at | DATETIME(6) | NO | — | — | — | |

---

### disease_symptoms

**Propósito:** Asociación N:M entre enfermedades y síntomas.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| disease_id | VARCHAR(36) | NO | — | SI (parcial) | diseases(id) | |
| symptom_id | VARCHAR(36) | NO | — | SI (parcial) | symptoms(id) | |

Índice: `idx_disease_symptoms_disease (disease_id)`, `idx_disease_symptoms_symptom (symptom_id)`.

---

### disease_specialties

**Propósito:** Asociación N:M entre enfermedades y especialidades adicionales a la primaria.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| disease_id | VARCHAR(36) | NO | — | SI (parcial) | diseases(id) | |
| specialty_id | VARCHAR(36) | NO | — | SI (parcial) | specialties(id) | |

---

### roles

**Propósito:** Roles del sistema RBAC (SYSTEM_ADMIN, HOSPITAL_ADMIN, DOCTOR).

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| code | VARCHAR(64) | NO | — | — | — | UNIQUE |
| name | VARCHAR(128) | NO | — | — | — | |
| description | VARCHAR(255) | YES | NULL | — | — | |
| created_at | DATETIME(6) | NO | — | — | — | |
| updated_at | DATETIME(6) | NO | — | — | — | |

---

### privileges

**Propósito:** Permisos granulares del sistema RBAC.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| code | VARCHAR(128) | NO | — | — | — | UNIQUE |
| description | VARCHAR(255) | YES | NULL | — | — | |
| created_at | DATETIME(6) | NO | — | — | — | |
| updated_at | DATETIME(6) | NO | — | — | — | |

---

### role_privileges

**Propósito:** Asociación N:M entre roles y privilegios.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| role_id | VARCHAR(36) | NO | — | SI (parcial) | roles(id) | |
| privilege_id | VARCHAR(36) | NO | — | SI (parcial) | privileges(id) | |

---

### hospitals

**Propósito:** Hospitales registrados en el sistema. Entidad central del módulo operativo.

**Relaciones:** Un hospital tiene N snapshots, departamentos, perfiles de personal, items de inventario, recomendaciones, pacientes y contactos (1:N).

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| code | VARCHAR(32) | NO | — | — | — | UNIQUE |
| name | VARCHAR(255) | NO | — | — | — | |
| address | VARCHAR(512) | YES | NULL | — | — | |
| phone | VARCHAR(32) | YES | NULL | — | — | |
| invite_code | VARCHAR(64) | YES | NULL | — | — | UNIQUE |
| active | BIT(1) | NO | — | — | — | |
| postal_code | VARCHAR(16) | YES | NULL | — | — | |
| bed_count | INT | YES | NULL | — | — | |
| doctor_count | INT | YES | NULL | — | — | |
| nurse_count | INT | YES | NULL | — | — | |
| municipality_id | VARCHAR(36) | YES | NULL | — | municipalities(id) | |
| latitude | DECIMAL(10,7) | YES | NULL | — | — | |
| longitude | DECIMAL(10,7) | YES | NULL | — | — | |
| created_at | DATETIME(6) | NO | — | — | — | |
| updated_at | DATETIME(6) | NO | — | — | — | |

---

### users

**Propósito:** Usuarios del sistema con identidad Firebase y roles RBAC.

**Relaciones:** Un usuario pertenece a 1 hospital (N:1). Un usuario tiene N roles (N:M vía user_roles).

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| full_name | VARCHAR(255) | NO | — | — | — | |
| email | VARCHAR(255) | NO | — | — | — | UNIQUE |
| active | BIT(1) | NO | — | — | — | |
| external_auth_id | VARCHAR(128) | NO | — | — | — | UNIQUE; Firebase UID |
| hospital_id | VARCHAR(36) | YES | NULL | — | hospitals(id) | |
| status | ENUM('ACTIVE','DISABLED','PENDING') | NO | — | — | — | |
| last_login_at | DATETIME(6) | YES | NULL | — | — | |
| license_number | VARCHAR(64) | YES | NULL | — | — | |
| specialty_id | VARCHAR(36) | YES | NULL | — | specialties(id) | |
| created_at | DATETIME(6) | NO | — | — | — | |
| updated_at | DATETIME(6) | NO | — | — | — | |

---

### user_roles

**Propósito:** Asociación N:M entre usuarios y roles.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| user_id | VARCHAR(36) | NO | — | SI (parcial) | users(id) | |
| role_id | VARCHAR(36) | NO | — | SI (parcial) | roles(id) | |

---

### outbreaks

**Propósito:** Brotes epidemiológicos rastreados por el sistema, fuente principal del dashboard.

**Relaciones:** Un brote pertenece a 1 enfermedad (N:1). Un brote se ubica en 1 municipio o 1 estado. Un brote genera N alertas (1:N).

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| disease_id | VARCHAR(36) | NO | — | — | diseases(id) | |
| scope | VARCHAR(16) | NO | — | — | — | MUNICIPALITY o STATE |
| municipality_id | VARCHAR(36) | YES | NULL | — | municipalities(id) | Obligatorio si scope=MUNICIPALITY |
| state_id | VARCHAR(36) | YES | NULL | — | states(id) | Obligatorio si scope=STATE |
| case_count | INT | NO | — | — | — | |
| confirmation_status | VARCHAR(16) | NO | — | — | — | SUSPECTED o CONFIRMED |
| status | VARCHAR(16) | NO | — | — | — | ACTIVE o RESOLVED |
| started_at | DATETIME(6) | NO | — | — | — | |
| ended_at | DATETIME(6) | YES | NULL | — | — | |
| created_at | DATETIME(6) | NO | — | — | — | |
| updated_at | DATETIME(6) | NO | — | — | — | |

**Índices adicionales:** idx_outbreaks_status_scope_municipality, idx_outbreaks_status_scope_state.

---

### alerts

**Propósito:** Alertas generadas por brotes epidemiológicos.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| outbreak_id | VARCHAR(36) | NO | — | — | outbreaks(id) | |
| severity | VARCHAR(16) | NO | — | — | — | |
| message | VARCHAR(512) | YES | NULL | — | — | |
| acknowledged_at | DATETIME(6) | YES | NULL | — | — | |
| created_at | DATETIME(6) | NO | — | — | — | |

---

### hospital_resource_snapshots

**Propósito:** Instantáneas de recursos hospitalarios (camas, personal, oxígeno) tomadas periódicamente.

**Relaciones:** Un hospital tiene N snapshots (1:N).

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| hospital_id | VARCHAR(36) | NO | — | — | hospitals(id) | |
| captured_at | TIMESTAMP | NO | — | — | — | |
| total_beds | INT | NO | 0 | — | — | |
| available_beds | INT | NO | 0 | — | — | |
| icu_total_beds | INT | NO | 0 | — | — | |
| icu_available_beds | INT | NO | 0 | — | — | |
| isolation_rooms_total | INT | NO | 0 | — | — | |
| isolation_rooms_available | INT | NO | 0 | — | — | |
| oxygen_capacity_units | INT | NO | 0 | — | — | |
| oxygen_available_units | INT | NO | 0 | — | — | |
| doctors_on_shift | INT | NO | 0 | — | — | |
| nurses_on_shift | INT | NO | 0 | — | — | |
| specialists_on_shift | INT | NO | 0 | — | — | |
| source | VARCHAR(16) | NO | 'MANUAL' | — | — | |
| created_at | TIMESTAMP | NO | — | — | — | |

**Índice:** idx_snapshots_hospital_captured (hospital_id, captured_at DESC).

---

### hospital_department_resources

**Propósito:** Estado de capacidad por departamento hospitalario.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| hospital_id | VARCHAR(36) | NO | — | — | hospitals(id) | |
| department_code | VARCHAR(32) | NO | — | — | — | |
| department_name | VARCHAR(128) | NO | — | — | — | |
| level_label | VARCHAR(64) | YES | NULL | — | — | |
| total_beds | INT | NO | 0 | — | — | |
| occupied_beds | INT | NO | 0 | — | — | |
| status | VARCHAR(32) | NO | 'NORMAL' | — | — | NORMAL, HIGH_LOAD, CRITICAL |
| notes | VARCHAR(512) | YES | NULL | — | — | |
| updated_at | TIMESTAMP | NO | — | — | — | |

**Índice:** idx_dept_hospital_status (hospital_id, status).

---

### hospital_staffing_profiles

**Propósito:** Plantilla de personal por hospital y rol.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| hospital_id | VARCHAR(36) | NO | — | — | hospitals(id) | |
| role_code | VARCHAR(32) | NO | — | — | — | |
| role_name | VARCHAR(128) | NO | — | — | — | |
| headcount | INT | NO | 0 | — | — | |
| on_shift_count | INT | NO | 0 | — | — | |
| on_call_count | INT | NO | 0 | — | — | |
| standby_count | INT | NO | 0 | — | — | |
| updated_at | TIMESTAMP | NO | — | — | — | |

**Índice:** idx_staff_hospital_role (hospital_id, role_name).

---

### hospital_inventory_items

**Propósito:** Items de inventario hospitalario con cantidades y umbrales críticos.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| hospital_id | VARCHAR(36) | NO | — | — | hospitals(id) | |
| item_code | VARCHAR(32) | NO | — | — | — | |
| item_name | VARCHAR(128) | NO | — | — | — | |
| category | VARCHAR(64) | YES | NULL | — | — | PPE, EQUIPMENT, SUPPLY, PHARMACEUTICAL |
| location | VARCHAR(128) | YES | NULL | — | — | |
| current_quantity | INT | NO | 0 | — | — | |
| capacity_quantity | INT | NO | 0 | — | — | |
| unit | VARCHAR(32) | YES | NULL | — | — | |
| critical_threshold | INT | NO | 0 | — | — | Umbral para estado CRITICAL |
| target_quantity | INT | NO | 0 | — | — | Cantidad objetivo |
| status | VARCHAR(32) | NO | 'ADEQUATE' | — | — | ADEQUATE, LOW, CRITICAL |
| updated_at | TIMESTAMP | NO | — | — | — | |

**Índices:** idx_inventory_hospital_category (hospital_id, category, item_name), idx_inventory_hospital_status (hospital_id, status).

---

### hospital_inventory_movements

**Propósito:** Historial de movimientos de inventario (consumo, reabastecimiento, ajuste).

**Relaciones:** Un item tiene N movimientos (1:N). Un movimiento puede estar vinculado a 1 solicitud (0:1).

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| hospital_id | VARCHAR(36) | NO | — | — | hospitals(id) | |
| inventory_item_id | VARCHAR(36) | NO | — | — | hospital_inventory_items(id) | |
| movement_type | VARCHAR(32) | NO | — | — | — | CONSUMPTION, REPLENISHMENT, MANUAL_ADJUSTMENT |
| quantity_delta | INT | NO | 0 | — | — | Negativo para consumo |
| unit | VARCHAR(32) | YES | NULL | — | — | |
| notes | TEXT | YES | NULL | — | — | |
| related_supply_request_id | VARCHAR(36) | YES | NULL | — | supply_requests(id) | |
| created_at | TIMESTAMP | NO | — | — | — | |

**Índice:** idx_movements_item_created (inventory_item_id, created_at DESC).

---

### operational_recommendations

**Propósito:** Recomendaciones operativas generadas por el motor de reglas o IA para cada hospital.

**Relaciones:** Un hospital tiene N recomendaciones (1:N). Una recomendación puede referenciar 1 alerta, 1 brote, 1 departamento, 1 perfil de personal, 1 item de inventario.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| hospital_id | VARCHAR(36) | NO | — | — | hospitals(id) | |
| source_alert_id | VARCHAR(36) | YES | NULL | — | alerts(id) | |
| source_outbreak_id | VARCHAR(36) | YES | NULL | — | outbreaks(id) | |
| type | VARCHAR(32) | NO | — | — | — | BED_CAPACITY, STAFFING, ISOLATION |
| severity | VARCHAR(16) | NO | 'MEDIUM' | — | — | CRITICAL, HIGH, MEDIUM, LOW |
| status | VARCHAR(32) | NO | 'NEW' | — | — | NEW, ACCEPTED, ASSIGNED, COMPLETED, REJECTED |
| category | VARCHAR(64) | YES | NULL | — | — | |
| title | VARCHAR(255) | NO | — | — | — | |
| description | TEXT | YES | NULL | — | — | |
| expected_impact | VARCHAR(512) | YES | NULL | — | — | |
| urgency_window | VARCHAR(128) | YES | NULL | — | — | |
| confidence_score | DECIMAL(5,2) | YES | NULL | — | — | 0.00 a 1.00 |
| image_mode | VARCHAR(32) | YES | NULL | — | — | |
| rationale_json | TEXT | YES | NULL | — | — | |
| recommended_actions_json | TEXT | YES | NULL | — | — | |
| affected_departments_json | TEXT | YES | NULL | — | — | |
| affected_resources_json | TEXT | YES | NULL | — | — | |
| primary_department_resource_id | VARCHAR(36) | YES | NULL | — | hospital_department_resources(id) | |
| primary_staffing_profile_id | VARCHAR(36) | YES | NULL | — | hospital_staffing_profiles(id) | |
| primary_inventory_item_id | VARCHAR(36) | YES | NULL | — | hospital_inventory_items(id) | |
| presentation_variant | VARCHAR(32) | YES | NULL | — | — | alert, urgent, standard |
| primary_action_code | VARCHAR(32) | YES | NULL | — | — | ASSIGN_TASK, NOTIFY_STAFF, ORDER_SUPPLIES |
| available_actions_json | TEXT | YES | NULL | — | — | |
| allowed_status_transitions_json | TEXT | YES | NULL | — | — | |
| display_category_label | VARCHAR(64) | YES | NULL | — | — | Desnormalizado |
| display_severity_label | VARCHAR(32) | YES | NULL | — | — | Desnormalizado |
| display_status_label | VARCHAR(32) | YES | NULL | — | — | Desnormalizado |
| expires_at | TIMESTAMP | YES | NULL | — | — | |
| assigned_owner_user_id | VARCHAR(36) | YES | NULL | — | users(id) | |
| model_provider | VARCHAR(64) | YES | NULL | — | — | |
| model_version | VARCHAR(64) | YES | NULL | — | — | |
| input_context_json | TEXT | YES | NULL | — | — | |
| created_by_mode | VARCHAR(32) | NO | 'RULE_ENGINE' | — | — | RULE_ENGINE, AI |
| created_at | TIMESTAMP | NO | — | — | — | |
| updated_at | TIMESTAMP | NO | — | — | — | |
| resolved_at | TIMESTAMP | YES | NULL | — | — | |

**Índices:** idx_recs_hospital_created, idx_recs_hospital_status_created, idx_recs_hospital_severity_created.

---

### operational_recommendation_audit

**Propósito:** Registro de auditoría de cambios en recomendaciones operativas.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| recommendation_id | VARCHAR(36) | NO | — | — | operational_recommendations(id) | |
| actor_user_id | VARCHAR(36) | YES | NULL | — | users(id) | |
| event_type | VARCHAR(32) | NO | — | — | — | STATUS_CHANGE, CREATED, etc. |
| event_label | VARCHAR(255) | YES | NULL | — | — | |
| event_payload_json | TEXT | YES | NULL | — | — | |
| created_at | TIMESTAMP | NO | — | — | — | |

**Índice:** idx_audit_recommendation_created (recommendation_id, created_at ASC).

---

### operational_tasks

**Propósito:** Tareas derivadas de recomendaciones operativas.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| recommendation_id | VARCHAR(36) | NO | — | — | operational_recommendations(id) | |
| hospital_id | VARCHAR(36) | NO | — | — | hospitals(id) | |
| owner_user_id | VARCHAR(36) | YES | NULL | — | users(id) | |
| owner_label | VARCHAR(255) | YES | NULL | — | — | |
| department_label | VARCHAR(255) | YES | NULL | — | — | |
| deadline_at | TIMESTAMP | YES | NULL | — | — | |
| priority | VARCHAR(16) | NO | 'MEDIUM' | — | — | |
| notes | TEXT | YES | NULL | — | — | |
| status | VARCHAR(32) | NO | 'PENDING' | — | — | |
| created_by_user_id | VARCHAR(36) | YES | NULL | — | — | |
| created_at | TIMESTAMP | NO | — | — | — | |
| updated_at | TIMESTAMP | NO | — | — | — | |

---

### operational_notifications

**Propósito:** Notificaciones enviadas por recomendaciones operativas.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| recommendation_id | VARCHAR(36) | NO | — | — | operational_recommendations(id) | |
| hospital_id | VARCHAR(36) | NO | — | — | hospitals(id) | |
| audience_label | VARCHAR(255) | YES | NULL | — | — | |
| message | TEXT | YES | NULL | — | — | |
| status | VARCHAR(32) | NO | 'SENT' | — | — | |
| sent_by_user_id | VARCHAR(36) | YES | NULL | — | — | |
| sent_at | TIMESTAMP | NO | — | — | — | |

---

### supply_requests

**Propósito:** Solicitudes de suministro generadas a partir de recomendaciones.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| recommendation_id | VARCHAR(36) | NO | — | — | operational_recommendations(id) | |
| hospital_id | VARCHAR(36) | NO | — | — | hospitals(id) | |
| inventory_item_id | VARCHAR(36) | YES | NULL | — | hospital_inventory_items(id) | |
| supply_type_label | VARCHAR(255) | YES | NULL | — | — | |
| quantity | INT | NO | 0 | — | — | |
| unit | VARCHAR(32) | YES | NULL | — | — | |
| destination | VARCHAR(255) | YES | NULL | — | — | |
| suggested_supplier | VARCHAR(255) | YES | NULL | — | — | |
| status | VARCHAR(32) | NO | 'REQUESTED' | — | — | REQUESTED, FULFILLED, CANCELLED |
| requested_by_user_id | VARCHAR(36) | YES | NULL | — | — | |
| created_at | TIMESTAMP | NO | — | — | — | |
| updated_at | TIMESTAMP | NO | — | — | — | |

---

### hospital_operational_contacts

**Propósito:** Contactos operativos de cada hospital para asignación y notificación.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| hospital_id | VARCHAR(36) | NO | — | — | hospitals(id) | |
| user_id | VARCHAR(36) | YES | NULL | — | users(id) | |
| display_name | VARCHAR(255) | NO | — | — | — | |
| role_label | VARCHAR(128) | NO | — | — | — | |
| department_code | VARCHAR(32) | YES | NULL | — | — | |
| contact_channel | VARCHAR(32) | YES | NULL | — | — | PHONE, EMAIL |
| contact_value | VARCHAR(255) | YES | NULL | — | — | |
| availability_status | VARCHAR(32) | YES | NULL | — | — | ON_SHIFT, ON_CALL, AVAILABLE |
| is_assignable | BOOLEAN | NO | FALSE | — | — | |
| is_notifiable | BOOLEAN | NO | FALSE | — | — | |
| updated_at | TIMESTAMP | NO | — | — | — | |

---

### hospital_operational_groups

**Propósito:** Grupos de respuesta operativa por hospital.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| hospital_id | VARCHAR(36) | NO | — | — | hospitals(id) | |
| group_code | VARCHAR(32) | NO | — | — | — | |
| group_name | VARCHAR(128) | NO | — | — | — | |
| group_type | VARCHAR(32) | NO | — | — | — | INCIDENT_RESPONSE, DEPARTMENT, SUPPLY_CHAIN |
| department_code | VARCHAR(32) | YES | NULL | — | — | |
| is_assignable | BOOLEAN | NO | FALSE | — | — | |
| is_notifiable | BOOLEAN | NO | FALSE | — | — | |
| updated_at | TIMESTAMP | NO | — | — | — | |

---

### hospital_operational_group_members

**Propósito:** Asociación N:M entre grupos operativos y contactos.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | VARCHAR(36) | NO | — | SI | — | |
| group_id | VARCHAR(36) | NO | — | — | hospital_operational_groups(id) | |
| contact_id | VARCHAR(36) | NO | — | — | hospital_operational_contacts(id) | |
| created_at | TIMESTAMP | NO | — | — | — | |

---

### outbreak_daily_kpis

**Propósito:** Snapshots diarios de KPIs epidemiológicos, generados por Event Scheduler. Tabla materializada.

| Columna | Tipo | Nulable | Default | PK | FK | Restricción |
|---------|------|---------|---------|----|----|-------------|
| id | BIGINT | NO | AUTO_INCREMENT | SI | — | |
| snapshot_date | DATE | NO | — | — | — | |
| scope | VARCHAR(16) | NO | — | — | — | STATE o MUNICIPALITY |
| state_id | VARCHAR(36) | YES | NULL | — | — | |
| state_name | VARCHAR(64) | YES | NULL | — | — | |
| municipality_id | VARCHAR(36) | YES | NULL | — | — | |
| municipality_name | VARCHAR(128) | YES | NULL | — | — | |
| total_cases | INT | NO | 0 | — | — | |
| active_outbreaks | INT | NO | 0 | — | — | |
| suspected | INT | NO | 0 | — | — | |
| confirmed | INT | NO | 0 | — | — | |
| top_disease | VARCHAR(128) | YES | NULL | — | — | |
| calculated_at | TIMESTAMP | NO | CURRENT_TIMESTAMP | — | — | |

**UNIQUE:** (snapshot_date, scope, state_id, municipality_id).
**Índices:** idx_kpi_scope_state (scope, state_id), idx_kpi_scope_municipality (scope, municipality_id), idx_kpi_calculated (calculated_at).

---

## Elementos programados

### Stored Procedures

| Nombre | Objetivo | Script |
|--------|----------|--------|
| sp_create_supply_request_with_movement | Crear solicitud de suministro y movimiento de inventario en una transacción atómica | scripts/mysql/stored_procedures/001_sp_create_supply_request_with_movement.sql |
| sp_generate_hospital_operational_summary | Generar resumen operativo consolidado por hospital | scripts/mysql/stored_procedures/002_sp_generate_hospital_operational_summary.sql |

### Funciones almacenadas

| Nombre | Propósito | Retorna | Script |
|--------|-----------|---------|--------|
| fn_bed_occupancy_pct | Calcular porcentaje de ocupación de camas de un hospital | DECIMAL(5,2) | scripts/mysql/functions/001_fn_bed_occupancy_pct.sql |
| fn_inventory_status | Determinar estado de un item de inventario según cantidades | VARCHAR(32) | scripts/mysql/functions/002_fn_inventory_status.sql |

### Triggers

| Nombre | Tipo | Tabla | Propósito | Script |
|--------|------|-------|-----------|--------|
| trg_validate_inventory_before_insert | BEFORE INSERT | hospital_inventory_movements | Validar que el movimiento no deje cantidad negativa | scripts/mysql/triggers/001_trg_validate_inventory_before_insert.sql |
| trg_validate_inventory_before_update | BEFORE UPDATE | hospital_inventory_items | Recalcular status según current_quantity vs critical_threshold | scripts/mysql/triggers/002_trg_validate_inventory_before_update.sql |
| trg_audit_recommendation_change | AFTER UPDATE | operational_recommendations | Registrar cambios de status en auditoría | scripts/mysql/triggers/003_trg_audit_recommendation_change.sql |

### Event Schedulers

| Nombre | Objetivo | Frecuencia | Script |
|--------|----------|------------|--------|
| ev_purge_audit_logs | Depurar registros >90 días | Diario 03:00 | scripts/mysql/events/001_ev_purge_audit_logs.sql |
| ev_snapshot_daily_kpis | Materializar KPIs diarios de brotes | Diario 05:00 | scripts/mysql/events/002_ev_snapshot_daily_kpis.sql |
| ev_rotate_snapshots | Rotar snapshots >30 días | Semanal domingo 04:00 | scripts/mysql/events/003_ev_rotate_snapshots.sql |