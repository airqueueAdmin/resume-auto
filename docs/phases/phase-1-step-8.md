# Phase 1 Step 8 - Docker Compose Local Database

## Status

Complete.

## Objective

Provide a repeatable local PostgreSQL startup path without containerizing the Spring Boot application
yet. The application continues to run through the Maven Wrapper and connects to the Compose-published
database port.

## Scope

Included:

- Docker Compose PostgreSQL 17 service
- Environment-variable based database configuration
- PostgreSQL healthcheck with `pg_isready`
- Named volume for local persistence
- Repeatable start, status, log, and stop commands
- Local Compose verification with Flyway and Actuator

Explicitly excluded:

- Spring Boot application container
- Production deployment configuration
- AWS infrastructure
- Redis, queue, worker, or AI services
- LLM, Embedding, pgvector, RAG, Citation, Tool Calling, Agent, and Evaluation

## Design reasons

Only PostgreSQL is containerized in Phase 1. This keeps the Java process directly debuggable from the
IDE or Maven Wrapper while Docker manages the stateful external dependency. Adding an application
container now would introduce image builds, bind mounts, networking differences, and JVM container
tuning before those concerns are needed.

The Compose service uses a named volume so a normal restart does not lose local data. The default
database password is intentionally a development-only value and must be replaced outside local use.

The healthcheck asks PostgreSQL itself whether it accepts connections. A running container is not
equivalent to a ready database; the health status is the explicit readiness signal used by local
verification and future dependent services.

## Compose configuration

`docker-compose.yml` defines:

- service `postgres`
- image `postgres:17-alpine`
- configurable `POSTGRES_DB`, `POSTGRES_USER`, and `POSTGRES_PASSWORD`
- configurable host port through `POSTGRES_HOST_PORT`
- named volume `postgres_data`
- `pg_isready` healthcheck
- `restart: unless-stopped` for local convenience

The file intentionally omits the obsolete top-level `version` field and uses the current Compose
Specification style.

## Local commands

```powershell
Copy-Item .env.example .env
docker compose up -d postgres
docker compose ps
docker compose logs postgres
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The application connects to `jdbc:postgresql://localhost:5432/ai_career` by default. If port 5432 is
already used, set `POSTGRES_HOST_PORT` to another host port and set `DB_URL` to the same port before
starting Spring Boot.

Normal shutdown commands are:

```powershell
docker compose stop
docker compose down
```

`docker compose down -v` deletes the named database volume and is a deliberate data-destroying
operation, not the default cleanup command.

## Verification

The full Maven test suite passed after adding the Compose assets:

```text
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Local verification used a PostgreSQL 17 container with the same credentials and schema path as the
Compose service. The application started with the `local` profile, applied all four Flyway migrations,
and returned `UP` from `/actuator/health`.

The Compose model was also validated with `docker compose config`, and the service health was checked
with `docker compose ps` and PostgreSQL readiness output.

During verification, the default host port `5432` showed an environment-specific Docker Desktop port
forwarding authentication failure even though the Compose service was healthy. The same credentials
worked through the Compose network and through the configured host-port override `55435`; Spring Boot
then connected successfully through `127.0.0.1:55435`. The project therefore keeps the conventional
default of `5432` and documents `POSTGRES_HOST_PORT`/`DB_URL` override troubleshooting.

## Core concepts to understand

- A named volume is persistent Docker-managed storage, not an immutable backup.
- `docker compose stop` and `docker compose down` have different lifecycle effects.
- A healthcheck measures readiness; container creation alone does not guarantee database readiness.
- Host-to-container port mapping uses `localhost:host_port`, while future containers should use the
  Compose service name and container port.
- Local Compose defaults are convenient development settings, not production secret management.

## Completion criteria

- [x] PostgreSQL can be started with Docker Compose.
- [x] The database uses a named volume and readiness healthcheck.
- [x] Application configuration can connect through the published host port.
- [x] Flyway and Actuator work against the Compose-style PostgreSQL instance.
- [x] Stop versus data-removing cleanup is documented.
- [x] No application, AI, or production containerization was added prematurely.

## Next step

Phase 1 is complete. Phase 2 starts with the first direct LLM API integration.
