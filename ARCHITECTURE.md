# StatusScope Backend Architecture

> Last updated: June 12, 2026

This document describes the backend as implemented in the current codebase. Older planning documents under `docs/plans` and some database notes are useful historical context, but the source of truth is the Quarkus code, Flyway migrations, JPA entities, and `README.md`.

## 1. System Role

The backend is the authoritative application service for StatuScope. It exposes a REST API for:

| Area | Responsibility |
| --- | --- |
| Authentication profile | Resolve Firebase identities into application users, roles, privileges, hospital scope, and status. |
| System administration | Global dashboard, hospital management, and cross-hospital user governance. |
| Hospital operations | Resource snapshots, departments, staffing, inventory, operational recommendations, tasks, notifications, contacts, groups, and supply requests. |
| Epidemiology | Local and state outbreak dashboards, maps, disease catalogs, alerts, breakdowns, and reports. |
| Diagnosis assistant | Patient evaluations, files, diagnosis suggestions, assistant threads, translated replies, historical case retrieval, and feedback. |
| Data ingestion | CSV-based municipal and state outbreak import at startup and via the ingestion toolchain. |

## 2. Runtime Stack

| Concern | Implementation |
| --- | --- |
| Runtime | Java 17, Quarkus 3.31.3 |
| HTTP | Quarkus REST Jackson / JAX-RS resources |
| Persistence | Hibernate ORM with Panache, JPA entities, MySQL 8 |
| Schema | Hibernate schema strategy per profile plus Flyway SQL and Java migrations |
| Auth provider | Firebase Admin SDK verifies Firebase ID tokens |
| Authorization | Application RBAC stored in MySQL and enforced by `@RequiresPrivilege` |
| AI providers | OpenAI and Gemini behind application-owned ports/adapters |
| Mail | Quarkus Mailer through `OperationalEmailGateway` |
| Tests | Quarkus JUnit, Rest Assured, Mockito, H2 test datasource |
| Deployment | Cloud Run, Cloud SQL, Secret Manager, GitHub Actions |

## 3. Layered Architecture

The package layout follows a layered architecture:

```text
src/main/java/com/itesm/interfaces/rest/
  REST resources, request boundaries, response construction, exception mapping

src/main/java/com/itesm/application/
  Use cases, DTOs, security contracts, outbound ports, workflow policies

src/main/java/com/itesm/domain/
  Domain models and repository interfaces

src/main/java/com/itesm/infrastructure/
  Firebase, LLM clients, mail gateway, bootstrap importers, JPA entities,
  repository implementations, mappers
```

The usual flow is:

```text
HTTP request
  -> FirebaseAuthFilter
  -> AuthorizationInterceptor when @RequiresPrivilege is present
  -> JAX-RS resource
  -> application use case
  -> domain repository interface
  -> infrastructure Panache/JPA implementation
  -> MySQL
```

Resources are intentionally thin. Business workflows live in `application/usecase`. Persistence details live in `infrastructure/persistence`.

## 4. Authentication And Authorization

Firebase is the identity provider, but MySQL is the source of truth for application access.

```text
Frontend Firebase login
  -> Firebase ID token
  -> Authorization: Bearer <token>
  -> FirebaseAuthFilter verifies token with Firebase Admin SDK
  -> UserRepository.findByExternalAuthId(firebaseUid)
  -> user.status must be ACTIVE
  -> AuthenticatedUserContext is populated
  -> @RequiresPrivilege enforces endpoint privilege
```

Public paths currently bypass the Firebase filter:

| Path prefix | Purpose |
| --- | --- |
| `auth/register` | Invite-code doctor registration |
| `q/health` | Quarkus health checks |
| `q/openapi` | OpenAPI metadata |

Important roles:

| Role | Scope |
| --- | --- |
| `SYSTEM_ADMIN` | Platform-wide access across hospitals |
| `HOSPITAL_ADMIN` | One hospital, operational and hospital-scoped workflows |
| `DOCTOR` | One hospital, epidemiology and diagnosis workflows |

Important privileges:

| Privilege | Used for |
| --- | --- |
| `isSystemAdmin` | System dashboard and hospital governance |
| `admin.operations` | Hospital admin dashboard, resources, recommendations, contacts, epidemiology |
| `users.read` | User listing |
| `users.manage` | User creation, update, and status changes |
| `roles.manage` | Role catalog and role assignment |
| `outbreaks.read` | Doctor epidemiology dashboards and analytics |
| `diagnosis.assist` | Diagnosis evaluations and assistant |

## 5. API Areas

### Authentication

| Method | Path | Use case |
| --- | --- | --- |
| `GET` | `/auth/me` | `GetMyProfileUseCase` |
| `POST` | `/auth/register` | `RegisterUserUseCase` |

`RegisterUserUseCase` validates the hospital invite code, creates the Firebase user, creates the MySQL user with the `DOCTOR` role, and deletes the Firebase user as compensation if database persistence fails.

### System Administration

| Method | Path | Privilege |
| --- | --- | --- |
| `GET` | `/system/dashboard/summary` | `isSystemAdmin` |
| `GET/POST/PUT/PATCH` | `/admin/hospitals...` | `isSystemAdmin` |
| `GET/POST/PUT/PATCH` | `/admin/users...` | `users.read`, `users.manage`, `roles.manage` |
| `GET` | `/admin/roles` | `roles.manage` |

System administrators are not hospital-scoped. Hospital admins are constrained to their own hospital in user management use cases.

### Hospital Operations

Hospital operations are exposed from `AdminOperationalResource` under `/admin` and require `admin.operations`.

Main resource groups:

| Group | Representative paths |
| --- | --- |
| Dashboard | `/admin/dashboard/summary` |
| Recommendations | `/admin/recommendations`, `/admin/recommendations/{id}`, workflow options, targets, refresh, status |
| Recommendation actions | tasks, notifications, supply requests |
| Resources | summary, departments, staffing, inventory, inventory movements, configuration |
| Directory | operational contacts and groups |

Supply requests that are linked to inventory use the MySQL stored procedure `sp_create_supply_request_with_movement` in MySQL profiles. Non-MySQL/test profiles fall back to JPA persistence.

### Epidemiology

Doctor endpoints live under `/doctor/dashboard` and require `outbreaks.read`.

Hospital-admin epidemiology endpoints live under `/admin/epidemiology` and require `admin.operations`.

Both areas read the same epidemiological model: `outbreaks`, `diseases`, `states`, `municipalities`, and `alerts`, but the response context is shaped for each user role.

### Diagnosis

Diagnosis endpoints require `diagnosis.assist`.

| Group | Paths |
| --- | --- |
| Disease catalog | `/diagnosis/diseases` |
| Evaluations | `/diagnosis/evaluations`, `/current`, `/{id}`, status, files, assistant feedback |
| Assistant | `/diagnosis/assistant/messages`, translations, evaluation thread restore |

Creating an evaluation persists a `patients` row and a `patient_evaluations` row for the current doctor and hospital. Assistant calls can persist a thread, user/assistant messages, parsed suggestions, retrieved historical cases, translations, and feedback events.

## 6. Database Architecture

The implemented schema is represented by:

```text
src/main/resources/import.sql
src/main/resources/db/migration/
src/main/java/db/migration/
src/main/resources/data/
scripts/mysql/
tools/ingesta-datos/
```

Core table groups:

| Group | Tables |
| --- | --- |
| Security | `users`, `roles`, `privileges`, `user_roles`, `role_privileges` |
| Geography | `states`, `municipalities`, `hospitals` |
| Epidemiology | `diseases`, `specialties`, `symptoms`, `disease_specialties`, `disease_symptoms`, `outbreaks`, `alerts` |
| Diagnosis | `patients`, `patient_evaluations`, `patient_evaluation_files`, `evaluation_differential_diagnoses`, `evaluation_recommended_tests`, `diagnosis_assistant_threads`, `diagnosis_assistant_messages`, `diagnosis_assistant_suggestions`, `diagnosis_assistant_retrieved_cases`, `diagnosis_feedback_events` |
| Operations | `hospital_resource_snapshots`, `hospital_department_resources`, `hospital_staffing_profiles`, `hospital_inventory_items`, `hospital_inventory_movements`, `operational_recommendations`, `operational_recommendation_audit`, `operational_tasks`, `operational_notifications`, `operational_notification_recipients`, `supply_requests`, `hospital_operational_contacts`, `hospital_operational_groups`, `hospital_operational_group_members` |
| Scheduled/materialized | `outbreak_daily_kpis` |

Important Flyway milestones:

| Migration | Purpose |
| --- | --- |
| `V1` Java | Mexico municipalities seed |
| `V3` Java | Diseases and symptoms seed |
| `V4` Java | Outbreak seed |
| `V5` Java | Persistent diagnosis assistant schema |
| `V6` SQL | Admin operational tables |
| `V7` SQL | Operational workflow contract and inventory movements |
| `V10` SQL | Operational notification recipients |
| `V12` SQL | Stored procedures, functions, and triggers |
| `V13` SQL | Optimization indexes |
| `V14` SQL | RBAC reference data |
| `V16` Java | Epidemiological catalog backfill |
| `V17`, `V18` SQL | System-admin and hospital-admin privilege split |

## 7. Stored Procedures, Functions, And Triggers

The advanced MySQL database module is implemented in `src/main/resources/db/migration/V12__advanced_database_module.sql`. Individual reference scripts live under `scripts/mysql`.

| Type | Name | Purpose |
| --- | --- | --- |
| Stored procedure | `sp_create_supply_request_with_movement` | Atomically creates a supply request and inventory replenishment movement |
| Stored procedure | `sp_generate_hospital_operational_summary` | Produces a consolidated operational summary |
| Function | `fn_bed_occupancy_pct` | Computes bed occupancy from the latest resource snapshot |
| Function | `fn_inventory_status` | Computes inventory status from quantity and threshold |
| Trigger | `trg_validate_inventory_before_insert` | Rejects inventory movements that would make quantity negative |
| Trigger | `trg_validate_inventory_before_update` | Recalculates inventory item status on update |
| Trigger | `trg_audit_recommendation_change` | Audits recommendation status changes |

The Java code uses `fn_bed_occupancy_pct` from `HospitalResourceRepositoryImpl` when the datasource is MySQL. It calls `sp_create_supply_request_with_movement` from `OperationalRecommendationRepositoryImpl` for inventory-backed supply requests. The individual files under `scripts/mysql/stored_procedures`, `scripts/mysql/functions`, and `scripts/mysql/triggers` mirror the current `V12` behavior; the benchmark runner applies experimental routine and trigger optimizations only inside the disposable `after` schema.

### Module 4 Evidence Automation

The Module 4 benchmark runner is `scripts/mysql/benchmark/run_module4_benchmark.py`.
It clones the configured MySQL source schema into disposable schemas named
`statusscope_module4_before` and `statusscope_module4_after`, applies advanced
database optimizations only to the `after` schema, and compares latency,
throughput, EXPLAIN ANALYZE output, routines, triggers, and event
materialization.

Output is written to the workspace-level `report-captures/module4/<run_id>/` with:

| Artifact | Purpose |
| --- | --- |
| `module4_database_report.pdf` | Main formatted report for the Module 4 deliverable |
| `metrics_summary.csv` | Aggregated before/after and concurrency metrics |
| `metrics_raw.json` | Raw benchmark samples, routine evidence, and incidents |
| `charts/` | PNG charts used by the PDF |
| `explain_plans/` | EXPLAIN ANALYZE captures per critical query |

Installation and execution instructions are documented in
`scripts/mysql/benchmark/README.md`.

## 8. Data Ingestion

The ingestion toolchain lives outside the Quarkus source tree:

```text
tools/ingesta-datos/
```

It produces versioned CSV files consumed by the backend:

```text
src/main/resources/data/outbreaks/municipal_outbreaks.csv
src/main/resources/data/outbreaks/state_outbreaks.csv
```

`OutbreakIngestionRunner` and `OutbreakCsvImporter` load these files at startup when `OUTBREAK_INGESTION_IMPORT_AT_START=true`. Local development enables this by default; production disables it unless configured.

## 9. AI Integration

Diagnosis and operational narratives are isolated behind application-owned contracts.

| Pattern | Location | Role |
| --- | --- | --- |
| Port | `application/port/out/AssistantChatGateway` | Application-facing chat contract |
| Message model | `AssistantChatMessage` | Provider-neutral message shape |
| Strategy | `infrastructure/llm/LlmChatStrategy` | Interchangeable provider behavior |
| OpenAI adapter | `infrastructure/openai/OpenAiChatClient` | OpenAI request/response mapping |
| Gemini adapter | `infrastructure/gemini/GeminiChatStrategy` | Gemini request/response mapping |
| Facade | `infrastructure/llm/LlmChatClient` | Provider selection/fallback facade |

`AskDiagnosisAssistantUseCase` builds the clinical prompt, includes hospital/geographic outbreak context, retrieves historical evaluations, calls the gateway, parses suggestions, and persists the assistant interaction when an evaluation id is present.

## 10. Configuration Profiles

| Profile | Schema behavior | Notes |
| --- | --- | --- |
| default | `drop-and-create` + `import.sql` | Destructive local development |
| `demo` | `drop-and-create` + `import.sql` | Explicit demo profile |
| `local-persistent` | `validate` + Flyway | Persistent local database |
| `local-schema-update` | `update` | One-time local schema helper |
| `prod` | `validate` | Cloud Run / Cloud SQL |
| `test` | H2 `drop-and-create` | Automated tests, no Firebase service account |

## 11. Documentation Ownership

Use these documents as final-state references:

| Document | Purpose |
| --- | --- |
| `README.md` | Setup, stack, API surface, deployment |
| `ARCHITECTURE.md` | Implemented backend architecture |
| `docs/database/05-modelo-logico.md` | Logical model |
| `docs/database/06-modelo-fisico.md` | Physical model |
| `docs/database/08-diccionario-datos.md` | Data dictionary |
| `docs/database/10-justificacion-sp-funciones-triggers.md` | Stored procedures, functions, triggers |
| `tools/ingesta-datos/README.md` | Epidemiological ingestion pipeline |

When architecture and code disagree, update the documentation from the code, not from historical plans.
