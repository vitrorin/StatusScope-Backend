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

## Implementation Status (Updated 2026-05-12)

### Backend completed

- schema support is implemented
  - `hospital_resource_snapshots`
  - `hospital_department_resources`
  - `hospital_staffing_profiles`
  - `hospital_inventory_items`
  - `operational_recommendations`
  - `operational_recommendation_audit`
  - `operational_tasks`
  - `operational_notifications`
  - `supply_requests`
- realistic seed data exists for the default hospital
- hospital-scoped admin APIs are implemented in `AdminOperationalResource`
  - `GET /admin/dashboard/summary`
  - `GET /admin/recommendations`
  - `GET /admin/recommendations/{id}`
  - `POST /admin/recommendations/refresh`
  - `PATCH /admin/recommendations/{id}/status`
  - `POST /admin/recommendations/{id}/tasks`
  - `POST /admin/recommendations/{id}/notifications`
  - `POST /admin/recommendations/{id}/supply-requests`
  - `GET /admin/resources/summary`
  - `GET /admin/resources/departments`
  - `GET /admin/resources/staffing`
  - `GET /admin/resources/inventory`
  - `PUT /admin/resources/summary`
  - `PUT /admin/resources/departments/{departmentId}`
  - `PUT /admin/resources/staffing/{profileId}`
  - `PUT /admin/resources/inventory/{itemId}`
- recommendation workflow is implemented end-to-end
  - status transitions are persisted
  - tasks, notifications, and supply requests are persisted
  - audit trail entries are persisted and returned in recommendation detail
- deterministic recommendation generation is implemented
  - uses outbreaks
  - uses nearby alerts
  - uses hospital resource snapshots
  - uses inventory criticality
  - uses active events
  - uses recent patient evaluation volume
- automated refresh is implemented
  - manual refresh endpoint exists
  - scheduled refresh job now runs across hospitals
  - duplicate open recommendations are refreshed instead of duplicated
- recommendation provenance is persisted
  - `source_alert_id`
  - `source_outbreak_id`
  - `input_context_json`
  - `model_provider`
  - `model_version`
- backend test coverage now includes the admin operational module

### Backend intentionally not implemented yet

- LLM narrative composition for recommendation wording
  - the backend is now functionally complete using deterministic text/rationale generation
  - LLM summarization remains an optional enhancement, not a blocker for frontend integration

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

Status:
- Completed on backend

Deliver:

- new schema tables
- admin dashboard summary endpoint
- recommendations list/detail/status endpoints
- resources summary/departments/staffing/inventory endpoints

Result:

- all hardcoded arrays in admin dashboard, recommendations, and resources pages can be removed

### Phase 2: Make overlays functional

Status:
- Completed on backend

Deliver:

- task creation endpoint
- notification creation endpoint
- supply request endpoint
- audit trail endpoint/data

Result:

- "Assign task", "Notify staff", and "Order supplies" become real workflows

### Phase 3: Automated recommendation generation

Status:
- Completed for deterministic generation, scheduled refresh, and dedupe
- Optional LLM wording enhancement still open

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

---

## Frontend Completion Addendum (Added 2026-05-12)

The backend now supports the core operational workflows, but the current admin frontend still contains product logic and presentation assumptions that are hardcoded in the UI.

Examples of what is still frontend-guessed today:

- which actions should appear on a recommendation card
- which status transitions are allowed from the current state
- which visual treatment should be used for a recommendation
- which department or inventory item should be treated as the primary target
- which users can be assigned for a task
- how dashboard cards should be interpreted and rendered

To make the feature fully functional and remove those assumptions, the backend contract should be extended so the UI becomes a renderer of backend-defined workflow state instead of a place where workflow rules are duplicated.

## Additional Goal

Make the hospital admin dashboard, recommendations, and resources screens:

1. fully backend-driven
2. audit-safe
3. role-aware
4. assignable to real hospital actors
5. free of hardcoded product behavior in the frontend

## Remaining Gaps To Close

### 1. Recommendation workflow metadata is still implicit

Current state:

- the backend returns recommendation type, severity, and status
- the frontend infers actions such as:
  - assign task
  - notify staff
  - order supplies
  - dismiss
- the frontend also infers which statuses are available to set

Problem:

- business rules now live in two places
- the UI can drift from the backend
- changes to workflow rules require frontend rewrites

Required improvement:

- the backend should explicitly return:
  - allowed actions
  - allowed status transitions
  - preferred primary action
  - action disable reasons when applicable

### 2. Recommendation targeting is still too generic

Current state:

- recommendations have `affected_departments_json` and `affected_resources_json`
- the UI treats these as display strings only

Problem:

- tasks, notifications, and supply requests are not tied strongly enough to concrete records
- the UI cannot know the primary department, primary staffing profile, or primary inventory item without guessing

Required improvement:

- recommendations should point to concrete target entities whenever possible

### 3. The frontend has no real assignee model for hospital operations

Current state:

- tasks can persist an owner label
- the UI does not have a backend-driven list of assignable staff or operational roles

Problem:

- assignment is not truly operational
- task ownership remains partly free text

Required improvement:

- add a real assignment directory for hospital admins to select from

### 4. Dashboard cards are only partially declarative

Current state:

- the dashboard summary endpoint returns card data
- the frontend still interprets status, progress, badges, and some actions heuristically

Problem:

- presentation semantics are still duplicated in UI logic

Required improvement:

- return more explicit dashboard card metadata

### 5. Resources UI still synthesizes staff and inventory behavior

Current state:

- staffing profiles are aggregated
- the frontend synthesizes a pseudo-roster from counts
- inventory actions are still generic and not tied to explicit replenishment workflow definitions

Problem:

- the screen is operationally useful, but not truly complete

Required improvement:

- differentiate between:
  - staffing capacity profiles
  - real assignable staff directory
  - inventory action recommendations
  - inventory replenishment workflows

## Recommended Data Model Extensions

### A. Extend `operational_recommendations`

Add columns:

- `primary_department_resource_id` nullable
- `primary_staffing_profile_id` nullable
- `primary_inventory_item_id` nullable
- `presentation_variant` nullable
- `primary_action_code` nullable
- `available_actions_json`
- `allowed_status_transitions_json`
- `display_category_label` nullable
- `display_severity_label` nullable
- `display_status_label` nullable
- `expires_at` nullable
- `assigned_owner_user_id` nullable

Purpose:

- eliminate frontend guessing for action visibility and state transitions
- let the UI render the exact workflow options supported by backend rules
- connect recommendations to real resource records

Suggested `available_actions_json` example:

```json
[
  { "code": "ASSIGN_TASK", "label": "Assign task", "style": "primary", "enabled": true },
  { "code": "NOTIFY_STAFF", "label": "Notify staff", "style": "secondary", "enabled": true },
  { "code": "ORDER_SUPPLIES", "label": "Order supplies", "style": "secondary", "enabled": false, "disabledReason": "No inventory item linked" }
]
```

Suggested `allowed_status_transitions_json` example:

```json
["ACCEPTED", "ASSIGNED", "COMPLETED", "REJECTED"]
```

### B. Add `hospital_operational_contacts`

Purpose:

- real assignment and notification directory for hospital admins

Suggested columns:

- `id`
- `hospital_id`
- `user_id` nullable
- `display_name`
- `role_label`
- `department_code` nullable
- `contact_channel`
- `contact_value`
- `availability_status`
- `is_assignable`
- `is_notifiable`
- `updated_at`

Reason:

- the recommendation overlays currently rely on free text
- this table allows:
  - assignable owners
  - notifyable groups
  - routing suggestions

### C. Add `hospital_operational_groups`

Purpose:

- define reusable notification and assignment audiences

Suggested columns:

- `id`
- `hospital_id`
- `group_code`
- `group_name`
- `group_type` (`DEPARTMENT`, `SHIFT_TEAM`, `INCIDENT_RESPONSE`, `SUPPLY_CHAIN`, `EXECUTIVE`)
- `is_assignable`
- `is_notifiable`
- `updated_at`

### D. Add `hospital_operational_group_members`

Purpose:

- many-to-many link between contacts and groups

Suggested columns:

- `id`
- `group_id`
- `contact_id`
- `created_at`

### E. Extend `operational_tasks`

Add columns:

- `owner_contact_id` nullable
- `owner_group_id` nullable
- `source_action_code` nullable
- `recommended_by_recommendation_id` nullable

Reason:

- convert assignment from string-only ownership to linked operational ownership

### F. Extend `operational_notifications`

Add columns:

- `audience_group_id` nullable
- `audience_contact_id` nullable
- `delivery_channel`
- `delivery_status_detail`
- `source_action_code` nullable

Reason:

- make notifications auditable and route-aware

### G. Extend `supply_requests`

Add columns:

- `source_action_code` nullable
- `priority`
- `requested_needed_by` nullable
- `linked_recommendation_inventory_item_id` nullable

Reason:

- allow UI and operations staff to understand urgency and direct item linkage

### H. Add `operational_recommendation_action_log`

Purpose:

- record user-visible action execution separately from generic audit trail

Suggested columns:

- `id`
- `recommendation_id`
- `action_code`
- `actor_user_id` nullable
- `target_entity_type` nullable
- `target_entity_id` nullable
- `result_status`
- `result_message`
- `created_at`

Reason:

- the audit trail is good for history
- this table is better for structured action execution and UI activity panels

### I. Optional: Add `hospital_inventory_movements`

Purpose:

- support richer inventory UI over time

Suggested columns:

- `id`
- `hospital_id`
- `inventory_item_id`
- `movement_type` (`CONSUMPTION`, `REPLENISHMENT`, `TRANSFER`, `MANUAL_ADJUSTMENT`)
- `quantity_delta`
- `unit`
- `notes`
- `related_supply_request_id` nullable
- `created_at`

Reason:

- inventory cards should eventually use real movement history instead of only snapshot values

## Required Backend Contract Changes

### 1. Extend recommendation DTOs

Add to `OperationalRecommendationDto`:

- `displayCategoryLabel`
- `displaySeverityLabel`
- `displayStatusLabel`
- `primaryDepartment`
- `primaryStaffingProfile`
- `primaryInventoryItem`
- `availableActions`
- `allowedStatusTransitions`
- `primaryActionCode`
- `expiresAt`
- `assignedOwner`

Suggested nested DTOs:

- `RecommendationActionDto`
  - `code`
  - `label`
  - `style`
  - `enabled`
  - `disabledReason`
- `RecommendationTargetDto`
  - `id`
  - `label`
  - `type`

### 2. Add assignee and audience lookup APIs

New APIs:

- `GET /admin/operational-contacts`
- `GET /admin/operational-groups`

Optional filters:

- `?assignable=true`
- `?notifiable=true`
- `?departmentCode=ICU`

Purpose:

- fill task assignment and notification overlays with real data

### 3. Add recommendation helper endpoints

New APIs:

- `GET /admin/recommendations/{id}/workflow-options`
- `GET /admin/recommendations/{id}/targets`

These can also be embedded directly inside detail responses if preferred.

Purpose:

- avoid frontend duplication of operational decision logic

### 4. Extend dashboard summary DTO

Add to each top card:

- `displayVariant`
- `progressPercent`
- `progressColorToken`
- `badgeTone`
- `recommendedActionId` nullable
- `actionLabel` nullable

Add to map zones:

- `displayPriorityLabel`
- `recommendedActionId` nullable
- `displayColorToken`

Purpose:

- make dashboard rendering declarative rather than inferred

### 5. Extend resources APIs

Current APIs are enough for snapshots and updates, but add:

- `GET /admin/resources/configuration`
- `GET /admin/resources/operational-roster`
- `GET /admin/resources/inventory/{itemId}/movements`

Purpose:

- distinguish editable operational configuration from live shift/state data
- support a real roster overlay
- support richer inventory dialogs

## Recommendation Engine Changes

The recommendation engine should also populate the new workflow metadata.

For each generated recommendation it should decide:

- primary target entity
- available actions
- valid transitions
- preferred owner role or group
- preferred audience group
- preferred inventory item linkage
- primary action code

Example:

If recommendation type is `SUPPLY` and it is linked to a critical inventory item:

- `primary_inventory_item_id` = linked item
- `primary_action_code` = `ORDER_SUPPLIES`
- `available_actions` includes `ORDER_SUPPLIES`
- `allowed_status_transitions` includes `ACCEPTED`, `ASSIGNED`, `REJECTED`

If recommendation type is `STAFFING`:

- `primary_staffing_profile_id` = emergency physicians profile
- `available_actions` includes `ASSIGN_TASK` and `NOTIFY_STAFF`
- `preferred audience` = emergency staffing group

## UI Changes Required To Remove Hardcoding

### Recommendations page

Replace hardcoded UI logic with backend-driven rendering for:

- action buttons
- status chips
- display labels
- primary target display
- assignee suggestions
- notification audience suggestions
- inventory request defaults

Frontend should stop inferring:

- whether `Order supplies` should appear
- whether `Assigned` is a valid status
- which department to prefill
- which supply type to prefill

Instead it should read:

- `availableActions`
- `allowedStatusTransitions`
- `primaryDepartment`
- `primaryInventoryItem`
- `assignedOwner`

### Recommendation overlays

Task overlay should use:

- `GET /admin/operational-contacts?assignable=true`
- recommendation-linked defaults from detail payload

Notify overlay should use:

- `GET /admin/operational-groups?notifiable=true`
- recommendation preferred audience if present

Supply overlay should use:

- linked inventory item metadata
- recommendation preferred quantity or target item if present

### Dashboard page

Replace hardcoded interpretation of:

- card progress
- card tone
- zone priority labels
- zone colors
- alert-derived recommended actions

Use DTO-driven display fields instead.

### Resources page

Replace synthesized pseudo-roster with:

- backend operational roster endpoint

Replace generic inventory actions with:

- item-specific workflow options
- movement history
- request priority and destination defaults

## Additional Backend Use Cases / Services

- `ListOperationalContactsUseCase`
- `ListOperationalGroupsUseCase`
- `GetOperationalRosterUseCase`
- `GetInventoryMovementHistoryUseCase`
- `GetRecommendationWorkflowOptionsUseCase`
- `RecommendationWorkflowPolicyService`
- `RecommendationTargetResolver`

## Extended Phased Delivery

### Phase 4: Remove frontend workflow hardcoding

Deliver:

- recommendation workflow metadata in DTOs
- operational contacts and groups APIs
- real roster API
- richer dashboard card metadata

Result:

- frontend no longer guesses actions, targets, transitions, or audiences

### Phase 5: Strengthen operational linkage

Deliver:

- recommendation-to-entity linking
- assignment directory
- inventory movement history
- structured action log

Result:

- the admin UI becomes an operational console, not just a visual wrapper around recommendation text

## Updated Final Recommendation

The backend is already sufficient for a real first version of the hospital admin recommendations experience.

To make the system fully functional and remove the remaining hardcoded frontend behavior, the next step is **not** primarily more AI. The next step is a stronger workflow contract:

- explicit available actions
- explicit allowed transitions
- explicit primary targets
- explicit assignable/notifiable operational actors
- more declarative dashboard/resource DTOs

That turns the frontend from a rule engine into a renderer of backend-defined hospital operations state, which is the right architecture for a persistent admin product.
