# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Dev mode (live reload)
./mvnw quarkus:dev

# Run all tests (uses H2 in-memory — no MySQL required)
./mvnw test

# Run a single test class
./mvnw test -Dtest=DiagnosisAssistantResourceTest

# Build production JAR
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

Dev endpoints: `http://localhost:8080/q/health`, `/q/swagger-ui`, `/q/dev`

## Environment setup

Copy `.env.example` to `.env` and fill in:

```env
QUARKUS_DATASOURCE_USERNAME=root
QUARKUS_DATASOURCE_PASSWORD=your-password
QUARKUS_DATASOURCE_JDBC_URL=jdbc:mysql://localhost:3306/statusscope
FIREBASE_SA_PATH=src/main/resources/firebase-service-account.json
OPENAI_API_KEY=...
GEMINI_API_KEY=...
```

Place the Firebase service account JSON at `src/main/resources/firebase-service-account.json` (git-ignored).

## Database profiles

`application.properties` defines three named profiles that control schema management:

| Profile flag | When to use |
|---|---|
| *(default / dev)* | Fresh DB each run — Hibernate drops and recreates schema, loads `import.sql` |
| `-Dquarkus.profile=local-persistent` | DB already at current schema — Flyway applies only new migrations |
| `-Dquarkus.profile=local-schema-update` | One-time upgrade of an older DB |

Tests always use H2 in-memory via system properties in `pom.xml`; no profile flag needed.

## Architecture

### Layered / Clean Architecture

```
interfaces/rest/         ← JAX-RS resources (HTTP only — no business logic)
application/
  usecase/               ← One class per operation, single execute() method
  dto/                   ← Request/response shapes crossing layer boundaries
  security/              ← CurrentUser, @RequiresPrivilege, AuthenticatedUserContext
  port/out/              ← Outbound ports (e.g. AssistantChatGateway)
domain/
  models/                ← Pure Java domain objects (no framework annotations)
  repository/            ← Repository interfaces
infrastructure/
  persistence/
    entity/              ← JPA entities (all @Entity annotations live here)
    repository/          ← Panache implementations of domain repository interfaces
  mapper/                ← Entity ↔ domain model conversions
  firebase/              ← Firebase Admin SDK integration
  openai/ & gemini/      ← LLM provider HTTP clients
  llm/                   ← LlmChatClient facade + LlmChatStrategy interface
  security/              ← FirebaseAuthFilter (ContainerRequestFilter)
  bootstrap/             ← Startup beans (outbreak ingestion, demo data seeding)
```

Dependencies flow strictly inward. Domain has zero framework dependencies.

### Request security pipeline

Every request (except `/auth/register`, `/q/*`) flows through:

1. `FirebaseAuthFilter` — verifies Firebase Bearer token, loads `User` from DB, asserts `UserStatus == ACTIVE`, populates `AuthenticatedUserContext` (request-scoped CDI bean).
2. `@RequiresPrivilege("privilege.code")` CDI interceptor — reads `CurrentUser` from context and throws 403 if the privilege is absent.

Public paths bypass the filter. Frontend route guards are UX convenience; the interceptor is the real boundary.

### LLM / Diagnosis assistant

`AskDiagnosisAssistantUseCase` depends only on the application port `AssistantChatGateway`. `LlmChatClient` (infrastructure facade) selects between `OpenAiChatClient` and `GeminiChatStrategy` at runtime. To swap or add a provider, implement `LlmChatStrategy` in infrastructure — the use case is unaffected.

### Error model

All exceptions are mapped to `{ "code": "...", "message": "..." }` JSON by `ApiExceptionMapper`. Custom exceptions: `InvalidInviteException` (400), `ConflictException` (409), `NotFoundException` (404).

### Registration compensation

`RegisterUserUseCase` creates the Firebase user first (external side effect). If the DB insert fails, it deletes the Firebase user to prevent orphaned identity records.

## Testing patterns

Integration tests (`@QuarkusTest`) use H2, `MockFirebaseAuthFilter`, and `MockFirebaseUserService` to bypass real Firebase. Tests seed their own DB state via `@Inject EntityManager` in a `@BeforeEach @Transactional` method. Use `@InjectMock` for outbound ports like `AssistantChatGateway` to avoid real LLM calls.

## Key configuration flags

| Property | Default | Purpose |
|---|---|---|
| `auth.registration.public-enabled` | `true` | Enables public `/auth/register` endpoint |
| `geo.assistant-radius-km` | `75` | Radius for outbreak context injected into diagnosis prompts |
| `diagnosis.assistant.seed-demo-data` | `false` | Seeds demo evaluation data on startup |
| `statusscope.admin.recommendations.scheduler.enabled` | `true` | Enables scheduled LLM recommendation refresh |
| `statusscope.admin.recommendations.refresh-interval` | `6h` | How often recommendations are regenerated |
