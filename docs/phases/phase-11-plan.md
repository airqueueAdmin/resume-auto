# Phase 11 - Deployment Packaging and Operability Plan

## Status

Complete for the approved packaging and measured-readiness scope. Step 1 container packaging, Step 2
remote CI verification, and Step 3 deployment runbook work are complete. This does not claim cloud
deployment or production capacity.

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

Status: complete. Remote CI run 4 verified the runner-specific action, Docker, and vulnerability-reporting
behavior for commit `845942c`.

### Step 3 - Deployment runbook and measured readiness

- Document secret injection, backup/restore, migration rollout, rollback, and incident checks.
- Capture cold-start, readiness, image size, and representative request latency before selecting a cloud
  platform or introducing horizontal scaling.
- Revisit Redis, queueing, centralized tracing, and managed PostgreSQL only from measured evidence.

Status: complete for the documented single-host scope. A local baseline and operating procedures are
recorded in `docs/runbooks/deployment-and-readiness.md` and `docs/phases/phase-11-step-3.md`.

## Explicit non-goals

- Production cloud account creation or deployment
- Kubernetes, Terraform, Redis, queues, or service mesh
- Automatic image publication or external state mutation
- Treating local Compose health as proof of production capacity or SLO compliance
