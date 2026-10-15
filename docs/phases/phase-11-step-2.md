# Phase 11 Step 2 - Automated Delivery Quality Gate

## Status

Implemented locally; the first run on the repository's CI host is pending.

Phase 11 remains in progress. This record does not claim that a remote CI run or a production
deployment has succeeded.

## Objective

Add a non-deploying quality gate that exercises the same delivery boundaries used by the local
full-stack Compose setup:

- backend tests with Java 25 and PostgreSQL Testcontainers
- frontend dependency installation and production build
- Docker Compose configuration validation
- reproducible backend and frontend image builds
- dependency and image vulnerability reporting

## Implemented behavior

`.github/workflows/ci.yml` runs on pushes to `main`/`master`, pull requests, and manual dispatch.
The jobs are intentionally separated so a backend or frontend failure is visible without waiting for
the other application layer:

- `backend-test` runs `./mvnw -B test` on Ubuntu 24.04 with Temurin Java 25 and Maven caching.
- `frontend-build` runs `npm ci`, `npm run build`, and stores a production dependency audit report.
- `compose-validate` runs `docker compose config --quiet` without starting external services.
- `container-build-and-scan` builds both application images and stores High/Critical Trivy reports.
- `dependency-review` runs only for pull requests and blocks newly introduced High or Critical
  dependency vulnerabilities.

The workflow has read-only repository permissions, cancels superseded runs, and does not publish
images, deploy resources, create cloud infrastructure, or upload anything to JobKorea.

## Vulnerability policy

Image scans use `CRITICAL,HIGH`, ignore unfixed findings, and are report-only (`exit-code: 0`) because
the current phase has not established a supported base-image remediation and exception process. Pull
request dependency changes are stricter: GitHub dependency review fails on newly introduced High or
Critical findings. The policy can become a blocking image gate after measured false-positive,
exception, and remediation handling is established.

## Local verification

The CI-equivalent checks were run locally during this step:

- `./mvnw test` - 98 tests passed, zero failures or errors.
- `frontend/npm run build` - production build succeeded.
- `docker compose config --quiet` - succeeded.
- `docker compose build backend frontend` - both images built successfully.
- the running Compose stack reported healthy backend, frontend, and PostgreSQL services.
- backend readiness, frontend SPA deep link, and frontend-proxied readiness returned HTTP 200.

During a later local revalidation, the first backend test attempt could not connect to the local
Docker named pipe, so Testcontainers could not start PostgreSQL. After Docker access was restored,
98 tests ran and two endpoint assertions exposed a deterministic behavior issue: the RAG match and
resume-draft flows requested a query embedding before checking whether the authenticated user had
any active embedding chunks. With no career evidence and no Ollama service, that surfaced as `503`
instead of the required `422 RAG_CONTEXT_EMPTY`. `VectorSearchService` now checks for owned active
chunks using the same source filter before calling the embedding provider, so an empty corpus is
reported as a grounding validation result and provider failures remain reserved for non-empty
searches. The full 98-test suite passed after this fix.

Remote CI verification history:

- Run 1 failed in the backend Maven step because the initial Windows-created commit stored
  `mvnw` as mode `100644`; Linux runners could not execute `./mvnw`.
- Commit `9d46469` changed `mvnw` to mode `100755`. Run 2 then passed backend tests, frontend build,
  and Compose validation.
- Run 2's container job failed during GitHub-hosted runner setup before any job step executed. The
  local backend/frontend image build passed. Run 3 reproduced the same setup failure; GitHub reported
  that `aquasecurity/trivy-action@v0.28.0` could not resolve its internal
  `aquasecurity/setup-trivy@v0.2.1` dependency. The workflow now uses Trivy Action v0.36.0 pinned to
  its immutable commit, which pins a valid setup-trivy revision.

The vulnerability reports and dependency-review result require a remote CI runner and are therefore
not marked as locally executed results.

## Concepts to understand

- CI quality gates should validate build and test boundaries without mutating deployment environments.
- A container image build is not the same as a runtime health check; both are needed.
- Vulnerability reporting and vulnerability blocking can have different policies while a baseline is
  being established.
- Dependency review detects changes introduced by a pull request, while image scanning observes the
  assembled runtime image and its operating-system packages.

## Completion criteria

- [x] CI workflow is version-pinned and non-deploying.
- [x] Backend, frontend, Compose, and container-build checks are defined.
- [x] Dependency and image vulnerability reporting is defined with an explicit severity policy.
- [x] Equivalent local checks pass.
- [ ] First remote CI run passes on the repository host.

## Next step

Phase 11 Step 3 is the deployment runbook and measured readiness work. Before starting it, run this
workflow on the repository host and resolve any environment-specific failures. Cloud deployment,
image publication, distributed infrastructure, and JobKorea automation remain out of scope.
