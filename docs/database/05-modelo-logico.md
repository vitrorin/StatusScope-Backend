# Modelo Lógico — StatusScope

## 1. Entidades y atributos

### Área de Seguridad

#### users
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | `30000000-0000-0000-0000-000000000001` |
| full_name | String | `Dr. Ana Lopez` |
| email | String (único) | `ana@hospital.mx` |
| external_auth_id | String (único) | `firebase-uid-abc123` |
| active | Boolean | `true` |
| status | Enum | `ACTIVE`, `DISABLED`, `PENDING` |
| hospital_id | FK → hospitals | `30000000-...000000000001` |
| specialty_id | FK → specialties | `50000000-...000000000003` |
| license_number | String | `12345` |
| last_login_at | Timestamp | `2026-05-20 14:30:00` |

#### roles
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | `00000000-...000000000001` |
| code | String (único) | `SYSTEM_ADMIN` |
| name | String | `System Administrator` |
| description | String | `Platform-wide administrative access` |

#### privileges
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | `10000000-...000000000001` |
| code | String (único) | `alerts.read` |
| description | String | `Read alerts` |

#### user_roles (asociativa)
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| user_id | FK → users | UUID |
| role_id | FK → roles | UUID |

Cardinalidad: un usuario tiene N roles; un rol pertenece a N usuarios (**N:M**).

#### role_privileges (asociativa)
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| role_id | FK → roles | UUID |
| privilege_id | FK → privileges | UUID |

Cardinalidad: un rol tiene N privilegios; un privilegio pertenece a N roles (**N:M**).

---

### Área de Geografía

#### states
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | `40000000-...000000000019` |
| code | String (único) | `NL` |
| name | String | `Nuevo Leon` |
| description | String | `Estado de Nuevo Leon` |

#### municipalities
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | `42000000-...000000001003` |
| code | String (único) | `038` |
| name | String | `Monterrey` |
| state_id | FK → states | UUID |
| latitude | Decimal | `25.6866142` |
| longitude | Decimal | `-100.3161126` |

Cardinalidad: un estado tiene N municipios; un municipio pertenece a 1 estado (**1:N**).

#### hospitals
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | `30000000-...000000000001` |
| code | String (único) | `HGZ-21` |
| name | String | `Hospital General Zona 21` |
| address | String | `Av. Principal 100, Monterrey` |
| phone | String | `+52 81 0000 0001` |
| invite_code | String (único) | `INVITE-HGZ21` |
| active | Boolean | `true` |
| postal_code | String | `64000` |
| bed_count | Integer | `240` |
| doctor_count | Integer | `72` |
| nurse_count | Integer | `180` |
| municipality_id | FK → municipalities | UUID |
| latitude | Decimal | `25.6866142` |
| longitude | Decimal | `-100.3161126` |

Cardinalidad: un municipio tiene N hospitales; un hospital pertenece a 1 municipio (**1:N**).

---

### Área de Epidemiología

#### diseases
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| code | String (único) | `COVID-19` |
| name | String | `COVID-19` |
| specialty_id | FK → specialties | UUID |

Cardinalidad: una especialidad cubre N enfermedades; una enfermedad tiene 1 especialidad principal (**1:N**).

#### symptoms
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| code | String (único) | `FEVER` |
| name | String (único) | `Fiebre` |

#### disease_symptoms (asociativa)
| Atributo | Tipo |
|----------|------|
| disease_id | FK → diseases |
| symptom_id | FK → symptoms |

Cardinalidad: una enfermedad tiene N síntomas; un síntoma aparece en N enfermedades (**N:M**).

#### disease_specialties (asociativa)
| Atributo | Tipo |
|----------|------|
| disease_id | FK → diseases |
| specialty_id | FK → specialties |

Cardinalidad: una enfermedad puede asociarse a N especialidades; una especialidad cubre N enfermedades (**N:M**).

#### outbreaks
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| disease_id | FK → diseases | UUID |
| scope | Enum | `MUNICIPALITY`, `STATE` |
| municipality_id | FK → municipalities (nullable) | UUID |
| state_id | FK → states (nullable) | UUID |
| case_count | Integer | `150` |
| confirmation_status | Enum | `SUSPECTED`, `CONFIRMED` |
| status | Enum | `ACTIVE`, `RESOLVED` |
| started_at | Timestamp | `2026-01-15` |
| ended_at | Timestamp (nullable) | `2026-03-20` |

Cardinalidad: una enfermedad tiene N brotes; un brote pertenece a 1 enfermedad (**1:N**). Un brote se ubica en 1 municipio o 1 estado (**1:1** con municipalities o states).

#### alerts
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| outbreak_id | FK → outbreaks | UUID |
| severity | Enum | `HIGH`, `CRITICAL` |
| message | String | `Outbreak surge detected` |
| acknowledged_at | Timestamp (nullable) | `2026-05-01` |

Cardinalidad: un brote genera N alertas; una alerta pertenece a 1 brote (**1:N**).

---

### Área de Diagnóstico

#### patients
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| full_name | String | `Maria Garcia` |
| sex | Enum | `FEMALE` |
| birth_date | Date | `1985-06-15` |
| postal_code | String | `64000` |
| hospital_id | FK → hospitals | UUID |

Cardinalidad: un hospital tiene N pacientes; un paciente pertenece a 1 hospital (**1:N**).

#### diagnosis_assistant_threads / messages / suggestions / fixtures
Entidades de apoyo para el asistente de diagnóstico con IA. Relacionan un paciente, un thread de conversación, mensajes y sugerencias de enfermedad diferencial.

Cardinalidad: un paciente tiene N threads; un thread tiene N mensajes; un thread tiene N sugerencias (**1:N** en cada nivel).

---

### Área de Operación Hospitalaria

#### hospital_resource_snapshots
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| hospital_id | FK → hospitals | UUID |
| captured_at | Timestamp | `2026-05-20 08:00` |
| total_beds | Integer | `240` |
| available_beds | Integer | `60` |
| icu_total_beds | Integer | `20` |
| icu_available_beds | Integer | `5` |
| ... | ... | ... |
| source | Enum | `MANUAL` |

Cardinalidad: un hospital tiene N snapshots; un snapshot pertenece a 1 hospital (**1:N**).

#### hospital_department_resources
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| hospital_id | FK → hospitals | UUID |
| department_code | String | `ICU` |
| department_name | String | `Intensive Care Unit` |
| total_beds | Integer | `20` |
| occupied_beds | Integer | `15` |
| status | Enum | `HIGH_LOAD`, `NORMAL` |

Cardinalidad: un hospital tiene N departamentos; un departamento pertenece a 1 hospital (**1:N**).

#### hospital_staffing_profiles
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| hospital_id | FK → hospitals | UUID |
| role_code | String | `ICU_NURSE` |
| role_name | String | `ICU Nurses` |
| headcount | Integer | `18` |
| on_shift_count | Integer | `6` |

Cardinalidad: un hospital tiene N perfiles de personal; un perfil pertenece a 1 hospital (**1:N**).

#### hospital_inventory_items
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| hospital_id | FK → hospitals | UUID |
| item_code | String | `N95_MASK` |
| item_name | String | `N95 Respirator Masks` |
| category | String | `PPE` |
| current_quantity | Integer | `500` |
| capacity_quantity | Integer | `2000` |
| critical_threshold | Integer | `200` |
| status | Enum | `ADEQUATE`, `LOW`, `CRITICAL` |

Cardinalidad: un hospital tiene N items de inventario; un item pertenece a 1 hospital (**1:N**).

#### hospital_inventory_movements
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| hospital_id | FK → hospitals | UUID |
| inventory_item_id | FK → hospital_inventory_items | UUID |
| movement_type | Enum | `CONSUMPTION`, `REPLENISHMENT` |
| quantity_delta | Integer | `-120` |
| related_supply_request_id | FK → supply_requests (nullable) | UUID |

Cardinalidad: un item tiene N movimientos; un movimiento pertenece a 1 item (**1:N**). Un movimiento puede estar vinculado a 0 o 1 solicitudes de suministro (**0:1**).

#### operational_recommendations
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| hospital_id | FK → hospitals | UUID |
| type | Enum | `BED_CAPACITY`, `STAFFING`, `ISOLATION` |
| severity | Enum | `CRITICAL`, `HIGH`, `MEDIUM` |
| status | Enum | `NEW`, `ACCEPTED`, `ASSIGNED`, `COMPLETED`, `REJECTED` |
| category | String | `Bed Capacity` |
| title | String | `ICU Capacity Critical` |
| description | Text | `ICU occupancy is at 75%...` |
| confidence_score | Decimal | `0.95` |
| primary_department_resource_id | FK → hospital_department_resources (nullable) | UUID |
| primary_staffing_profile_id | FK → hospital_staffing_profiles (nullable) | UUID |
| primary_inventory_item_id | FK → hospital_inventory_items (nullable) | UUID |
| presentation_variant | String | `alert` |
| primary_action_code | String | `ASSIGN_TASK` |
| available_actions_json | JSON | `[{...}]` |
| allowed_status_transitions_json | JSON | `["ACCEPTED","ASSIGNED"]` |
| created_by_mode | Enum | `RULE_ENGINE`, `AI` |

Cardinalidad: un hospital tiene N recomendaciones; una recomendación pertenece a 1 hospital (**1:N**). Una recomendación puede referenciar 0 o 1 departamento, perfil de personal e item de inventario (**0:1** cada uno).

#### operational_recommendation_audit
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| recommendation_id | FK → operational_recommendations | UUID |
| actor_user_id | FK → users (nullable) | UUID |
| event_type | String | `STATUS_CHANGE` |
| event_label | String | `Accepted` |
| event_payload_json | JSON | `{...}` |
| created_at | Timestamp | `2026-05-20` |

Cardinalidad: una recomendación tiene N auditorías; una auditoría pertenece a 1 recomendación (**1:N**).

#### operational_tasks
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| recommendation_id | FK → operational_recommendations | UUID |
| hospital_id | FK → hospitals | UUID |
| owner_user_id | FK → users (nullable) | UUID |
| priority | Enum | `MEDIUM` |
| status | Enum | `PENDING`, `IN_PROGRESS`, `COMPLETED` |
| deadline_at | Timestamp (nullable) | `2026-06-01` |

Cardinalidad: una recomendación tiene N tareas; una tarea pertenece a 1 recomendación y 1 hospital (**1:N**).

#### operational_notifications
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| recommendation_id | FK → operational_recommendations | UUID |
| hospital_id | FK → hospitals | UUID |
| status | Enum | `SENT`, `DELIVERED`, `FAILED` |
| sent_at | Timestamp | `2026-05-20` |

Cardinalidad: una recomendación tiene N notificaciones; una notificación pertenece a 1 recomendación y 1 hospital (**1:N**).

#### supply_requests
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| recommendation_id | FK → operational_recommendations | UUID |
| hospital_id | FK → hospitals | UUID |
| inventory_item_id | FK → hospital_inventory_items (nullable) | UUID |
| quantity | Integer | `500` |
| status | Enum | `REQUESTED`, `FULFILLED`, `CANCELLED` |

Cardinalidad: una recomendación genera N solicitudes; una solicitud pertenece a 1 recomendación y 1 hospital (**1:N**).

#### hospital_operational_contacts
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| hospital_id | FK → hospitals | UUID |
| user_id | FK → users (nullable) | UUID |
| display_name | String | `Dr. Elena Ramirez` |
| role_label | String | `ICU Supervisor` |
| department_code | String | `ICU` |
| contact_channel | String | `PHONE` |
| contact_value | String | `+52 81 1111 0001` |
| is_assignable | Boolean | `true` |
| is_notifiable | Boolean | `true` |

Cardinalidad: un hospital tiene N contactos; un contacto pertenece a 1 hospital (**1:N**).

#### hospital_operational_groups
| Atributo | Tipo | Ejemplo |
|----------|------|---------|
| id | UUID | UUID |
| hospital_id | FK → hospitals | UUID |
| group_code | String | `ICU_RESPONSE` |
| group_name | String | `ICU Response Team` |
| group_type | String | `INCIDENT_RESPONSE` |
| is_assignable | Boolean | `true` |
| is_notifiable | Boolean | `true` |

Cardinalidad: un hospital tiene N grupos; un grupo pertenece a 1 hospital (**1:N**).

#### hospital_operational_group_members (asociativa)
| Atributo | Tipo |
|----------|------|
| id | UUID |
| group_id | FK → hospital_operational_groups |
| contact_id | FK → hospital_operational_contacts |

Cardinalidad: un grupo tiene N miembros (contactos); un contacto pertenece a N grupos (**N:M**).

---

## 2. Diagrama de relaciones principales

```text
states 1──N municipalities 1──N hospitals
                                    │
                    ┌───────────────┼───────────────┐───────────────┐
                    │               │               │               │
              patients    operational_      hospital_dept_    hospital_staffing_
                │         recommendations    resources        profiles
                │               │
         diagnosis_     ┌──────┼──────────┐
         assistant     │      │          │
         entities    tasks  notifications supply_requests
                        │          │            │
                        audit    sent_at    inventory_movements
```

```text
diseases 1──N outbreaks N──1 municipalities/states
                 │
              alerts

diseases N──M symptoms     diseases N──M specialties

users N──M roles N──M privileges
```

```text
hospitals 1──N operational_contacts N──M operational_groups
         1──N inventory_items 1──N inventory_movements
         1──N resource_snapshots
         1──N recommendations
```