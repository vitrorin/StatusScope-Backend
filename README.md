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

OPENAI_API_KEY=
OPENAI_MODEL=gpt-4o
GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.0-flash

STATUSSCOPE_MAIL_ENABLED=false
STATUSSCOPE_MAIL_FROM=StatuScope <zerostatuscope@gmail.com>
QUARKUS_MAILER_HOST=smtp.gmail.com
QUARKUS_MAILER_PORT=587
QUARKUS_MAILER_USERNAME=
QUARKUS_MAILER_PASSWORD=
QUARKUS_MAILER_START_TLS=REQUIRED
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

## Build And Test

```bash
./mvnw test
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

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
| `GET` | `/admin/users` | List all users when caller is `SYSTEM_ADMIN` |
| `POST` | `/admin/users` | Create users across roles and hospitals |
| `PUT` | `/admin/users/{id}` | Edit user profile, role, hospital, and status |
| `PATCH` | `/admin/users/{id}/status` | Activate, disable, or mark a user pending |
| `GET` | `/admin/hospitals` | List registered hospitals |
| `POST` | `/admin/hospitals` | Register hospital |
| `PUT` | `/admin/hospitals/{id}` | Edit hospital |
| `PATCH` | `/admin/hospitals/{id}/status` | Activate/deactivate hospital |
| `GET` | `/admin/hospitals/municipalities` | Municipality catalog for hospital registration |

### Hospital Administrator

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/admin/dashboard/summary` | Hospital operational dashboard |
| `GET` | `/admin/resources/summary` | Hospital resource snapshot |
| `GET` | `/admin/resources/departments` | Department capacity |
| `GET` | `/admin/resources/staffing` | Staffing profiles |
| `GET` | `/admin/resources/inventory` | Inventory items |
| `POST` | `/admin/resources/inventory/{itemId}/supply-requests` | Create direct supply request |
| `GET` | `/admin/recommendations` | Operational recommendations |
| `POST` | `/admin/recommendations/refresh` | Refresh recommendation models |
| `POST` | `/admin/recommendations/{id}/tasks` | Assign or reassign operational task |
| `POST` | `/admin/recommendations/{id}/notifications` | Notify contact or department by email |

### Doctor

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/doctor/dashboard/summary` | Doctor dashboard summary |
| `GET` | `/doctor/dashboard/map` | Outbreak map data |
| `GET` | `/doctor/dashboard/alerts` | Contextual outbreak alerts |
| `GET` | `/doctor/dashboard/diseases` | Disease catalog for analytics |
| `POST` | `/diagnosis-assistant/messages` | Diagnosis assistant chat |

## Project Structure

```text
src/main/java/com/itesm/
  interfaces/rest/       HTTP resources
  application/           Use cases, DTOs, security policies
  domain/                Domain models and repository ports
  infrastructure/        Firebase, mail, LLM clients, JPA entities, repositories
src/main/resources/
  import.sql             Local seed data
  db/migration/          Flyway migrations for persistent profiles
  data/                  Epidemiological seed data
```

## Notes

- Operational recommendation narratives can be LLM-assisted and store bilingual content.
- Email delivery uses Quarkus Mailer through `OperationalEmailGateway`.
- Notification delivery evidence is stored per recipient for contact and department notifications.
