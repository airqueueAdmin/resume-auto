# Phase 11 - Deployment Packaging and Operability

## Status

Complete for the approved scope. The project now has reproducible full-stack container packaging, a
non-deploying CI quality gate verified on the repository host, and a measured single-host deployment
runbook. This is not a claim of cloud deployment or production capacity.

## Objective and scope

Package the backend, frontend, and PostgreSQL stack reproducibly, verify delivery boundaries in CI,
and document startup, configuration, recovery, and readiness checks without introducing unmeasured
distributed infrastructure.

## Important decisions

- The backend runs as a non-root Java 25 runtime image; the frontend is built into static Nginx assets.
- Compose health ordering is the local deployment contract: PostgreSQL before backend, backend before
  frontend.
- CI validates tests, frontend build, Compose configuration, container builds, and vulnerability
  reporting but does not publish images or deploy resources.
- Production secrets are injected outside Git and images.
- Flyway remains the only schema migration owner. Application rollback does not imply database
  migration rollback.
- The current evidence supports a single-host model only; Redis, queues, Kubernetes, and cloud
  resources remain deferred.

## Implemented features

- Java 25 backend and React/Nginx frontend container images.
- PostgreSQL + pgvector Compose service with healthcheck and health-ordered dependencies.
- Same-origin frontend proxy for API and readiness paths with SPA deep-link fallback.
- GitHub Actions quality gate for backend tests, frontend build/audit, Compose validation, image
  builds, and report-only High/Critical image scanning.
- Deployment and readiness runbook covering configuration, secrets, backup/restore, migrations,
  rollback, smoke checks, incidents, and evidence-based review triggers.
- Local baseline for cold container/database startup, image size, readiness, and latency.

## Explicitly excluded

- Cloud account creation or deployment
- Image registry publication or automatic release promotion
- Kubernetes, Terraform, Redis, queues, service mesh, or distributed tracing backend
- Automatic database failover, automatic rollback, or production backup retention service
- JobKorea login/upload automation

## Tests and executable verification

- Local Maven/Testcontainers suite: 98 tests passed.
- Local frontend production build: passed.
- Local Compose validation: passed.
- Local backend and frontend image builds: passed.
- Remote CI run 4 for commit `845942c`: backend tests, frontend build/audit, Compose validation, and
  container builds/Trivy reports all passed.
- Step 3 temporary Compose measurement: backend and frontend readiness returned HTTP 200; the
  temporary resources were removed afterward.

## Failures and root causes

- Remote CI run 1 failed because the Windows-created `mvnw` was committed as mode `100644`; commit
  `9d46469` corrected it to executable mode `100755`.
- Remote CI runs 2 and 3 could not resolve `aquasecurity/setup-trivy@v0.2.1` through the old Trivy
  action. The workflow now pins a valid Trivy Action v0.36.0 commit, and run 4 passed.
- The local revalidation initially could not access Docker from the sandbox; rerunning with Docker
  access allowed Testcontainers to execute and exposed the actual empty-RAG regression, which was
  fixed before the successful test run.

## Concepts the learner must understand

- Build reproducibility, runtime health, and production capacity are separate claims.
- CI gates should be non-deploying and should state which findings are report-only versus blocking.
- Migrations and rollback require compatibility planning; database restoration is a data-loss event
  for writes after the backup.
- Readiness, latency, and image-size measurements are useful only with their environment and workload
  attached.
- Infrastructure should follow measured constraints rather than a default product checklist.

## Completion criteria

- [x] Backend, frontend, and PostgreSQL packaging is reproducible locally.
- [x] The non-deploying CI quality gate passes on the repository host.
- [x] Deployment, recovery, rollback, and incident procedures are documented.
- [x] Measured readiness and baseline limits are recorded.
- [x] Cloud deployment and distributed infrastructure remain excluded until explicitly justified.

## Known limitations and next direction

The system remains a single-host, manually promoted Compose deployment with no production traffic
benchmark, centralized tracing, managed backup retention, or automatic rollback. The next change must
be justified by measured latency, concurrency, reliability, cost, privacy, or operational evidence;
cloud deployment and image publication are not implied by this completion record.
