# StatusScope - Backend

REST API for the StatuScope medical radar system. Built with Quarkus 3, Java 17, MySQL, Flyway, Firebase Authentication, LLM integrations, and Cloud Run-ready configuration.

## Current Stack

| Area | Technology |
| --- | --- |
| Runtime | Java 17, Quarkus `3.31.3` |
| HTTP | Quarkus REST Jackson |
| Persistence | Hibernate ORM with Panache, MySQL 8 |
| Migrations | Flyway SQL and Java migrations |
| Auth | Firebase Admin SDK `9.2.0` |
| AI | OpenAI and Gemini behind application-owned ports/adapters |
| Email | Quarkus Mailer via `OperationalEmailGateway` |
| Tests | JUnit, Rest Assured, Mockito, H2 test datasource |
| Deployment | GitHub Actions, Cloud Run, Cloud SQL, Secret Manager |

## Prerequisites

- Java 17 JDK
- MySQL 8
- A Firebase project with Authentication enabled
- A Firebase service account JSON key

Maven does not need to be installed globally. The project includes the Maven Wrapper.

## Setup

```bash
git clone <repo-url>
cd StatusScope-Backend
```

Create the local database:

```sql
CREATE DATABASE statusscope CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Create `.env` from `.env.example`:

```env
QUARKUS_DATASOURCE_USERNAME=root
QUARKUS_DATASOURCE_PASSWORD=your-mysql-password
QUARKUS_DATASOURCE_JDBC_URL=jdbc:mysql://localhost:3306/statusscope

FIREBASE_SA_PATH=src/main/resources/firebase-service-account.json

OUTBREAK_INGESTION_IMPORT_AT_START=true

ADMIN_RECOMMENDATIONS_LLM_ENABLED=true
ADMIN_RECOMMENDATIONS_SCHEDULER_ENABLED=false
ADMIN_RECOMMENDATIONS_REFRESH_AT_START=false
ADMIN_RECOMMENDATIONS_REFRESH_INTERVAL=6h

OPENAI_API_KEY=
OPENAI_MODEL=gpt-4o
OPENAI_TEMPERATURE=0.2
OPENAI_TIMEOUT_SECONDS=30

GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.5-flash
GEMINI_TEMPERATURE=0.2

STATUSSCOPE_MAIL_ENABLED=false
STATUSSCOPE_MAIL_FROM=StatuScope <no-reply@statusscope.local>
QUARKUS_MAILER_HOST=localhost
QUARKUS_MAILER_PORT=1025
QUARKUS_MAILER_USERNAME=
QUARKUS_MAILER_PASSWORD=
QUARKUS_MAILER_START_TLS=OPTIONAL
QUARKUS_MAILER_MOCK=false
```

Place the Firebase service account at:

```text
src/main/resources/firebase-service-account.json
```

This file is git-ignored. Never commit Firebase service-account JSON files.

## Run

```bash
./mvnw quarkus:dev        # Linux / macOS
mvnw.cmd quarkus:dev      # Windows
```

The API starts at `http://localhost:8080`.

By default, local development uses `drop-and-create` plus `import.sql`, so seed data is recreated on startup. Startup can also import versioned outbreak CSV data from `src/main/resources/data/outbreaks` when `OUTBREAK_INGESTION_IMPORT_AT_START=true`.

Useful profiles:

| Profile | Purpose |
| --- | --- |
| default | Destructive local development with `drop-and-create` and `import.sql` |
| `demo` | Explicit destructive demo profile |
| `local-persistent` | Validates an existing schema and runs Flyway migrations |
| `local-schema-update` | One-time local schema update helper for older local databases |
| `prod` | Cloud Run / Cloud SQL runtime profile; validates schema and skips `import.sql` |
| `test` | H2 in-memory profile used by automated tests |

## Build And Test

```bash
./mvnw test
./mvnw -Dtest=AuthRbacResourceTest test
./mvnw -DskipTests compile
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

Recently verified:

- `.\mvnw.cmd -DskipTests compile` passes with `BUILD SUCCESS`.

Focused test areas include:

- Auth and RBAC: `AuthRbacResourceTest`, `RegisterUserUseCaseTest`, security unit tests.
- Doctor dashboard and epidemiology: `DoctorDashboardResourceTest`, `GetDoctorDashboardSummaryUseCaseTest`.
- Diagnosis assistant: assistant prompt/parser/use-case tests and REST tests.
- Operational admin workflows: `AdminOperationalResourceTest`, `OperationalRecommendationDedupeServiceTest`.

## Architecture

The backend follows a layered architecture:

```text
src/main/java/com/itesm/interfaces/rest/       JAX-RS resources and exception mapping
src/main/java/com/itesm/application/           Use cases, DTOs, security contracts, outbound ports
src/main/java/com/itesm/domain/                Domain models and repository interfaces
src/main/java/com/itesm/infrastructure/        Firebase, LLM, mail, persistence entities/repositories, mappers
src/main/resources/db/migration/               SQL Flyway migrations
src/main/java/db/migration/                    Java Flyway migrations
tools/ingesta-datos/                           Epidemiological ingestion pipeline
```

Resources are thin and delegate to application use cases. Repository interfaces live in `domain/repository`; Panache implementations live in `infrastructure/persistence/repository`.

## Security Model

Firebase is the identity provider; MySQL is the source of truth for application users, roles, privileges, status, and hospital scope.

Request flow:

```text
HTTP request
  -> FirebaseAuthFilter validates Bearer token
  -> User is loaded by Firebase UID
  -> User status must be ACTIVE
  -> AuthenticatedUserContext is populated
  -> @RequiresPrivilege is enforced by AuthorizationInterceptor
  -> Resource delegates to use case
```

Important privileges:

| Privilege | Purpose |
| --- | --- |
| `isSystemAdmin` | Platform-wide system administration |
| `admin.operations` | Hospital-scoped administrator operations |
| `users.read` | Read users |
| `users.manage` | Create/update/status-change users |
| `roles.manage` | Assign roles and list role catalog |
| `outbreaks.read` | Doctor epidemiological dashboard and analytics |
| `diagnosis.assist` | Diagnosis assistant and evaluation workflows |

Important role split:

- `SYSTEM_ADMIN` uses `isSystemAdmin` and is not tied to one hospital.
- `HOSPITAL_ADMIN` uses `admin.operations` and is scoped to one hospital.
- `DOCTOR` uses doctor-facing privileges such as outbreak reading and diagnosis assistance.

## Main API Areas

### Authentication

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/auth/me` | Current authenticated profile |
| `POST` | `/auth/register` | Public doctor registration with invite code |

### System Administrator

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/system/dashboard/summary` | Global platform metrics, regional distribution, activity, hospital status, and recent events |
| `GET` | `/admin/roles` | List available platform roles |
| `GET` | `/admin/users` | List users; system admin sees all hospitals |
| `POST` | `/admin/users` | Create users across roles and hospitals |
| `PUT` | `/admin/users/{id}` | Edit user profile, role, hospital, and status |
| `PATCH` | `/admin/users/{id}/status` | Activate, disable, or mark a user pending |
| `POST` | `/admin/users/{userId}/roles` | Assign a role to a user |
| `GET` | `/admin/hospitals` | List registered hospitals |
| `POST` | `/admin/hospitals` | Register hospital |
| `PUT` | `/admin/hospitals/{id}` | Edit hospital |
| `PATCH` | `/admin/hospitals/{id}/status` | Activate/deactivate hospital |
| `GET` | `/admin/hospitals/municipalities` | Municipality catalog for hospital registration |

### Hospital Administrator - Operations

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/admin/dashboard/summary` | Hospital operational dashboard |
| `GET` | `/admin/recommendations` | Operational recommendations filtered by status, severity, and type |
| `GET` | `/admin/recommendations/{id}` | Recommendation detail with audit trail, tasks, notifications, and supply requests |
| `GET` | `/admin/recommendations/{id}/workflow-options` | Allowed status transitions and workflow actions |
| `GET` | `/admin/recommendations/{id}/targets` | Department, staffing, inventory, and contact targets |
| `POST` | `/admin/recommendations/refresh` | Refresh recommendation models |
| `PATCH` | `/admin/recommendations/{id}/status` | Update recommendation status |
| `POST` | `/admin/recommendations/{id}/tasks` | Assign or reassign operational task |
| `POST` | `/admin/recommendations/{id}/notifications` | Notify contact or department by email |
| `POST` | `/admin/recommendations/{id}/supply-requests` | Create supply request from a recommendation |
| `GET` | `/admin/resources/summary` | Hospital resource snapshot |
| `GET` | `/admin/resources/departments` | Department capacity rows |
| `GET` | `/admin/resources/staffing` | Staffing profiles |
| `GET` | `/admin/resources/inventory` | Inventory items |
| `GET` | `/admin/resources/configuration` | Full resource configuration payload |
| `GET` | `/admin/resources/operational-roster` | Operational contacts visible from resources |
| `GET` | `/admin/resources/inventory/{itemId}/movements` | Inventory movement history |
| `POST` | `/admin/resources/inventory/{itemId}/supply-requests` | Create direct inventory supply request |
| `PUT` | `/admin/resources/summary` | Update resource snapshot |
| `POST` | `/admin/resources/departments` | Create department resource row |
| `PUT` | `/admin/resources/departments/{departmentId}` | Update department resource row |
| `DELETE` | `/admin/resources/departments/{departmentId}` | Delete department resource row |
| `POST` | `/admin/resources/staffing` | Create staffing profile |
| `PUT` | `/admin/resources/staffing/{profileId}` | Update staffing profile |
| `DELETE` | `/admin/resources/staffing/{profileId}` | Delete staffing profile |
| `POST` | `/admin/resources/inventory` | Create inventory item |
| `PUT` | `/admin/resources/inventory/{itemId}` | Update inventory item |
| `DELETE` | `/admin/resources/inventory/{itemId}` | Delete inventory item |
| `GET` | `/admin/operational-contacts` | List operational contacts |
| `POST` | `/admin/operational-contacts` | Create operational contact |
| `PUT` | `/admin/operational-contacts/{contactId}` | Update operational contact |
| `PATCH` | `/admin/operational-contacts/{contactId}/status` | Activate or deactivate operational contact |
| `GET` | `/admin/operational-groups` | List operational groups |

### Hospital Administrator - Epidemiology

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/admin/epidemiology/summary` | Hospital-scoped epidemiological dashboard summary |
| `GET` | `/admin/epidemiology/metrics` | Hospital-scoped epidemiological KPI cards |
| `GET` | `/admin/epidemiology/map` | Local outbreak map |
| `GET` | `/admin/epidemiology/map/states` | State-level outbreak map |
| `GET` | `/admin/epidemiology/map/states/{stateId}/outbreaks` | Outbreak map for one state |
| `GET` | `/admin/epidemiology/diseases` | Disease catalog |
| `GET` | `/admin/epidemiology/alerts` | Epidemiological alerts |
| `GET` | `/admin/epidemiology/disease-breakdown/local` | Local disease breakdown |
| `GET` | `/admin/epidemiology/disease-breakdown/state` | State disease breakdown |
| `GET` | `/admin/epidemiology/reports/{scope}` | Local, state, or combined report |
| `GET` | `/admin/epidemiology/reports/states/{stateId}` | Report for one state |

### Doctor

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/doctor/dashboard/summary` | Doctor dashboard summary |
| `GET` | `/doctor/dashboard/metrics` | Doctor dashboard KPI metrics |
| `GET` | `/doctor/dashboard/map` | Outbreak map data |
| `GET` | `/doctor/dashboard/map/states` | State-level outbreak map |
| `GET` | `/doctor/dashboard/map/states/{stateId}/outbreaks` | Outbreak map for one state |
| `GET` | `/doctor/dashboard/alerts` | Contextual outbreak alerts |
| `GET` | `/doctor/dashboard/diseases` | Disease catalog for analytics |
| `GET` | `/doctor/dashboard/disease-breakdown/local` | Local disease breakdown |
| `GET` | `/doctor/dashboard/disease-breakdown/state` | State disease breakdown |
| `GET` | `/doctor/dashboard/reports/{scope}` | Local, state, or combined report |
| `GET` | `/doctor/dashboard/reports/states/{stateId}` | Report for one state |
| `GET` | `/diagnosis/diseases` | Diagnosis disease options |
| `POST` | `/diagnosis/evaluations` | Create diagnosis evaluation |
| `GET` | `/diagnosis/evaluations/current` | Current diagnosis evaluation |
| `GET` | `/diagnosis/evaluations/{id}` | Diagnosis evaluation detail |
| `PUT` | `/diagnosis/evaluations/{id}` | Update diagnosis evaluation |
| `POST` | `/diagnosis/evaluations/{id}/status` | Update evaluation status |
| `POST` | `/diagnosis/evaluations/{id}/files` | Attach evaluation file metadata |
| `POST` | `/diagnosis/evaluations/{id}/assistant-feedback` | Record assistant feedback |
| `POST` | `/diagnosis/assistant/messages` | Diagnosis assistant chat |
| `POST` | `/diagnosis/assistant/translations` | Translate assistant messages |
| `GET` | `/diagnosis/assistant/evaluations/{evaluationId}/thread` | Restore assistant thread |

## Data And Migrations

Important locations:

```text
src/main/resources/import.sql
src/main/resources/db/migration/
src/main/java/db/migration/
src/main/resources/data/outbreaks/
scripts/mysql/
tools/ingesta-datos/
```

The database documentation covers:

| Document | Purpose |
| --- | --- |
| `docs/database/05-modelo-logico.md` | Logical data model |
| `docs/database/06-modelo-fisico.md` | Physical MySQL model |
| `docs/database/08-diccionario-datos.md` | Data dictionary |
| `docs/database/10-justificacion-sp-funciones-triggers.md` | Stored procedures, functions, and triggers |
| `tools/ingesta-datos/README.md` | Epidemiological ingestion pipeline |
| `scripts/mysql/faker/README-faker.md` | Synthetic data and ETL notes |

## Integrations

### Firebase

- Frontend signs users in with Firebase Auth.
- Backend verifies Firebase ID tokens using Firebase Admin SDK.
- `RegisterUserUseCase` creates Firebase users and compensates by deleting the Firebase user if DB persistence fails.

### AI Providers

Diagnosis and recommendation narratives use application-owned ports and provider adapters:

- `application/port/out/AssistantChatGateway`
- `infrastructure/openai/OpenAiChatClient`
- `infrastructure/gemini/GeminiChatStrategy`
- `infrastructure/llm/LlmChatClient`

This keeps use cases independent from provider-specific request/response DTOs.

### Email

Operational notifications use:

- `application/port/out/OperationalEmailGateway`
- `infrastructure/mail/SmtpOperationalEmailGateway`

Delivery can be disabled locally with `STATUSSCOPE_MAIL_ENABLED=false`.

## Deploy To Cloud Run

The backend deploy pipeline lives in `.github/workflows/deploy-dev.yml` and runs on pushes to `develop`.

Required GitHub secrets:

| Secret | Purpose |
| --- | --- |
| `GCP_PROJECT_ID` | Google Cloud project ID |
| `GCP_SA_KEY` | Deploy service account JSON |
| `GCP_RUNTIME_SERVICE_ACCOUNT` | Cloud Run runtime service account email |
| `CLOUD_SQL_INSTANCE` | Cloud SQL connection name, for example `project:us-central1:statusscope-dev` |

Allowed frontend origins for CORS are configured in `.github/workflows/deploy-dev.yml` as `FRONTEND_ORIGINS` and passed to the runtime as `QUARKUS_HTTP_CORS_ORIGINS`.

Required Secret Manager secrets:

| Secret | Runtime variable |
| --- | --- |
| `statusscope-backend-db-username` | `QUARKUS_DATASOURCE_USERNAME` |
| `statusscope-backend-db-password` | `QUARKUS_DATASOURCE_PASSWORD` |
| `statusscope-backend-cloudsql-jdbc-url` | `QUARKUS_DATASOURCE_JDBC_URL` |
| `statusscope-backend-firebase-service-account` | mounted at `/secrets/firebase/firebase-service-account.json` |

The Cloud SQL JDBC URL secret must use the Cloud SQL socket factory format:

```text
jdbc:mysql://google/statusscope?cloudSqlInstance=<project>:<region>:<instance>&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false
```

The Cloud Run runtime service account needs Cloud SQL Client and Secret Manager Secret Accessor.

Useful dev URLs:

| URL | Description |
| --- | --- |
| `http://localhost:8080/q/health` | Health check |
| `http://localhost:8080/q/swagger-ui` | Swagger UI, when enabled |
| `http://localhost:8080/q/dev` | Quarkus Dev UI |

## Documentation Notes

- `README.md` is the operational source of truth for setup, roles, endpoints, and deployment.
- `ARCHITECTURE.md` contains useful architectural background, but some sections are older than the current system-admin, epidemiology, and operational modules.
- Documents under `docs/plans` are historical implementation plans. Use them for context, not as final-state documentation without verification.
