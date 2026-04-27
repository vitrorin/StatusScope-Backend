# StatuScope — Architecture & Feature Documentation

> Last updated: April 27, 2026

---

## Table of Contents

1. [Application Overview](#1-application-overview)
2. [Roles & Privileges](#2-roles--privileges)
3. [Authentication System](#3-authentication-system)
4. [Route & View Protection](#4-route--view-protection)
5. [Frontend Architecture](#5-frontend-architecture)
6. [Backend Architecture](#6-backend-architecture)
7. [Design Patterns](#7-design-patterns)
8. [Tech Stack Summary](#8-tech-stack-summary)

---

## 1. Application Overview

**StatuScope** is a cross-platform (iOS, Android, Web) medical radar system for hospitals. It provides:

| Feature | Description |
|---------|-------------|
| **Epidemiological Radar** | Real-time geographic disease tracking with risk-zone maps, cluster pins, and trend alerts |
| **Clinical Decision Support** | AI-assisted differential diagnosis with confidence scores and recommended lab tests |
| **Hospital Resource Management** | Live tracking of beds, ICU capacity, oxygen supply, staffing, and isolation rooms |
| **Admin User Management** | Role assignment, user creation/disabling, invite-code-based registration |
| **Analytics** | Time-ranged (24h / 72h / week) and radius-based (1 km / 5 km / 10 km) disease breakdown with growth trends |
| **Recommendations** | System-generated outbreak response and resource allocation recommendations |

The frontend is built with **Expo React Native** (file-based routing via Expo Router). The backend is a **Quarkus 3 (Java 17)** REST API backed by **MySQL** and **Firebase Authentication**.

---

## 2. Roles & Privileges

### 2.1 Roles

Three roles are defined in the system (`role.code`):

| Role Code | Display Name | Scope |
|-----------|--------------|-------|
| `SYSTEM_ADMIN` | System Administrator | Full platform — all hospitals |
| `HOSPITAL_ADMIN` | Hospital Administrator | Single hospital |
| `DOCTOR` | Doctor / Clinician | Single hospital, clinical features only |

New users who register via invite code are automatically assigned the `DOCTOR` role. `HOSPITAL_ADMIN` and `SYSTEM_ADMIN` are assigned manually via the admin users panel.

### 2.2 Privileges

Roles carry a set of fine-grained privileges enforced on the backend by the `@RequiresPrivilege` interceptor:

| Privilege Code | What It Allows |
|----------------|----------------|
| `users.read` | List and view users |
| `users.manage` | Create, disable, and update users |
| `roles.manage` | Assign and manage roles |
| `hospitals.read` | View hospital records |
| `hospitals.manage` | Create and modify hospitals |

`SYSTEM_ADMIN` sees all users across all hospitals. `HOSPITAL_ADMIN` sees only users belonging to their own hospital.

### 2.3 User Status

Users also carry a `UserStatus` enum value: `ACTIVE`, `INACTIVE`, `SUSPENDED`. The auth filter rejects any request from a non-`ACTIVE` user.

---

## 3. Authentication System

### 3.1 Flow Overview

```
User enters email + password
        │
        ▼
Firebase Auth SDK (client-side)
  signInWithEmailAndPassword()
        │
        ▼
Firebase issues ID Token (JWT)
        │
        ▼
Frontend stores token in AuthContext
        │
        ▼
GET /auth/me  ← Authorization: Bearer <idToken>
        │
        ▼
Backend: FirebaseAuthFilter verifies token
  → looks up User by externalAuthId (Firebase UID)
  → checks UserStatus == ACTIVE
  → builds CurrentUser (roles + privileges)
        │
        ▼
Returns UserSummaryDto → stored in AuthContext.profile
```

### 3.2 Frontend — `AuthContext`

`contexts/AuthContext.tsx` exposes:

```typescript
interface AuthContextValue {
  user: FirebaseUser | null;       // raw Firebase user
  profile: UserProfile | null;     // roles, privileges, hospital info
  loading: boolean;
  login(email, password): Promise<void>;
  logout(): Promise<void>;
}
```

- `login()` calls `signInWithEmailAndPassword`, then fetches `/auth/me` to populate `profile`.
- `logout()` calls Firebase `signOut()` and clears state.
- On mount, `onAuthStateChanged` re-hydrates the session if a valid Firebase session already exists.

### 3.3 Frontend — `api.ts`

All API calls go through a single wrapper:

```typescript
async function api<T>(path: string, init?: RequestInit): Promise<T>
```

- Automatically calls `firebaseAuth.currentUser.getIdToken()` and injects `Authorization: Bearer <token>`.
- Sets `Content-Type: application/json`.
- On **401** → triggers auto sign-out.
- On errors → throws `ApiError` with `status` and `code` fields (e.g., `INVALID_INVITE`).

### 3.4 Firebase Configuration

Firebase is initialised in `lib/firebase.ts` using environment variables:

```
EXPO_PUBLIC_FIREBASE_API_KEY
EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN
EXPO_PUBLIC_FIREBASE_PROJECT_ID
EXPO_PUBLIC_FIREBASE_APP_ID
```

The backend uses the **Firebase Admin SDK** (`FirebaseConfig.java`) to verify tokens server-side. Firebase is treated purely as an identity provider — the authoritative user record (roles, hospital, status) lives in MySQL.

### 3.5 Registration

Registration requires an **invite code** tied to a specific hospital:

1. `POST /auth/register` with `{ fullName, email, password, inviteCode }`.
2. Backend validates the invite code (must be active and linked to a hospital).
3. Firebase user is created first.
4. DB user record is inserted and assigned the `DOCTOR` role.
5. **Compensation**: if the DB insert fails after Firebase user creation, the Firebase user is deleted to keep systems in sync.

---

## 4. Route & View Protection

### 4.1 Route Guard — `app/_layout.tsx`

The root layout wraps all screens inside `<AuthProvider>`. An `AuthGate` component runs on every navigation event:

- **Logged in + on `/login` or `/register`** → redirect to `/dashboard/{role}` (prevents back-navigation into auth screens).
- **Not logged in + on any protected screen** → redirect to `/login`.
- **During auth hydration** (`loading === true`) → render a loading state; no premature redirects.

The dashboard redirect target is derived from the user's primary role:
- `HOSPITAL_ADMIN` / `SYSTEM_ADMIN` → `/dashboard/administrator`
- `DOCTOR` → `/dashboard/doctor`

### 4.2 `RoleGate` Component

`components/auth/RoleGate.tsx` is a declarative guard used inside screens to conditionally render UI by role:

```tsx
<RoleGate roles={['HOSPITAL_ADMIN', 'SYSTEM_ADMIN']}>
  <AdminOnlySection />
</RoleGate>
```

It reads `profile.roles` from `AuthContext` and renders `null` (or a fallback) if the user doesn't match.

### 4.3 Dynamic Role-Based Dashboard — `app/dashboard/[role].tsx`

The `[role]` segment is resolved at runtime and matched against the authenticated user's role. If the segment doesn't match the user's actual role, the screen redirects to the correct dashboard. This ensures users can't manually navigate to another role's dashboard via the URL.

### 4.4 Route–Role Matrix

| Route | Allowed Roles |
|-------|---------------|
| `/login` | Public |
| `/register` | Public (invite code required) |
| `/dashboard/doctor` | `DOCTOR` |
| `/dashboard/administrator` | `HOSPITAL_ADMIN`, `SYSTEM_ADMIN` |
| `/diagnosis` | `DOCTOR` |
| `/analytics` | `DOCTOR` |
| `/admin/users` | `HOSPITAL_ADMIN`, `SYSTEM_ADMIN` |
| `/admin/analytics` | `HOSPITAL_ADMIN`, `SYSTEM_ADMIN` |
| `/admin/recommendations` | `HOSPITAL_ADMIN`, `SYSTEM_ADMIN` |
| `/admin/resources` | `HOSPITAL_ADMIN`, `SYSTEM_ADMIN` |

### 4.5 Backend Enforcement — `@RequiresPrivilege`

Every sensitive API endpoint is annotated with `@RequiresPrivilege("privilege.code")`. An `AuthorizationInterceptor` intercepts the call, reads the `CurrentUser` from `AuthenticatedUserContext`, and throws a 403 if the privilege is absent. This means route-level protection on the frontend is UX convenience; the real security boundary is the backend.

---

## 5. Frontend Architecture

### 5.1 File-Based Routing (Expo Router)

Screens live in the `app/` directory and map directly to routes:

```
app/
├── _layout.tsx          ← root layout, AuthProvider + AuthGate
├── login.tsx            ← /login
├── register.tsx         ← /register
├── modal.tsx            ← /modal
├── analytics.tsx        ← /analytics  (Doctor)
├── diagnosis.tsx        ← /diagnosis  (Doctor)
├── (tabs)/
│   ├── _layout.tsx      ← tab bar shell
│   ├── index.tsx        ← Home tab
│   └── explore.tsx      ← Dashboard tab
├── dashboard/
│   └── [role].tsx       ← /dashboard/:role
└── admin/
    ├── analytics.tsx    ← /admin/analytics
    ├── recommendations.tsx
    ├── resources.tsx
    └── users.tsx
```

Each route file is a thin shell that imports a **view component** from `components/views/`. Screens contain no business logic.

### 5.2 Component Organisation

```
components/
├── auth/           ← Login, RegisterForm, RoleGate
├── dashboard/      ← Shared dashboard widgets
├── diagnosis/      ← Diagnosis chat & form components
├── feedback/       ← Toast, error banners
├── foundation/     ← Base primitives (typography, spacing)
├── inputs/         ← Form inputs
├── layout/         ← Screen layout wrappers
├── patterns/       ← Reusable composite patterns
├── recommendations/
├── resources/
├── ui/             ← Generic UI elements (buttons, cards)
├── users/          ← User management UI
└── views/          ← Full-page view compositions
    ├── doctor/
    │   ├── dashboard/
    │   ├── diagnosis/
    │   └── analytics/
    └── admin/
        ├── dashboard/
        ├── users/
        ├── analytics/
        ├── recommendations/
        └── resources/
```

### 5.3 Key Shared Dashboard Widgets

| Component | Purpose |
|-----------|---------|
| `StatCard` | Metric tile with status badge, optional progress bar, and trend text |
| `RadarMapCard` | Geographic map with risk-zone pins, legend, and overlay panel |
| `MiniMetricCard` | Compact metric with directional trend indicator |
| `MiniBarChartCard` | Small bar chart for disease distribution |
| `DiseaseBreakdownCard` | Disease list with case counts and week-over-week change |
| `TimeFilterTabs` | 24h / 72h / Week selector |
| `RangeSelector` | Radius selector (1 km / 5 km / 10 km) |
| `ProgressMetricRow` | Horizontal resource utilisation bar |

### 5.4 Styling

The app uses **NativeWind** (Tailwind CSS for React Native) for utility-class styling, with a centralised design token file at `constants/theme.ts`. Brand primary is `#0003B8` (deep blue).

---

## 6. Backend Architecture

### 6.1 Layered / Clean Architecture

The backend follows a strict layered architecture:

```
interfaces/rest/     ← HTTP controllers (JAX-RS resources)
application/         ← Use cases, DTOs, security abstractions
domain/              ← Pure domain models, repository interfaces
infrastructure/      ← Firebase, JPA entities, repository implementations, mappers
```

Dependencies point inward: `infrastructure` → `application` → `domain`. Domain has no framework dependencies.

### 6.2 Key Use Cases

| Use Case | Responsibility |
|----------|---------------|
| `RegisterUserUseCase` | Validate invite, create Firebase + DB user, compensate on failure |
| `GetMyProfileUseCase` | Resolve CurrentUser → UserSummaryDto |
| `CreateUserByAdminUseCase` | Admin-initiated user creation with role assignment |
| `AssignRoleToUserUseCase` | Attach a role to an existing user |
| `DisableUserUseCase` | Set `UserStatus` to `INACTIVE` |
| `CreateHospitalUseCase` | Provision a new hospital with invite code |

### 6.3 Security Pipeline

```
HTTP Request
    │
    ▼
FirebaseAuthFilter (ContainerRequestFilter)
  1. Extract Bearer token from Authorization header
  2. Verify with Firebase Admin SDK
  3. Look up User by externalAuthId
  4. Assert UserStatus == ACTIVE
  5. Populate AuthenticatedUserContext (request-scoped CDI bean)
    │
    ▼
JAX-RS Resource method
    │
    ▼
@RequiresPrivilege interceptor (CDI interceptor)
  6. Read CurrentUser from AuthenticatedUserContext
  7. Assert required privilege present → 403 if absent
    │
    ▼
Use Case / Domain Logic
```

Public paths (`/auth/register`, `/q/health`, `/q/openapi`) bypass the filter entirely.

### 6.4 API Error Model

All exceptions are mapped to a consistent JSON response by `ApiExceptionMapper`:

```json
{
  "code": "INVALID_INVITE",
  "message": "The provided invite code is not valid"
}
```

Custom exception types: `InvalidInviteException` (400), `ConflictException` (409), `NotFoundException` (404).

---

## 7. Design Patterns

### 7.1 Context + Provider (React)

`AuthContext` is a standard React Context + Provider pattern. All components that need auth state call `useAuth()` instead of prop-drilling. This decouples auth logic from the component tree.

### 7.2 View / Smart-Component Split

Route files (`app/*.tsx`) are **dumb shells** — they render a single imported view component and nothing else. All composition, state management, and layout live inside `components/views/`. This keeps routes navigational and components reusable.

### 7.3 Role-Based Rendering (`RoleGate`)

A dedicated gate component isolates role-check logic. Screens don't contain `if (role === 'DOCTOR')` scattered throughout JSX; they wrap sections in `<RoleGate>` declaratively. This mirrors backend `@RequiresPrivilege` in spirit.

### 7.4 Dynamic Route Segment for Role Dashboards

`/dashboard/[role]` uses Expo Router's dynamic segment to serve different view compositions from a single route file. The segment is validated against the authenticated role and corrected if mismatched, making the URL self-healing.

### 7.5 Use Case Pattern (Backend)

Each operation is encapsulated in a dedicated `*UseCase` class with a single public `execute()` method. Resources are thin: they validate HTTP concerns, delegate to a use case, and map the result to a DTO. No business logic lives in controllers.

### 7.6 Repository Pattern (Backend)

`domain/repository/` defines interfaces (`UserRepository`, `RoleRepository`, `HospitalRepository`). Infrastructure provides JPA/Panache implementations. Domain models are pure Java; JPA annotations live only on `infrastructure/persistence/entity/` classes. Mappers convert between entity and domain model.

### 7.7 CDI Interceptor for Authorization (`@RequiresPrivilege`)

Rather than repeating privilege checks inside every use case, a custom CDI interceptor reads the annotation and enforces it at the method boundary. This is an application of the **Aspect-Oriented / Cross-Cutting Concern** pattern.

### 7.8 Compensation / Saga (Registration)

`RegisterUserUseCase` implements a simple compensating transaction: Firebase user creation happens first (external side effect), and if the subsequent DB insert fails, the Firebase user is deleted. This prevents orphaned identity records in a system that spans two stores.

### 7.9 Request-Scoped Security Context

`AuthenticatedUserContext` is a CDI `@RequestScoped` bean populated by the filter and consumed by interceptors and use cases. This avoids threading concerns and keeps security context naturally tied to the HTTP request lifecycle.

### 7.10 Design Token System (Frontend)

`constants/theme.ts` centralises colours, spacing, and typography. NativeWind utility classes reference these tokens. Dashboard widgets accept typed `status` props (`'positive' | 'danger' | 'warning' | 'neutral'`) that map to semantic colours from the token file, ensuring consistent visual language across roles.

### 7.11 API Wrapper with Automatic Token Injection

`lib/api.ts` is a thin facade over `fetch`. All token refresh, header injection, and error normalisation happen in one place. Components and use cases never call `fetch` directly. This makes the auth contract enforceable and easy to change (e.g., swapping token strategy).

---

## 8. Tech Stack Summary

| Layer | Technology |
|-------|-----------|
| **Mobile / Web** | Expo 53, React Native, Expo Router (file-based routing) |
| **Styling** | NativeWind (Tailwind CSS for RN), custom design tokens |
| **State / Auth (client)** | React Context, Firebase JS SDK v11 |
| **API Client** | Native `fetch` wrapped in `lib/api.ts` |
| **Backend Framework** | Quarkus 3.31.3, Java 17, JAX-RS |
| **ORM** | Hibernate ORM + Panache, MySQL |
| **Identity Provider** | Firebase Authentication (email/password) |
| **Backend Auth Verification** | Firebase Admin SDK 9.2.0 |
| **Dependency Injection** | Quarkus CDI (ArC) |
| **Validation** | Hibernate Validator (Bean Validation) |
| **Testing (frontend)** | Vitest + Storybook stories per view |
| **Build** | Maven Wrapper (`mvnw`), Metro bundler |
