# Hospital Admin AI Recommendations Backend Plan

## Feature Identified In The UI

The feature you described is the `hospital_admin` recommendations experience, primarily in:

- `StatuScope-FrontEnd/components/views/admin/recommendations/index.tsx`
- `StatuScope-FrontEnd/components/views/admin/dashboard/index.tsx`
- `StatuScope-FrontEnd/components/views/admin/resources/index.tsx`

Why this is the correct feature:

- the recommendations page is titled **"AI Operational Recommendations"**
- its copy says **"Predictive resource management based on real-time clinical data streams."**
- the cards already describe outbreak-driven actions like opening beds, moving staff, and ordering supplies
- the action overlays already assume the user can:
  - assign a task
  - notify staff
  - order supplies
  - change recommendation status

Today, all of that is mocked in the UI with hardcoded arrays and local state. There is no backend contract that makes the flow functional.

## Current Backend Reuse We Already Have

The current backend already gives us strong building blocks:

- `hospitals`
  - has baseline capacity fields: `bed_count`, `doctor_count`, `nurse_count`
- `users`
  - hospital-scoped users already exist
- `outbreaks`
  - seeded and queryable by municipality/state
- `alerts`
  - existing outbreak-linked alert table
- `events`
  - can represent active disease burden in the hospital population
- `patient_evaluations`
  - can be used for recent intake / case trend context
- existing geographic/outbreak logic
  - `HospitalGeoContextService`
  - `OutbreakRepository`
  - `GetDoctorDashboardSummaryUseCase`

This means we do **not** need to invent the epidemiological side from zero. The main missing piece is the hospital-admin operational layer: resource snapshots, recommendations, and action execution records.

## Main Gap

The admin UI currently expects data that the schema does not yet model:

- recommendation feed items with rationale, confidence, urgency, affected departments, and affected resources
- recommendation lifecycle state: `new`, `accepted`, `assigned`, `completed`, `rejected`
- audit trail entries
- generated operational tasks
- generated staff notifications
- generated supply requests
- richer live resource data than the three numeric columns on `hospitals`
- department-level bed utilization
- inventory stock levels
- specialist counts beyond doctor/nurse totals

So the backend plan needs to cover both:

1. recommendation generation
2. the operational data model the recommendations depend on

## Proposed Architecture

Build this feature as a hospital-scoped backend module with four layers:

1. **Operational resource state**
   - current hospital capacity, department occupancy, staffing distribution, and inventory levels
2. **Recommendation engine**
   - combines outbreaks + hospital load + resource state into actionable recommendations
3. **Recommendation workflow**
   - lets hospital admins accept, assign, reject, complete, notify, and order supplies
4. **Admin read APIs**
   - powers dashboard, recommendations, and resources screens with real data

## Data Model Plan

### Reuse existing tables

- `hospitals`
  - keep as hospital-level baseline configuration
- `outbreaks`
  - primary epidemiological signal input
- `alerts`
  - keep as outbreak alert source; admin dashboard can consume these directly or through a projection
- `users`
  - owners/assignees for recommendation tasks and actions
- `events`
  - use active event counts by disease as one of the hospital burden inputs

### New tables to add

#### 1. `hospital_resource_snapshots`

Purpose:
- current hospital-wide operational state at a point in time

Suggested columns:
- `id`
- `hospital_id`
- `captured_at`
- `total_beds`
- `available_beds`
- `icu_total_beds`
- `icu_available_beds`
- `isolation_rooms_total`
- `isolation_rooms_available`
- `oxygen_capacity_units`
- `oxygen_available_units`
- `doctors_on_shift`
- `nurses_on_shift`
- `specialists_on_shift`
- `source` (`MANUAL`, `INTEGRATION`, `DERIVED`)
- `created_at`

Reason:
- the admin dashboard top cards cannot be backed only by `hospitals.bed_count/doctor_count/nurse_count`

#### 2. `hospital_department_resources`

Purpose:
- department-level capacity and occupancy shown in the resources page

Suggested columns:
- `id`
- `hospital_id`
- `department_code`
- `department_name`
- `level_label`
- `total_beds`
- `occupied_beds`
- `status`
- `notes`
- `updated_at`

#### 3. `hospital_staffing_profiles`

Purpose:
- counts by specialty / role for resources and recommendation targeting

Suggested columns:
- `id`
- `hospital_id`
- `role_code`
- `role_name`
- `headcount`
- `on_shift_count`
- `on_call_count`
- `standby_count`
- `updated_at`

This supports UI values like pulmonologists, infectious disease specialists, emergency physicians, etc.

#### 4. `hospital_inventory_items`

Purpose:
- inventory cards and supply ordering recommendations

Suggested columns:
- `id`
- `hospital_id`
- `item_code`
- `item_name`
- `category`
- `location`
- `current_quantity`
- `capacity_quantity`
- `unit`
- `critical_threshold`
- `target_quantity`
- `status`
- `updated_at`

#### 5. `operational_recommendations`

Purpose:
- canonical AI recommendation record for hospital admins

Suggested columns:
- `id`
- `hospital_id`
- `source_alert_id` nullable
- `source_outbreak_id` nullable
- `type` (`BED_CAPACITY`, `STAFFING`, `SUPPLY`, `TRIAGE`, `ISOLATION`, `CUSTOM`)
- `severity`
- `status`
- `category`
- `title`
- `description`
- `expected_impact`
- `urgency_window`
- `confidence_score`
- `image_mode`
- `rationale_json`
- `recommended_actions_json`
- `affected_departments_json`
- `affected_resources_json`
- `model_provider`
- `model_version`
- `input_context_json`
- `created_by_mode` (`RULE_ENGINE`, `LLM_ASSISTED`)
- `created_at`
- `updated_at`
- `resolved_at` nullable

#### 6. `operational_recommendation_audit`

Purpose:
- audit trail shown in the UI

Suggested columns:
- `id`
- `recommendation_id`
- `actor_user_id` nullable
- `event_type` (`GENERATED`, `STATUS_CHANGED`, `TASK_CREATED`, `NOTIFICATION_SENT`, `SUPPLY_REQUESTED`, `DISMISSED`, `COMPLETED`)
- `event_label`
- `event_payload_json`
- `created_at`

#### 7. `operational_tasks`

Purpose:
- persists "Assign task" overlay output

Suggested columns:
- `id`
- `recommendation_id`
- `hospital_id`
- `owner_user_id` nullable
- `owner_label`
- `department_label`
- `deadline_at` nullable
- `priority`
- `notes`
- `status`
- `created_by_user_id`
- `created_at`
- `updated_at`

#### 8. `operational_notifications`

Purpose:
- persists "Notify staff" overlay output

Suggested columns:
- `id`
- `recommendation_id`
- `hospital_id`
- `audience_label`
- `message`
- `status`
- `sent_by_user_id`
- `sent_at`

#### 9. `supply_requests`

Purpose:
- persists "Order supplies" overlay output

Suggested columns:
- `id`
- `recommendation_id`
- `hospital_id`
- `inventory_item_id` nullable
- `supply_type_label`
- `quantity`
- `unit`
- `destination`
- `suggested_supplier`
- `status`
- `requested_by_user_id`
- `created_at`
- `updated_at`

## Recommendation Generation Strategy

## Important rule

The backend should **not** let the LLM invent hard numbers for resources by itself.

Use a two-step approach:

1. **Deterministic operational calculator**
   - computes deficits and suggested quantities from outbreak severity + active hospital state
2. **LLM summarizer**
   - turns those computed numbers into human-readable rationale/action text

That avoids hallucinated resource counts while still delivering an AI explanation.

### Inputs

- active local/state outbreaks from `outbreaks`
- outbreak alerts from `alerts`
- current hospital resource snapshot
- department occupancy
- staffing profiles
- inventory levels
- hospital active disease burden from `events`
- recent evaluation volume from `patient_evaluations`

### Example generation rule

If:

- nearby active COVID-19 outbreak severity is high
- ICU and oxygen availability are already below thresholds
- recent respiratory evaluations are increasing

Then the calculator creates a recommendation payload like:

- type: `BED_CAPACITY`
- suggested additional monitored beds: `6`
- suggested oxygen reserve target: `+20%`
- suggested staffing gap: `2 respiratory nurses`

The LLM only converts that into narrative:

- title
- short description
- rationale bullets
- recommended action bullets

## End-to-End Backend Flows

### 1. Recommendation feed

Frontend replacement:
- `components/views/admin/recommendations/index.tsx`

Backend flow:
- scheduled job or manual refresh endpoint runs recommendation generation per hospital
- generated rows are stored in `operational_recommendations`
- UI reads `/admin/recommendations`

### 2. Recommendation detail

Frontend replacement:
- `RecommendationDetailOverlay`

Backend flow:
- `GET /admin/recommendations/{id}`
- returns full rationale, impact, departments/resources, and audit trail

### 3. Status change

Frontend replacement:
- local `setRecommendations(...)` status mutation

Backend flow:
- `PATCH /admin/recommendations/{id}/status`
- writes recommendation status
- appends audit event

### 4. Assign task

Frontend replacement:
- `RecommendationTaskOverlay`

Backend flow:
- `POST /admin/recommendations/{id}/tasks`
- creates `operational_tasks`
- optionally sets recommendation status to `assigned`
- appends audit event

### 5. Notify staff

Frontend replacement:
- `RecommendationNotifyOverlay`

Backend flow:
- `POST /admin/recommendations/{id}/notifications`
- creates `operational_notifications`
- appends audit event

### 6. Order supplies

Frontend replacement:
- `RecommendationSupplyOverlay`

Backend flow:
- `POST /admin/recommendations/{id}/supply-requests`
- creates `supply_requests`
- appends audit event

### 7. Admin dashboard

Frontend replacement:
- `components/views/admin/dashboard/index.tsx`

Backend flow:
- `GET /admin/dashboard/summary`
- returns:
  - top metrics
  - outbreak alerts
  - hospital map zones
  - linked recommended actions

Implementation note:
- this can initially reuse the shape/pattern of `GetDoctorDashboardSummaryUseCase`, but adapted for hospital admin operations instead of doctor diagnosis context

### 8. Admin resources

Frontend replacement:
- `components/views/admin/resources/index.tsx`

Backend flow:
- `GET /admin/resources/summary`
- `GET /admin/resources/departments`
- `GET /admin/resources/staffing`
- `GET /admin/resources/inventory`
- optional update endpoints for manual operational input

## API Proposal

### Read APIs

- `GET /admin/dashboard/summary`
- `GET /admin/recommendations?status=&severity=&type=`
- `GET /admin/recommendations/{id}`
- `GET /admin/resources/summary`
- `GET /admin/resources/departments`
- `GET /admin/resources/staffing`
- `GET /admin/resources/inventory`

### Command APIs

- `POST /admin/recommendations/refresh`
- `PATCH /admin/recommendations/{id}/status`
- `POST /admin/recommendations/{id}/tasks`
- `POST /admin/recommendations/{id}/notifications`
- `POST /admin/recommendations/{id}/supply-requests`
- `PUT /admin/resources/summary`
- `PUT /admin/resources/departments/{departmentId}`
- `PUT /admin/resources/staffing/{profileId}`
- `PUT /admin/resources/inventory/{itemId}`

## Suggested Use Cases / Services

- `GetAdminDashboardSummaryUseCase`
- `ListOperationalRecommendationsUseCase`
- `GetOperationalRecommendationDetailUseCase`
- `RefreshOperationalRecommendationsUseCase`
- `UpdateOperationalRecommendationStatusUseCase`
- `CreateOperationalTaskUseCase`
- `CreateOperationalNotificationUseCase`
- `CreateSupplyRequestUseCase`
- `GetHospitalResourcesUseCase`
- `UpdateHospitalResourcesUseCase`
- `OperationalRecommendationEngine`
- `OperationalResourceProjectionService`

## Phased Delivery Plan

### Phase 1: Make the UI read real backend data

Deliver:

- new schema tables
- admin dashboard summary endpoint
- recommendations list/detail/status endpoints
- resources summary/departments/staffing/inventory endpoints

Result:

- all hardcoded arrays in admin dashboard, recommendations, and resources pages can be removed

### Phase 2: Make overlays functional

Deliver:

- task creation endpoint
- notification creation endpoint
- supply request endpoint
- audit trail endpoint/data

Result:

- "Assign task", "Notify staff", and "Order supplies" become real workflows

### Phase 3: Automated recommendation generation

Deliver:

- scheduled generation job
- deterministic rules + LLM narrative composition
- dedupe/update strategy for repeated recommendations

Result:

- recommendation feed becomes continuously refreshed instead of manually seeded

## Frontend Mocks This Plan Removes

This plan removes hardcoded data from:

- `components/views/admin/recommendations/index.tsx`
  - `initialRecommendations`
  - tab counts derived from mock data
  - local-only audit/status transitions
- `components/views/admin/dashboard/index.tsx`
  - `alerts`
  - `topCards`
  - `mapZones`
- `components/views/admin/resources/Sub-funcionalidades/types.ts`
  - `defaultResourceConfiguration`
  - `defaultDepartments`
  - `defaultRoster`
  - `defaultInventory`

## Key Implementation Decisions

### 1. Keep hospital scope strict

All admin endpoints should resolve the caller's hospital from the authenticated user and never accept arbitrary hospital IDs for hospital admins.

### 2. Separate facts from AI wording

- facts and quantities come from deterministic computation
- AI is only used to summarize and explain

### 3. Store recommendation inputs

Persist `input_context_json` so each recommendation can be audited later:

- which outbreaks were active
- which resource values were used
- which event/evaluation counts influenced the decision

### 4. Prefer additive schema changes

The current schema is missing operational entities, so this should be delivered as new Flyway migrations rather than forcing the existing hospital table to absorb everything.

## Recommended First Build Order

1. Add migrations for:
   - `hospital_resource_snapshots`
   - `hospital_department_resources`
   - `hospital_staffing_profiles`
   - `hospital_inventory_items`
   - `operational_recommendations`
   - `operational_recommendation_audit`
   - `operational_tasks`
   - `operational_notifications`
   - `supply_requests`
2. Seed one realistic hospital-admin dataset for the default hospital
3. Implement `GET /admin/recommendations` and `GET /admin/recommendations/{id}`
4. Implement `PATCH /admin/recommendations/{id}/status`
5. Implement `GET /admin/dashboard/summary`
6. Implement `GET /admin/resources/*`
7. Implement task/notification/supply command endpoints
8. Add scheduled recommendation generation

## Final Recommendation

Build this as a **hospital operational intelligence module**, not as a thin LLM endpoint.

The UI is already designed around a persistent workflow system, not a one-off chatbot answer. To make the feature fully functional and remove all hardcoded values, the backend must own:

- operational hospital state
- recommendation generation
- recommendation persistence
- recommendation actions
- audit history

That approach fits the current schema direction, reuses the existing outbreak/hospital foundations, and gives the `hospital_admin` a real end-to-end product instead of a mocked AI feed.
