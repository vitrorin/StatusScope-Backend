# Diagnosis Assistant — Feature Specification

This document describes the AI assistant that helps a doctor build a differential
diagnosis from a patient evaluation. It covers what the feature does end-to-end,
the HTTP contract, the database model, how the LLM is invoked, how retrieval and
feedback work, and the boundaries of the current version.


## Goal

Give a doctor a chat-style assistant attached to a single patient evaluation that:

- proposes a ranked differential diagnosis grounded in the patient's symptoms,
- uses the hospital's active outbreak context to bias suggestions toward locally
  plausible diseases,
- uses prior confirmed cases at the same hospital as additional grounding,
- persists the entire conversation, structured suggestions, and the doctor's
  final decision so the data can later be audited and analysed,
- works the same way regardless of which LLM provider serves the request.

The assistant is **not** a diagnosis system — it never confirms a diagnosis on
its own. The doctor remains the only entity that confirms, overrides, or
rejects a clinical conclusion.

## High-level flow

```
Doctor opens /diagnosis
        │
        ▼
GET /diagnosis/evaluations/current ── if exists, hydrate form
        │
        ▼
GET /diagnosis/assistant/evaluations/{id}/thread
        │
        ▼  (the chat panel shows the prior conversation + structured suggestions)
        │
Doctor types a message
        │
        ▼
POST /diagnosis/assistant/messages
        │
        ├─ Backend persists the user message
        ├─ Backend retrieves similar confirmed cases at the hospital (last 12mo)
        ├─ Backend builds the system prompt
        ├─ Backend calls the LLM (OpenAI primary, Gemini fallback)
        ├─ Backend parses <DIAGNOSIS_JSON> block from the reply
        ├─ Backend persists the assistant message, structured suggestions,
        │   and the audit log of retrieved cases
        └─ Backend returns reply + suggestions + contextUsed
        │
        ▼
Doctor confirms or rejects via the feedback panel
        │
        ▼
POST /diagnosis/evaluations/{id}/assistant-feedback
        │
        └─ Backend writes final_* fields on the evaluation and a
           diagnosis_feedback_events row
```

## HTTP API

All endpoints require Firebase-authenticated requests and the `diagnosis.assist`
privilege (granted to the `DOCTOR` role). Unauthenticated calls return 401;
authenticated callers without the privilege return 403.

### Evaluation lifecycle

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/diagnosis/evaluations` | Create a new evaluation (patient name, birth date, sex, symptoms). |
| `GET`  | `/diagnosis/evaluations/current` | Return the most recent evaluation for the calling doctor. 404 if none. |
| `GET`  | `/diagnosis/evaluations/{id}` | Read a single evaluation owned by the doctor. |
| `PUT`  | `/diagnosis/evaluations/{id}` | Update patient and symptoms. |
| `POST` | `/diagnosis/evaluations/{id}/status` | Raw status change (`IN_PROGRESS` / `CONFIRMED` / `REJECTED`) — superseded by the feedback endpoint. |
| `POST` | `/diagnosis/evaluations/{id}/files` | Attach a base64-encoded lab result or image. |
| `POST` | `/diagnosis/evaluations/{id}/assistant-feedback` | Record the doctor's final decision and write a feedback event. |

### Assistant interaction

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/diagnosis/assistant/messages` | Send a user turn, get an assistant reply with structured suggestions. |
| `GET`  | `/diagnosis/assistant/evaluations/{evaluationId}/thread` | Re-hydrate the full conversation, per-message structured suggestions, and the current outbreak context. |

### Request / response shapes

**`POST /diagnosis/assistant/messages`**

```jsonc
{
  "evaluationId": "uuid",            // optional but recommended
  "messages": [
    { "role": "user", "content": "Patient has fever and a rash for 3 days" }
  ],
  "patientContext": {                // optional
    "ageYears": 32,
    "sex": "female",
    "symptoms": "fever, rash"
  }
}
```

```jsonc
{
  "reply": "Cleaned assistant text (the <DIAGNOSIS_JSON> block is stripped).",
  "messageId": "uuid of the persisted assistant message",
  "contextUsed": {
    "regionName": "Región Norte",
    "outbreaks": [
      { "diseaseName": "Dengue", "caseCount": 12, "startedAt": "..." }
    ]
  },
  "suggestions": [
    {
      "id": "uuid",
      "messageId": "uuid",
      "diseaseId": "uuid or null",
      "displayName": "Dengue fever",
      "rankOrder": 1,
      "confidence": 0.62,
      "rationale": "Fever, rash, low platelets; active dengue outbreak.",
      "localityRiskLevel": "HIGH",
      "primary": true
    }
  ]
}
```

**`POST /diagnosis/evaluations/{id}/assistant-feedback`**

```jsonc
{
  "finalDecisionSource": "ASSISTANT_ACCEPTED",  // or ASSISTANT_REJECTED_DOCTOR_OVERRIDE / DOCTOR_ONLY
  "finalDiseaseId": "uuid",                     // optional
  "finalDiagnosisLabel": "Confirmed dengue",    // optional
  "doctorFeedbackNotes": "Positive NS1 antigen.",
  "acceptedAssistantMessageId": "uuid"          // optional — anchors the audit to a specific assistant turn
}
```

The response is the updated `DiagnosisEvaluationDto` (now carrying
`finalDiseaseId`, `finalDiagnosisLabel`, `finalDecisionSource`,
`doctorFeedbackNotes`).

## Data model

All persistence goes through JPA / Hibernate. Schema is regenerated on every
startup (`quarkus.hibernate-orm.schema-management.strategy=drop-and-create`) —
adding new entities is therefore a code-only change.

### `patient_evaluations` (existing, extended)

Holds the clinical work item. Fields added for the feedback loop:

| Column | Type | Purpose |
| --- | --- | --- |
| `final_disease_id` | FK → `diseases.id`, nullable | Disease the doctor confirmed, when it maps to a known one. |
| `final_diagnosis_label` | varchar(256) | Free-text confirmed diagnosis. |
| `final_decision_source` | varchar(48) | `ASSISTANT_ACCEPTED` / `ASSISTANT_REJECTED_DOCTOR_OVERRIDE` / `DOCTOR_ONLY`. |
| `doctor_feedback_notes` | text | Rationale, supporting evidence, rejection reason. |

### `diagnosis_assistant_threads`

One row per evaluation. Created lazily the first time a doctor sends a message
on that evaluation.

| Column | Type |
| --- | --- |
| `id` | UUID PK |
| `evaluation_id` | FK → `patient_evaluations.id`, unique |
| `doctor_user_id` | FK → `users.id` |
| `hospital_id` | FK → `hospitals.id`, nullable |
| `status` | varchar(16) — `OPEN` / `FINALIZED` / `ABANDONED` |
| `created_at`, `updated_at` | datetime |

### `diagnosis_assistant_messages`

One row per chat turn (system messages are not persisted; only `user` and
`assistant`).

| Column | Type |
| --- | --- |
| `id` | UUID PK |
| `thread_id` | FK → `diagnosis_assistant_threads.id` |
| `role` | varchar(16) |
| `message_text` | text — for assistant rows this is the cleaned reply, with the JSON block already stripped |
| `sequence_no` | int — monotonic per thread |
| `created_at` | datetime |

### `diagnosis_assistant_suggestions`

The structured differential parsed out of the assistant reply. Zero or more
rows per assistant message.

| Column | Type |
| --- | --- |
| `id` | UUID PK |
| `message_id` | FK → `diagnosis_assistant_messages.id` |
| `evaluation_id` | FK → `patient_evaluations.id` |
| `disease_id` | FK → `diseases.id`, nullable — populated when `displayName` matches a known disease by case-insensitive name |
| `display_name` | varchar(256) — verbatim from the LLM |
| `rank_order` | int — 1 = top suggestion |
| `confidence` | double 0..1, nullable |
| `rationale` | text, nullable |
| `locality_risk_level` | varchar(16) — `HIGH` / `MEDIUM` / `LOW` / `NONE` / null |
| `was_primary_suggestion` | bool — the LLM's `isPrimary` flag |
| `created_at` | datetime |

### `diagnosis_assistant_retrieved_cases`

Audit log of which historical evaluations were injected into the prompt for a
given assistant turn. One row per (assistant message, retrieved evaluation).

| Column | Type |
| --- | --- |
| `id` | UUID PK |
| `message_id` | FK → `diagnosis_assistant_messages.id` |
| `retrieved_evaluation_id` | FK → `patient_evaluations.id` |
| `rank_order` | int — order shown in the prompt |
| `similarity_score` | double, nullable — Jaccard overlap on tokenized symptoms |
| `created_at` | datetime |

### `diagnosis_feedback_events`

One row per doctor disposition. Even if the doctor changes their mind, prior
events are kept — the table is append-only.

| Column | Type |
| --- | --- |
| `id` | UUID PK |
| `evaluation_id` | FK → `patient_evaluations.id` |
| `thread_id` | FK → `diagnosis_assistant_threads.id`, nullable (some evaluations may never have started an assistant thread) |
| `doctor_user_id` | FK → `users.id` |
| `hospital_id` | FK → `hospitals.id`, nullable |
| `feedback_type` | varchar(48) — same vocabulary as `final_decision_source` |
| `accepted_assistant_message_id` | FK → `diagnosis_assistant_messages.id`, nullable |
| `final_disease_id` | FK → `diseases.id`, nullable |
| `final_diagnosis_label` | varchar(256), nullable |
| `feedback_notes` | text, nullable |
| `created_at` | datetime |

## Backend orchestration

### Use cases

| Class | Responsibility |
| --- | --- |
| `AskDiagnosisAssistantUseCase` | Orchestrates a single chat turn — loads the thread, retrieves historical cases, builds the prompt, calls the LLM, parses suggestions, persists everything, returns the response DTO. |
| `GetDiagnosisAssistantThreadUseCase` | Returns the full conversation plus per-message suggestions and the current `contextUsed`. |
| `RecordAssistantFeedbackUseCase` | Validates `finalDecisionSource`, writes the `final_*` fields on the evaluation, appends a `diagnosis_feedback_events` row, and resolves the optional `acceptedAssistantMessageId` and `finalDiseaseId`. |
| `HistoricalCaseRetriever` | Returns the top-N similar confirmed evaluations at the hospital. |
| `AssistantSuggestionParser` | Extracts and parses the `<DIAGNOSIS_JSON>` block. |
| `AssistantPromptBuilder` | Renders the system prompt from region + outbreaks + historical cases + patient context. |

### LLM provider abstraction

`AssistantChatGateway` is the outbound port. `LlmChatClient` implements it with
a Strategy pattern: `OpenAiChatStrategy` is primary, `GeminiChatStrategy` is the
fallback. If the primary call throws, the fallback is invoked; if both throw,
an `OpenAiException` propagates. Both strategies receive the exact same list of
`(role, content)` messages, so a provider switch never affects the persisted
conversation or the prompt structure.

### Conversation state ownership

The backend is the source of truth for the conversation. The frontend may send
the most recent user message in the request, but the use case rebuilds the full
context from `diagnosis_assistant_messages` ordered by `sequence_no`. This
means: a page reload, a provider switch, or a different device opening the same
evaluation all see the same conversation.

The frontend sends only the latest user turn in `messages[]`; older turns are
ignored on the backend in favor of the persisted thread when an `evaluationId`
is provided.

## Prompt construction

`AssistantPromptBuilder` produces a single system message that the LLM sees.
The structure is fixed:

1. Role line ("You are a clinical decision-support assistant for a doctor in
   `<region>`.") and a one-line guardrail against fabricated diagnoses.
2. **Active outbreaks** block — for each outbreak in the doctor's region:
   disease name, case count, start date, hallmark symptoms.
3. **Similar confirmed cases at this hospital in the last 12 months** block —
   for each retrieved case: age, sex initial, truncated symptoms, confirmed
   diagnosis, finalization date. Explicitly tagged as grounding context, *not*
   as a label to copy.
4. **Patient under evaluation** block — age, sex, reported symptoms.
5. Reminder to call out outbreak overlap when present.
6. **Output format instruction** — the LLM is told to append a single
   `<DIAGNOSIS_JSON>{...}</DIAGNOSIS_JSON>` block on a new line at the end of
   its reply, with up to five entries ranked by confidence (0..1) and at most
   one marked `isPrimary: true`. The block is the only structured output —
   everything before it is free text shown to the doctor.

Provider-neutrality: the instruction is plain prompt text, not OpenAI-specific
"response_format" JSON mode, so Gemini and OpenAI both receive identical
instructions.

## Retrieval (Phase 3)

`HistoricalCaseRetriever.retrieveSimilar(hospitalId, currentEvaluationId, symptoms)`:

1. **Candidate set** — confirmed evaluations from the same hospital
   (`patient.hospital_id = :hospitalId`), `status = 'CONFIRMED'`, `finalized_at`
   within the last 12 months, excluding the current evaluation. Up to 60
   candidates, ordered by `finalized_at desc`.
2. **Ranking** — symptoms are lowercased and tokenized (`[^a-z0-9]+`),
   stop-worded (English clinical filler + length ≥ 3), then ranked by Jaccard
   overlap with the current evaluation's symptoms. Ties are broken by
   `finalized_at desc`.
3. **Output** — top 5 are returned as `HistoricalCase` records (evaluation id,
   age, sex, symptoms, confirmed diagnosis name or label, similarity score) and
   are persisted as `diagnosis_assistant_retrieved_cases` rows on the assistant
   message after the LLM replies, so the retrieval is auditable.

The retriever is intentionally simple — Jaccard on tokens, no embeddings — to
keep the surface area small. Replacing it with a vector store later requires no
schema change.

## Suggestion parsing (Phase 2)

`AssistantSuggestionParser`:

1. Locates the `<DIAGNOSIS_JSON>...</DIAGNOSIS_JSON>` block via regex (case
   insensitive, multi-line).
2. Strips the block from the reply before storing it as `message_text` — the
   doctor never sees raw JSON.
3. Parses the JSON via Jackson. The expected shape is
   `{"differentialDiagnoses": [{...}]}` where each entry may carry
   `displayName`, `confidence`, `rationale`, `localityRiskLevel`, `isPrimary`.
4. Coerces values: `confidence` is clamped to `[0, 1]`; `localityRiskLevel` is
   uppercased and restricted to `HIGH` / `MEDIUM` / `LOW` / `NONE`; `rankOrder`
   is derived from array position.

If the block is missing or malformed, the parser returns the (untouched) reply
text and an empty suggestion list. The reply still reaches the doctor — only
the structured analytics data is lost for that turn. This keeps the flow
robust to model formatting drift.

Disease resolution happens in the use case, not the parser: the use case looks
up `DiseaseEntity` by case-insensitive exact name match. No fuzzy matching in
v1 — a non-match leaves `disease_id` null, which is still useful for free-text
analytics.

## Frontend

The doctor-facing screen lives at [`/app/diagnosis.tsx`](../../../../StatuScope-FrontEnd/app/diagnosis.tsx),
which delegates to [`components/views/doctor/diagnosis/index.tsx`](../../../../StatuScope-FrontEnd/components/views/doctor/diagnosis/index.tsx).

Key pieces:

- **Form column** (`PatientEvaluationForm`) — patient identity, symptoms, lab
  uploads. Hits `POST /diagnosis/evaluations` on save and `POST /diagnosis/evaluations/{id}/files`
  on upload.
- **Chat panel** — renders `chatHistory` as a list of `DiagnosisChatBubble`
  (user) / `DiagnosisResponseCard` + `AssistantSuggestionsList` (assistant).
  Each assistant turn carries its `messageId` and the array of structured
  suggestions returned by the backend.
- **`AssistantSuggestionsList`** — ranked badges with display name, optional
  primary tag, confidence percentage, locality-risk pill (color-coded), and
  rationale.
- **Feedback panel** — opens when the doctor clicks `Confirm Diagnosis` or
  `Reject Suggestion`. Captures an optional final-diagnosis label and notes
  and submits to `POST /diagnosis/evaluations/{id}/assistant-feedback`.
- **Hydration** — on mount, `GET /diagnosis/evaluations/current` fetches the
  active evaluation, then `GET /diagnosis/assistant/evaluations/{id}/thread`
  rebuilds the chat history (messages **and** suggestions) and the
  `contextUsed` block.

## Authorization

`@RequiresPrivilege("diagnosis.assist")` guards every endpoint listed above.
The privilege is granted to the `DOCTOR` role only (`HOSPITAL_ADMIN` and
`SYSTEM_ADMIN` do not have it). The `AuthorizationInterceptor` reads the
annotation at request time and inspects the resolved `CurrentUser`.

The use cases also enforce ownership: `loadManagedEvaluation` joins on
`doctor_user_id = :currentUserId`, so a doctor can never read or write another
doctor's evaluation, thread, or feedback event.

## Privacy boundary

The data model intentionally avoids storing free-text PHI in places where it
is hard to redact later:

- `diagnosis_assistant_messages.message_text` keeps the conversation, including
  whatever the doctor typed. Treat this column as PHI for retention and access
  reviews.
- The system prompt is **not** persisted. Only the structured inputs that built
  it (region, outbreaks, retrieved case ids, patient context) can be
  reconstructed from other tables.
- Historical cases are referenced by id, not snapshot, so a future de-id
  pipeline can scrub them once at the source.

## What is intentionally not built yet

These were considered and deferred. They are listed here so future work can
pick them up without re-deriving the context:

- **Per-doctor personalization** — never. Doctor identity is for audit and
  acceptance-rate analytics only.
- **Fine-tuning on confirmed cases** — out of scope. Retrieval-augmented
  prompting is the chosen mechanism for "the assistant learns from past cases".
- **Doctor-selectable rejection reasons** — the plan mentioned codes like
  "insufficient evidence" / "outbreak bias". v1 captures a free-text reason in
  `doctor_feedback_notes` only.
- **Per-suggestion confirm picker** — the feedback API accepts
  `acceptedAssistantMessageId` so the audit can be anchored to a turn, but the
  UI does not yet let the doctor pick *which* suggestion within a turn was
  accepted. This is a UI-only follow-up.
- **Embedding-based retrieval** — `HistoricalCaseRetriever` is keyword-overlap
  only. The interface is encapsulated, so a vector-store implementation can
  drop in without touching the use case or the prompt builder.
- **Analytics dashboards** — the data is in the database, but the reporting
  layer (acceptance rate per disease, hospital, region; over-prediction
  detection) is not built.

## Where to look

| Concern | File |
| --- | --- |
| Assistant orchestration | [`AskDiagnosisAssistantUseCase`](../../src/main/java/com/itesm/application/usecase/AskDiagnosisAssistantUseCase.java) |
| Thread read | [`GetDiagnosisAssistantThreadUseCase`](../../src/main/java/com/itesm/application/usecase/GetDiagnosisAssistantThreadUseCase.java) |
| Doctor feedback | [`RecordAssistantFeedbackUseCase`](../../src/main/java/com/itesm/application/usecase/RecordAssistantFeedbackUseCase.java) |
| Prompt construction | [`AssistantPromptBuilder`](../../src/main/java/com/itesm/application/usecase/AssistantPromptBuilder.java) |
| Suggestion parsing | [`AssistantSuggestionParser`](../../src/main/java/com/itesm/application/usecase/AssistantSuggestionParser.java) |
| Historical retrieval | [`HistoricalCaseRetriever`](../../src/main/java/com/itesm/application/usecase/HistoricalCaseRetriever.java) |
| Provider strategy | [`LlmChatClient`](../../src/main/java/com/itesm/infrastructure/llm/LlmChatClient.java) |
| REST surface | [`DiagnosisAssistantResource`](../../src/main/java/com/itesm/interfaces/rest/DiagnosisAssistantResource.java), [`DiagnosisEvaluationResource`](../../src/main/java/com/itesm/interfaces/rest/DiagnosisEvaluationResource.java) |
