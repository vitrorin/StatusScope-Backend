# Plan de Implementacion: Auth (Login/Register) + RBAC para StatusScope-Backend

**Fecha:** 2026-04-20  
**Referencia base:** flujo de autenticacion y registro de demo-quarkus  
**Objetivo:** implementar en StatusScope-Backend un flujo equivalente al de demo-quarkus (registro + autenticacion por token + contexto de usuario por request), extendido con RBAC completo (roles, privilegios y tablas de relacion).

**Decision obligatoria de persistencia:** usar el mismo ORM que demo-quarkus: **Quarkus Hibernate ORM con Panache** (`quarkus-hibernate-orm-panache`) para entidades, mapeos y repositorios.

---

## 1. Objetivo funcional

Implementar un backend Quarkus que permita:


---
## 2. Estado actual y brecha

### En demo-quarkus (base existente)

- Hay `UserResource` para registro.
- Hay `RegisterUserUseCase`.
- Hay `FirebaseAuthFilter` que valida token Bearer.
## Fase 0: Preparacion del backend

- [x] Confirmar stack y build (Quarkus + Maven + Java 17).
- [x] Confirmar ORM obligatorio: `quarkus-hibernate-orm-panache` (mismo patron de demo-quarkus).
- [x] Confirmar guardrails de arquitectura: capas `interfaces -> application -> domain` y adaptadores en `infrastructure`.
- [x] Definir proveedor de identidad para token:
  - Opcion A: Firebase (igual que demo-quarkus).
  - Opcion B: JWT propio (SmallRye JWT).
- [x] Agregar dependencias necesarias en `pom.xml`:
  - `quarkus-arc`
  - `quarkus-resteasy-jsonb` o `quarkus-rest-jackson`
  - `quarkus-hibernate-orm-panache` (obligatorio)
  - `quarkus-jdbc-mysql`
  - `quarkus-hibernate-validator`
  - Proveedor de token (Firebase Admin SDK o SmallRye JWT)
  - Testing: `quarkus-junit5`, `rest-assured`, `quarkus-junit5-mockito`, H2
- Se carga `CurrentUser` en `AuthenticatedUserContext` (request-scoped).
- El modelo actual de usuario guarda `role` como `String`.

Falta modelar:

## Fase 1: Esquema RBAC y migraciones

- [x] Crear migracion inicial para `users`, `roles`, `privileges`, `user_roles`, `role_privileges`.
- [x] Sembrar datos base:
  - Roles: `ADMIN`, `DOCTOR`, `ANALYST`.
  - Privilegios base por modulo.
  - Mapear privilegios a roles en `role_privileges`.
- [x] Definir rol default de registro (`DOCTOR` o `USER`).
- Tabla de `privileges`.
- Relacion `role_privileges`.
## Fase 2: Dominio y persistencia

- [x] Crear modelos de dominio: `User`, `Role`, `Privilege`.
- [x] Extender `User` para incluir colecciones de roles/privilegios efectivos.
- [x] Crear entidades JPA equivalentes y relaciones many-to-many.
- [x] Crear repositorios de dominio e implementaciones Panache (`PanacheRepositoryBase`) con el mismo estilo de demo-quarkus.
- [x] Agregar consultas optimizadas para cargar usuario con roles/privilegios (evitar N+1).
- [x] Crear mappers dedicados (`UserMapper`, `RoleMapper`, `PrivilegeMapper`) para aislar dominio de entidades.
## 3. Arquitectura objetivo (similar + extendida)


La base `demo-quarkus` no es Clean Architecture "pura" al 100%, pero si sigue claramente una **arquitectura en capas inspirada en Clean/Hexagonal (Ports & Adapters)**:

## Fase 3: Registro y perfil de usuario

- [x] Implementar `RegisterUserUseCase`:
  - Crea usuario en proveedor externo (si aplica).
  - Persiste usuario local.
  - Asigna rol default en `user_roles`.
- [x] Implementar endpoint `POST /auth/register`.
- [x] Implementar endpoint `GET /auth/me` para validar contexto autenticado y devolver roles/privilegios.
- `domain/models` + `domain/repository`: nucleo de dominio + puertos (contratos).
- `infrastructure/persistence|security|firebase|mapper`: adaptadores externos (DB, auth provider, mapeo, filtros).
## Fase 4: Autenticacion por request (patron demo-quarkus)

- [x] Crear `AuthFilter` con `@Provider` y `@Priority(Priorities.AUTHENTICATION)`.
- [x] Excluir rutas publicas (`/auth/register`, health y docs).
- [x] Validar Bearer token y abortar con 401 cuando falle.
- [x] Resolver usuario local por identificador externo.
- [x] Cargar roles + privilegios efectivos.
- [x] Guardar `CurrentUser` en `AuthenticatedUserContext`.
- `infrastructure` implementa puertos de `domain` y se conecta a Quarkus/JPA/Firebase.

1. **Use Case / Application Service pattern**
  - Cada accion de negocio principal vive en `application/usecase/*UseCase`.
  - Ejemplo observado: `RegisterUserUseCase`, `CreateTodoUseCase`.
## Fase 5: Autorizacion RBAC por privilegios

- [x] Crear anotacion `@RequiresPrivilege`.
- [x] Crear interceptor/filtro de autorizacion que:
  - Lea privilegio requerido.
  - Consulte `currentUser.hasPrivilege(...)`.
  - Responda 403 si no autorizado.
- [x] Aplicar anotaciones en endpoints sensibles.
- [x] Agregar servicio auxiliar `AuthorizationService` para use cases.
  - Interfaces en `domain/repository`.
  - Implementaciones en `infrastructure/persistence/repository` con Panache.

3. **Data Mapper pattern**
  - Conversores explicitos `Entity <-> Domain` en `infrastructure/mapper`.
## Fase 6: Gestion de roles y privilegios

- [x] Crear endpoints admin para:
  - Asignar rol a usuario.
  - Revocar rol.
  - Listar roles y privilegios.
- [x] Proteger endpoints admin con `roles.manage`.
- [x] Agregar validaciones (rol/privilegio existente, no duplicados).
4. **Dependency Injection / Inversion of Control**
  - Ensamblado por CDI (`@Inject`, `@ApplicationScoped`, `@RequestScoped`).

5. **Context Object pattern para usuario autenticado**
  - `AuthenticatedUserContext` request-scoped para compartir el usuario actual durante la request.

6. **Filter / Intercepting Filter (Chain of Responsibility en HTTP)**
  - Filtro `@Provider` + `@Priority(Priorities.AUTHENTICATION)` para autenticar antes del endpoint.
## Fase 7: Pruebas

- [x] Unit tests:
  - `RegisterUserUseCase` (asigna rol default).
  - `AuthorizationService`.
  - `CurrentUser.hasPrivilege/hasRole`.
- [x] Integration tests:
  - `POST /auth/register` exitoso y casos invalidos.
  - Rutas protegidas: 401 sin token, 403 sin privilegio, 200 con privilegio.
  - `GET /auth/me` retorna roles/privilegios esperados.
  - Test de regresion del flujo similar a demo-quarkus.

7. **Transaction Script boundary con `@Transactional`**

8. **DTO boundary pattern**
  - DTOs de entrada/salida en `application/dto` y/o modelos de lectura dedicados.
## Fase 8: Observabilidad y hardening

- [x] Estandarizar errores 401/403 con payload JSON.
- [x] Agregar auditoria minima:
  - userId
  - endpoint
  - decision (ALLOW/DENY)
- [x] Validar CORS y configuraciones de entorno (`dev`, `test`, `prod`).
- [x] Revisar que no se expongan tokens/claims sensibles en logs.
### Patron de seguridad

## 8. Criterios de aceptacion

- [x] Un usuario registrado obtiene rol default automaticamente.
- [x] Toda ruta privada requiere token valido (401 si no).
- [x] Toda ruta RBAC requiere privilegio correcto (403 si no).
- [x] `GET /auth/me` devuelve perfil, roles y privilegios efectivos.
- [x] Existen pruebas automatizadas de 401/403/200.
- [x] El flujo de autenticacion conserva el patron de demo-quarkus (filtro + contexto request-scoped).
   - email
   - fullName
   - roles
   - privileges
6. Se guarda en `AuthenticatedUserContext`.
7. Cada endpoint/use case consulta contexto y aplica autorizacion RBAC.

### Enfoque de autorizacion

- Autorizacion declarativa por privilegio: anotacion tipo `@RequiresPrivilege("alerts.read")`.
- Implementacion via interceptor/filtro JAX-RS.
- Fallback: helper en casos de uso criticos (`authorizationService.assertHas(...)`).

---

## 4. Modelo de datos RBAC

## Tablas requeridas

- `users`
- `roles`
- `privileges`
- `user_roles`
- `role_privileges`

## DDL sugerido (referencial)

```sql
create table users (
  id binary(16) primary key,
  full_name varchar(255) not null,
  email varchar(255) not null unique,
  active boolean not null default true,
  firebase_uuid varchar(128) unique,
  created_at timestamp not null,
  updated_at timestamp not null
);

create table roles (
  id binary(16) primary key,
  code varchar(64) not null unique,
  name varchar(128) not null,
  description varchar(255),
  created_at timestamp not null,
  updated_at timestamp not null
);

create table privileges (
  id binary(16) primary key,
  code varchar(128) not null unique,
  description varchar(255),
  created_at timestamp not null,
  updated_at timestamp not null
);

create table user_roles (
  user_id binary(16) not null,
  role_id binary(16) not null,
  primary key (user_id, role_id),
  constraint fk_user_roles_user foreign key (user_id) references users(id),
  constraint fk_user_roles_role foreign key (role_id) references roles(id)
);

create table role_privileges (
  role_id binary(16) not null,
  privilege_id binary(16) not null,
  primary key (role_id, privilege_id),
  constraint fk_role_priv_role foreign key (role_id) references roles(id),
  constraint fk_role_priv_priv foreign key (privilege_id) references privileges(id)
);
```

## Convencion de privilegios

Formato recomendado: `modulo.accion`.

Ejemplos:

- `alerts.read`
- `alerts.manage`
- `users.read`
- `users.manage`
- `roles.manage`
- `diagnosis.create`

---

## 5. Estructura de codigo objetivo

Estructura alineada al estilo de demo-quarkus:

```text
src/main/java/com/itesm/
  application/
    dto/
      RegisterUserDto.java
      AssignRoleDto.java
    security/
      CurrentUser.java
      AuthenticatedUserContext.java
      RequiresPrivilege.java
    usecase/
      RegisterUserUseCase.java
      GetMyProfileUseCase.java
      AssignRoleToUserUseCase.java
  domain/
    models/
      User.java
      Role.java
      Privilege.java
    repository/
      UserRepository.java
      RoleRepository.java
      PrivilegeRepository.java
  infrastructure/
    persistence/
      entity/
        UserEntity.java
        RoleEntity.java
        PrivilegeEntity.java
      repository/
        UserRepositoryImpl.java
        RoleRepositoryImpl.java
        PrivilegeRepositoryImpl.java
      mapper/
        UserMapper.java
        RoleMapper.java
        PrivilegeMapper.java
    security/
      AuthFilter.java
      AuthorizationInterceptor.java
      TokenVerifier.java
  interfaces/
    rest/
      AuthResource.java
      UserResource.java
      AdminRoleResource.java
```

## Reglas de implementacion ORM (alineado a demo-quarkus)

- No usar otro ORM ni capa alternativa de acceso a datos (por ejemplo, Spring Data JPA, jOOQ o MyBatis).
- Implementar repositorios como interfaces de dominio + implementaciones `PanacheRepositoryBase<Entidad, UUID>` en infraestructura.
- Mantener entidades JPA en `infrastructure/persistence/entity` y mappers dedicados en `infrastructure/mapper`.
- Mantener relaciones RBAC con anotaciones JPA (`@ManyToMany`, tablas puente `user_roles` y `role_privileges`).
- Mantener la logica de negocio fuera de entidades; las entidades solo modelan persistencia.
- Mantener la regla de dependencia Clean/Hexagonal inspirada: dominio no depende de infraestructura.
- Prohibido acceder a repositorios concretos de infraestructura desde `interfaces/rest`.

---

## 6. Plan por fases (checklist ejecutable)

## Fase 0: Preparacion del backend

- [ ] Confirmar stack y build (Quarkus + Maven + Java 17).
- [ ] Confirmar ORM obligatorio: `quarkus-hibernate-orm-panache` (mismo patron de demo-quarkus).
- [ ] Confirmar guardrails de arquitectura: capas `interfaces -> application -> domain` y adaptadores en `infrastructure`.
- [ ] Definir proveedor de identidad para token:
  - Opcion A: Firebase (igual que demo-quarkus).
  - Opcion B: JWT propio (SmallRye JWT).
- [ ] Agregar dependencias necesarias en `pom.xml`:
  - `quarkus-arc`
  - `quarkus-resteasy-jsonb` o `quarkus-rest-jackson`
  - `quarkus-hibernate-orm-panache` (obligatorio)
  - `quarkus-jdbc-mysql`
  - `quarkus-hibernate-validator`
  - Proveedor de token (Firebase Admin SDK o SmallRye JWT)
  - Testing: `quarkus-junit5`, `rest-assured`, `quarkus-junit5-mockito`, H2

## Fase 1: Esquema RBAC y migraciones

- [ ] Crear migracion inicial para `users`, `roles`, `privileges`, `user_roles`, `role_privileges`.
- [ ] Sembrar datos base:
  - Roles: `ADMIN`, `DOCTOR`, `ANALYST`.
  - Privilegios base por modulo.
  - Mapear privilegios a roles en `role_privileges`.
- [ ] Definir rol default de registro (`DOCTOR` o `USER`).

## Fase 2: Dominio y persistencia

- [ ] Crear modelos de dominio: `User`, `Role`, `Privilege`.
- [ ] Extender `User` para incluir colecciones de roles/privilegios efectivos.
- [ ] Crear entidades JPA equivalentes y relaciones many-to-many.
- [ ] Crear repositorios de dominio e implementaciones Panache (`PanacheRepositoryBase`) con el mismo estilo de demo-quarkus.
- [ ] Agregar consultas optimizadas para cargar usuario con roles/privilegios (evitar N+1).
- [ ] Crear mappers dedicados (`UserMapper`, `RoleMapper`, `PrivilegeMapper`) para aislar dominio de entidades.

## Fase 3: Registro y perfil de usuario

- [ ] Implementar `RegisterUserUseCase`:
  - Crea usuario en proveedor externo (si aplica).
  - Persiste usuario local.
  - Asigna rol default en `user_roles`.
- [ ] Implementar endpoint `POST /auth/register`.
- [ ] Implementar endpoint `GET /auth/me` para validar contexto autenticado y devolver roles/privilegios.

## Fase 4: Autenticacion por request (patron demo-quarkus)

- [ ] Crear `AuthFilter` con `@Provider` y `@Priority(Priorities.AUTHENTICATION)`.
- [ ] Excluir rutas publicas (`/auth/register`, health y docs).
- [ ] Validar Bearer token y abortar con 401 cuando falle.
- [ ] Resolver usuario local por identificador externo.
- [ ] Cargar roles + privilegios efectivos.
- [ ] Guardar `CurrentUser` en `AuthenticatedUserContext`.

## Fase 5: Autorizacion RBAC por privilegios

- [ ] Crear anotacion `@RequiresPrivilege`.
- [ ] Crear interceptor/filtro de autorizacion que:
  - Lea privilegio requerido.
  - Consulte `currentUser.hasPrivilege(...)`.
  - Responda 403 si no autorizado.
- [ ] Aplicar anotaciones en endpoints sensibles.
- [ ] Agregar servicio auxiliar `AuthorizationService` para use cases.

## Fase 6: Gestion de roles y privilegios

- [ ] Crear endpoints admin para:
  - Asignar rol a usuario.
  - Revocar rol.
  - Listar roles y privilegios.
- [ ] Proteger endpoints admin con `roles.manage`.
- [ ] Agregar validaciones (rol/privilegio existente, no duplicados).

## Fase 7: Pruebas

- [ ] Unit tests:
  - `RegisterUserUseCase` (asigna rol default).
  - `AuthorizationService`.
  - `CurrentUser.hasPrivilege/hasRole`.
- [ ] Integration tests:
  - `POST /auth/register` exitoso y casos invalidos.
  - Rutas protegidas: 401 sin token, 403 sin privilegio, 200 con privilegio.
  - `GET /auth/me` retorna roles/privilegios esperados.
- [ ] Test de regresion del flujo similar a demo-quarkus.

## Fase 8: Observabilidad y hardening

- [ ] Estandarizar errores 401/403 con payload JSON.
- [ ] Agregar auditoria minima:
  - userId
  - endpoint
  - decision (ALLOW/DENY)
- [ ] Validar CORS y configuraciones de entorno (`dev`, `test`, `prod`).
- [ ] Revisar que no se expongan tokens/claims sensibles en logs.

---

## 7. Historias tecnicas sugeridas

- HT-01: Migraciones RBAC base.
- HT-02: Modelos y repositorios RBAC.
- HT-03: Registro con asignacion de rol default.
- HT-04: AuthFilter + contexto de usuario con privilegios.
- HT-05: Autorizacion por anotaciones de privilegio.
- HT-06: Endpoints admin de roles.
- HT-07: Suite de pruebas de seguridad.

---

## 8. Criterios de aceptacion

- [ ] Un usuario registrado obtiene rol default automaticamente.
- [ ] Toda ruta privada requiere token valido (401 si no).
- [ ] Toda ruta RBAC requiere privilegio correcto (403 si no).
- [ ] `GET /auth/me` devuelve perfil, roles y privilegios efectivos.
- [ ] Existen pruebas automatizadas de 401/403/200.
- [ ] El flujo de autenticacion conserva el patron de demo-quarkus (filtro + contexto request-scoped).

---

## 9. Riesgos y decisiones abiertas

- Proveedor de identidad: Firebase vs JWT propio.
- Granularidad inicial de privilegios: minima vs detallada por modulo.
- Estrategia de migraciones: SQL manual vs Flyway/Liquibase.
- Politica de roles multiples por usuario (se recomienda habilitar desde inicio).

---

## 10. Entregables

- Documento de diseno RBAC (tablas y permisos).
- Migraciones SQL.
- Endpoints de auth + admin roles.
- Filtro de autenticacion y middleware/interceptor de autorizacion.
- Tests unitarios e integracion para escenarios de seguridad.
- Guia de uso para frontend (headers, rutas publicas/privadas, errores esperados).

---

## 11. Secuencia recomendada de implementacion

1. Migraciones + seed RBAC.
2. Entidades/repositorios.
3. Registro + rol default.
4. AuthFilter + contexto con privilegios.
5. Autorizacion por privilegios.
6. Endpoints admin de roles.
7. Pruebas y hardening.
