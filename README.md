# StatusScope - Backend

REST API for the StatuScope medical radar system. Built with **Quarkus 3**, **Java 17**, **MySQL**, and **Firebase Authentication**.

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

Configure environment variables in `.env`:

```env
QUARKUS_DATASOURCE_USERNAME=root
QUARKUS_DATASOURCE_PASSWORD=your-mysql-password
QUARKUS_DATASOURCE_JDBC_URL=jdbc:mysql://localhost:3306/statusscope

FIREBASE_SA_PATH=src/main/resources/firebase-service-account.json

ADMIN_RECOMMENDATIONS_LLM_ENABLED=true
ADMIN_RECOMMENDATIONS_SCHEDULER_ENABLED=false
ADMIN_RECOMMENDATIONS_REFRESH_AT_START=false
OUTBREAK_INGESTION_IMPORT_AT_START=true

OPENAI_API_KEY=
OPENAI_MODEL=gpt-4o
GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.5-flash

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

This file is git-ignored. Never commit it.

## Run

```bash
./mvnw quarkus:dev        # Linux / macOS
mvnw.cmd quarkus:dev      # Windows
```

The API starts at `http://localhost:8080`.

By default, local development uses `drop-and-create` plus `import.sql`, so seed data is recreated on startup. Use the explicit persistent profiles only when you intentionally want local data to survive restarts.

Startup also imports versioned outbreak CSV data from `src/main/resources/data/outbreaks` when `OUTBREAK_INGESTION_IMPORT_AT_START=true`.

Useful profiles:

| Profile | Purpose |
| --- | --- |
| default | Destructive local development with `drop-and-create` and `import.sql` |
| `demo` | Explicit destructive demo profile |
| `local-persistent` | Validates an existing schema and runs Flyway migrations |
| `local-schema-update` | One-time local schema update helper for older local databases |
| `prod` | Cloud Run / Cloud SQL MySQL runtime profile; validates schema and does not load `import.sql` |

## Build And Test

```bash
./mvnw test
./mvnw -Dtest=AuthRbacResourceTest test
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

`AuthRbacResourceTest` includes the focused super-admin split check: `SYSTEM_ADMIN` has `isSystemAdmin` and no `admin.operations`, while `HOSPITAL_ADMIN` has `admin.operations` and no `isSystemAdmin`.

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

The Cloud Run runtime service account needs `Cloud SQL Client` and `Secret Manager Secret Accessor`.

Useful dev URLs:

| URL | Description |
| --- | --- |
| `http://localhost:8080/q/health` | Health check |
| `http://localhost:8080/q/swagger-ui` | Swagger UI, when enabled |
| `http://localhost:8080/q/dev` | Quarkus Dev UI |

## Roles

Seed data includes platform and hospital roles:

| Role | Scope |
| --- | --- |
| `SYSTEM_ADMIN` | Platform-wide administration across hospitals, users, and system metrics |
| `HOSPITAL_ADMIN` | Hospital-scoped operations, resources, recommendations, and users |
| `DOCTOR` | Doctor dashboard, analytics, and diagnosis workflows |

`SYSTEM_ADMIN` users are not tied to a single hospital. Hospital-scoped users require a `hospitalId`.

The RBAC seed separates platform and hospital operations: `SYSTEM_ADMIN` receives `isSystemAdmin` for global system routes, while `HOSPITAL_ADMIN` receives `admin.operations` for hospital-scoped operational routes.

## Main API Areas

### Authentication

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/auth/me` | Current authenticated profile |
| `POST` | `/auth/register` | Public doctor registration |

### System Administrator

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/system/dashboard/summary` | Global platform metrics, regional distribution, activity, and recent events |
| `GET` | `/admin/roles` | List available platform roles |
| `GET` | `/admin/users` | List all users when caller is `SYSTEM_ADMIN` |
| `POST` | `/admin/users` | Create users across roles and hospitals |
| `PUT` | `/admin/users/{id}` | Edit user profile, role, hospital, and status |
| `PATCH` | `/admin/users/{id}/status` | Activate, disable, or mark a user pending |
| `POST` | `/admin/users/{userId}/roles` | Assign a role to a user |
| `GET` | `/admin/hospitals` | List registered hospitals |
| `POST` | `/admin/hospitals` | Register hospital |
| `PUT` | `/admin/hospitals/{id}` | Edit hospital |
| `PATCH` | `/admin/hospitals/{id}/status` | Activate/deactivate hospital |
| `GET` | `/admin/hospitals/municipalities` | Municipality catalog for hospital registration |

### Hospital Administrator

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/admin/dashboard/summary` | Hospital operational dashboard |
| `GET` | `/admin/recommendations` | Operational recommendations, filterable by status, severity, and type |
| `GET` | `/admin/recommendations/{id}` | Recommendation detail with audit trail, tasks, notifications, and supply requests |
| `GET` | `/admin/recommendations/{id}/workflow-options` | Allowed workflow actions and status transitions |
| `GET` | `/admin/recommendations/{id}/targets` | Primary department, staffing, and inventory targets |
| `POST` | `/admin/recommendations/refresh` | Refresh recommendation models |
| `PATCH` | `/admin/recommendations/{id}/status` | Update recommendation status |
| `POST` | `/admin/recommendations/{id}/tasks` | Assign or reassign operational task |
| `POST` | `/admin/recommendations/{id}/notifications` | Notify contact or department by email |
| `POST` | `/admin/recommendations/{id}/supply-requests` | Create supply request from a recommendation |
| `GET` | `/admin/resources/summary` | Hospital resource snapshot |
| `GET` | `/admin/resources/departments` | Department capacity |
| `GET` | `/admin/resources/staffing` | Staffing profiles |
| `GET` | `/admin/resources/inventory` | Inventory items |
| `GET` | `/admin/resources/configuration` | Full resource configuration payload |
| `GET` | `/admin/resources/operational-roster` | Operational contacts visible from resources |
| `GET` | `/admin/resources/inventory/{itemId}/movements` | Inventory movement history |
| `POST` | `/admin/resources/inventory/{itemId}/supply-requests` | Create direct supply request |
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

## Project Structure

```text
src/main/java/com/itesm/
  interfaces/rest/       HTTP resources
  application/           Use cases, DTOs, security policies
  domain/                Domain models and repository ports
  infrastructure/        Firebase, mail, LLM clients, JPA entities, repositories
src/main/resources/
  import.sql             Local seed data
  db/migration/          SQL Flyway migrations for persistent profiles
  data/                  Epidemiological seed data
src/main/java/db/migration/
  V*.java                 Java Flyway migrations for catalog and outbreak data
tools/ingesta-datos/      Python/PowerShell outbreak ingestion pipeline
```

## Notes

- Operational recommendation narratives can be LLM-assisted and store bilingual content.
- Email delivery uses Quarkus Mailer through `OperationalEmailGateway`.
- Notification delivery evidence is stored per recipient for contact and department notifications.
