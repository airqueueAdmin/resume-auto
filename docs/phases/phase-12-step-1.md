# Phase 12 Step 1 - Production Compose Packaging and Oracle VM Runbook

## Status

Complete for repository preparation. Phase 12 remains in progress because the OCI VM has not yet been
provisioned and the public deployment smoke test has not run.

## Objective and scope

Prepare the current Compose application for a single Oracle Cloud Always Free VM without changing the
application's business behavior or introducing a managed service dependency.

## Implemented

- Added `application-prod.yml` with environment-backed datasource settings, a small default connection
  pool, forwarded-header handling, and non-detailed health responses.
- Added bind-address variables to the base Compose file and `docker-compose.oracle.yml` to select the
  `prod` Spring profile; `.env.oracle` sets all three host binds to loopback.
- Added `.env.oracle.example` for VM-only secrets and provider configuration.
- Added the Oracle VM ADR and Phase 12 deployment plan.
- Added the [Oracle VM deployment runbook](../runbooks/oracle-vm-deployment.md).

## Verification

- Merged Compose configuration passed `docker compose --env-file .env.oracle.example -f
  docker-compose.yml -f docker-compose.oracle.yml config --quiet`.
- The merged Oracle configuration renders `SPRING_PROFILES_ACTIVE=prod` and publishes only
  `127.0.0.1:5432`, `127.0.0.1:8080`, and `127.0.0.1:5173`.
- The full Maven suite passed: 131 tests, zero failures, zero errors, and zero skipped tests.
- The frontend production build passed.
- Backend and frontend images built successfully using the Oracle Compose override.
- The Java 25 JDK/JRE, Node 24, Nginx 1.29, and pgvector PostgreSQL 17 image manifests all include
  `linux/arm64`, matching the Oracle Ampere A1 target.
- A temporary `prod` Compose stack started with a separate project and database volume; PostgreSQL,
  backend, and frontend became healthy, backend readiness returned `UP`, and the frontend proxy
  returned HTTP 200 for readiness and the SPA root. The temporary resources were removed afterward.
- The base local Compose file remains available for local development.
- The Oracle override keeps the existing frontend same-origin `/api` proxy and prevents the database,
  backend, and frontend ports from being externally published.

## Failure encountered and root cause

The first full Maven test attempt could not start Testcontainers because Docker Desktop's Linux engine
was stopped. After Docker was restarted, pgvector PostgreSQL 17 started successfully, all 15 Flyway
migrations applied in the integration databases, and the full suite passed. The image build initially
could not read the user's Docker Desktop config from the sandbox, so the build used temporary
repository-local Docker and Buildx configuration directories; those directories were removed after the
build.

The first temporary `prod` stack check used a separate Compose project without building project-named
images, then tried the local port 5432 which was already reserved. Building with the temporary project
name and using verification-only host ports resolved both setup issues; the application then started
successfully.

## Known limitations

- An example environment file is intentionally not a deployment secret.
- OCI account creation, VM capacity, DNS, TLS issuance, and public smoke tests require external account
  state and remain in Phase 12 Step 2.
- The VM is a single failure domain and Ollama response latency is workload-dependent.

## Concepts to understand before Step 2

- Compose file merging changes runtime exposure without duplicating the application topology.
- Spring profiles separate local diagnostics from public deployment defaults.
- Loopback port binding and an HTTPS reverse proxy reduce the public attack surface but do not replace
  host patching, strong secrets, backups, or access control.
