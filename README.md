# StatusScope — Backend

REST API for the StatuScope medical radar system. Built with **Quarkus 3 (Java 17)**, **MySQL**, and **Firebase Authentication**.

## Prerequisites

- [Java 17](https://adoptium.net/) (JDK)
- [MySQL 8](https://dev.mysql.com/downloads/mysql/) running locally or remotely
- A [Firebase](https://firebase.google.com/) project with **Authentication** enabled (email/password)
- A Firebase **service account** JSON key (downloaded from Firebase console)

> Maven is not required globally — the project includes the Maven Wrapper (`mvnw`).

## 1. Clone the repository

```bash
git clone <repo-url>
cd StatusScope-Backend
```

## 2. Create the MySQL database

```sql
CREATE DATABASE statusscope CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 3. Configure environment variables

```bash
cp .env.example .env
```

Edit `.env` with your values:

```env
# Database
QUARKUS_DATASOURCE_USERNAME=root
QUARKUS_DATASOURCE_PASSWORD=your-mysql-password
QUARKUS_DATASOURCE_JDBC_URL=jdbc:mysql://localhost:3306/statusscope

# Firebase service account (path to the downloaded JSON key file)
FIREBASE_SA_PATH=src/main/resources/firebase-service-account.json
```

## 4. Add the Firebase service account key

Download your service account JSON from **Firebase console → Project Settings → Service accounts → Generate new private key** and place it at:

```
src/main/resources/firebase-service-account.json
```

> This file is git-ignored. Never commit it.

## 5. Run in development mode

```bash
./mvnw quarkus:dev        # Linux / macOS
mvnw.cmd quarkus:dev      # Windows
```

The API starts at **http://localhost:8080**. Quarkus Dev Mode enables live reload on code changes.

### Useful dev endpoints

| URL | Description |
|-----|-------------|
| `http://localhost:8080/q/health` | Health check |
| `http://localhost:8080/q/swagger-ui` | Swagger UI (if OpenAPI extension is enabled) |
| `http://localhost:8080/q/dev` | Quarkus Dev UI |

## 6. Run tests

```bash
./mvnw test               # Linux / macOS
mvnw.cmd test             # Windows
```

## 7. Build a production JAR

```bash
./mvnw package -DskipTests
java -jar target/quarkus-app/quarkus-run.jar
```

## Project structure

```
src/main/java/com/itesm/statusscope/
├── interfaces/rest/      ← JAX-RS HTTP controllers
├── application/          ← Use cases, DTOs, security abstractions
├── domain/               ← Pure domain models, repository interfaces
└── infrastructure/       ← Firebase, JPA entities, repository implementations
```

## Environment variables reference

| Variable | Description |
|----------|-------------|
| `QUARKUS_DATASOURCE_USERNAME` | MySQL username |
| `QUARKUS_DATASOURCE_PASSWORD` | MySQL password |
| `QUARKUS_DATASOURCE_JDBC_URL` | JDBC connection URL |
| `FIREBASE_SA_PATH` | Path to the Firebase service account JSON file |