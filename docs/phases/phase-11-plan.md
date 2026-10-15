# Phase 11 - Deployment Packaging and Operability Plan

## Status

In progress. Step 1 full-stack container packaging is complete. Step 2 CI workflow is implemented;
its first remote CI run is pending.

## Objective

Turn the portfolio-grade implementation into a reproducible, inspectable delivery unit without adding
unmeasured distributed infrastructure. Preserve the existing single-instance operational assumptions
while making startup, health verification, configuration, and release checks explicit.

## Planned steps

### Step 1 - Reproducible full-stack containers

- Build the Java 25 backend as a non-root runtime image.
- Build the React frontend as static assets served by Nginx.
- Proxy same-origin `/api` and `/actuator` requests to the backend and preserve SPA deep links.
- Start frontend, backend, and pgvector PostgreSQL with health-ordered Docker Compose dependencies.
- Keep local Ollama reachable through an explicit host bridge and keep provider secrets outside images.

### Step 2 - Automated delivery quality gate

- Add a CI workflow for backend tests, frontend build, Compose validation, and container builds.
- Pin action and tool versions and avoid publishing images or deploying without explicit authorization.
- Add dependency and image vulnerability reporting with documented severity policy.

Status: implemented locally in `.github/workflows/ci.yml`; a repository-hosted run is still required to
verify the runner-specific action, Docker, and vulnerability-reporting behavior.

### Step 3 - Deployment runbook and measured readiness

- Document secret injection, backup/restore, migration rollout, rollback, and incident checks.
- Capture cold-start, readiness, image size, and representative request latency before selecting a cloud
  platform or introducing horizontal scaling.
- Revisit Redis, queueing, centralized tracing, and managed PostgreSQL only from measured evidence.

## Explicit non-goals

- Production cloud account creation or deployment
- Kubernetes, Terraform, Redis, queues, or service mesh
- Automatic image publication or external state mutation
- Treating local Compose health as proof of production capacity or SLO compliance
