# Phase 1 - Step 2: PostgreSQL and Flyway Foundation

Status: Step complete; Phase 1 remains in progress

## Objective

Add the PostgreSQL persistence foundation without implementing User, Authentication, JWT, or domain
CRUD. The application must connect to PostgreSQL, run Flyway at startup, validate JPA mappings, and
use the same database engine in integration tests.

## Design Decisions

### PostgreSQL as the source of truth

PostgreSQL is the relational source of truth selected in Phase 0. Using it from the beginning makes
constraints, SQL behavior, transaction behavior, and future pgvector support representative of the
target architecture.

### Flyway owns schema changes

Flyway is the only schema migration owner. Hibernate runs with `ddl-auto: validate`, so Entity-to-
database mismatches fail at startup without allowing runtime schema mutation. Spring SQL script
initialization is disabled to avoid multiple schema owners.

### No artificial migration

This step intentionally contains no SQL domain migration. A no-op migration would create history
without meaningful schema change. The first real migration will create User and Authentication
tables in Step 3.

### PostgreSQL Testcontainers

Tests use an isolated PostgreSQL 17 container rather than H2. Spring Boot `@ServiceConnection`
injects the container's dynamic JDBC URL, username, and password into the test application.

## Implemented

- `spring-boot-starter-data-jpa`
- `spring-boot-starter-flyway`
- `flyway-database-postgresql`
- PostgreSQL JDBC runtime driver
- `spring-boot-testcontainers`
- Testcontainers JUnit Jupiter and PostgreSQL modules
- Local PostgreSQL environment variables
- Hikari pool configuration
- Flyway migration location and naming validation
- Flyway `clean` disabled
- PostgreSQL integration tests
- Database health exposed through Actuator
- Migration rules documented in `src/main/resources/db/migration/README.md`

## Runtime Flow

```text
DataSource
  -> Flyway validation/migration
  -> Hibernate EntityManagerFactory
  -> Spring Boot application
  -> Actuator database health
```

This ordering prevents JPA repositories from starting against an uninitialized schema.

## Verification

The following checks passed:

- PostgreSQL 17.11 Testcontainer started successfully.
- JDBC connection succeeded.
- Flyway created `public.flyway_schema_history`.
- No domain migrations were applied, as expected.
- Hibernate started with PostgreSQL dialect and `validate` mode.
- Local profile connected to PostgreSQL through an external `DB_URL`.
- `/actuator/health` returned `UP` and `components.db.status` returned `UP`.
- Maven tests: 5 executed, 0 failures, 0 errors.

## Failures and Root Causes

### Testcontainers API mismatch

Testcontainers 2 relocated the PostgreSQL container class and made it non-generic. The initial
1.x-style `PostgreSQLContainer<?>` declaration failed compilation. It was corrected to the
Testcontainers 2 API.

### Missing Flyway auto-configuration

Adding only `flyway-core` compiled but did not create a Spring-managed Flyway bean under Spring Boot
4. Spring Boot 4 uses the separate `spring-boot-starter-flyway` module, which fixed the startup
failure.

### Local port collision

Port 5432 was already associated with another PostgreSQL service, so the first local run reached a
different server and failed password authentication. Running the same container on port 55432 and
setting `DB_URL` explicitly verified the application configuration path correctly.

## Concepts to Understand

- A migration is an immutable, forward-only database change.
- `ddl-auto: validate` is different from `create`, `update`, or `create-drop`.
- Testcontainers provides an actual database, not an in-memory approximation.
- `@ServiceConnection` maps container connection details into Spring Boot test configuration.
- HikariCP manages a pool of reusable database connections.
- Health status can include dependency health, so Actuator can expose database readiness.
- Externalized configuration allows the same application artifact to use different databases.

## Known Notes

Flyway logs `No migrations found` until Step 3 adds the first domain migration. This is intentional
and documented. JDK 25 may also print Mockito dynamic-agent warnings during tests; they do not cause
test failures and will be addressed as part of test/build hardening if needed.

## Step Completion Criteria

- PostgreSQL dependency and local connection configuration exist.
- Flyway is the active schema migration mechanism.
- Hibernate cannot create or update the schema.
- Integration tests use PostgreSQL.
- Database connectivity, Flyway initialization, JPA startup, and Actuator health are verified.
- User, Authentication, JWT, and domain CRUD remain excluded.

All Step 2 criteria are satisfied.

## Next Step

Phase 1 Step 3 will implement User and Authentication, including the first meaningful Flyway
migration, password hashing, short-lived stateless Access JWTs, Refresh Session persistence, and
logout revocation.
