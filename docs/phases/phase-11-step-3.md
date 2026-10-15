# Phase 11 Step 3 - Deployment Runbook and Measured Readiness

## Status

Complete for the approved single-host packaging and operability scope. This record does not claim
that the application is production-ready or deployed to a cloud environment.

## Objective and scope

Document the repeatable deployment and incident procedures around the Compose delivery unit, then
capture a small local baseline for startup, readiness, image size, and representative endpoint
latency. Cloud deployment, image publication, horizontal scaling, and distributed infrastructure
remain explicitly out of scope.

## Implemented work

- Added [deployment-and-readiness.md](../runbooks/deployment-and-readiness.md).
- Documented secret injection without committing `.env` or provider credentials.
- Documented PostgreSQL backup/restore, Flyway migration rollout, rollback boundaries, readiness
  checks, smoke checks, and incident triage.
- Measured a separate cold container/database start without interrupting the existing local stack.
- Recorded backend/frontend/pgvector image sizes and 20-sample readiness latency baselines.
- Kept the current single-instance Compose topology and did not add cloud-specific infrastructure.

## Design reasons

- Secrets remain deployment-environment inputs because checked-in defaults are suitable only for local
  development.
- Flyway remains the only schema owner; rollback is application-first because database migrations may
  not be safely reversible.
- Readiness and smoke checks are separate from image build success because a valid image can still
  fail on datasource, migration, proxy, or dependency startup.
- Measurements are recorded with their environment and limitations so they can inform later decisions
  without being mistaken for production SLO evidence.

## Verification and measurements

- `docker compose config --quiet`: passed.
- `docker compose build backend frontend`: passed.
- Temporary `phase11-step3` Compose project started PostgreSQL, backend, and frontend healthy.
- Cold container/database start to backend readiness: 22.4 seconds.
- Cold container/database start to frontend HTTP 200: 22.4 seconds.
- Image sizes: backend 135,757,142 bytes; frontend 26,353,719 bytes; pgvector PostgreSQL
  160,561,945 bytes.
- Twenty direct and twenty frontend-proxied readiness requests returned HTTP 200. Backend p50/p95
  latency was 19.98/25.27 ms; proxied p50/p95 latency was 19.20/24.94 ms.
- The measurement-only containers, network, and PostgreSQL volume were removed. The existing
  `ai-career-platform` stack remained healthy and returned HTTP 200 for both readiness paths.

## Failures and root causes

No Step 3 implementation failure occurred. The measured stack used a fresh temporary project and was
cleaned up after verification. Earlier Phase 11 CI failures and their fixes are recorded in
[Phase 11 Step 2](phase-11-step-2.md): the Maven wrapper executable bit and an invalid Trivy action
dependency.

## Concepts to understand

- Secret injection is a deployment concern; `.env.example` is documentation, not a secret store.
- Database backup, migration compatibility, and application rollback are coupled decisions.
- A healthcheck answers whether a component is ready for a narrow dependency contract, not whether the
  system meets a production SLO.
- A local latency sample needs workload, concurrency, network, and persistence context before it can
  justify scaling infrastructure.

## Completion criteria

- [x] Secret injection and local-secret exclusions are documented.
- [x] Backup/restore and migration rollout procedures are documented.
- [x] Rollback and incident checks are documented with irreversible-operation warnings.
- [x] Cold-start, image-size, readiness, and representative latency measurements are recorded.
- [x] No cloud resource, image publication, or distributed infrastructure was added.

## Next phase and limitations

Phase 11 is complete for the approved packaging and measured-readiness scope. Known limitations are
the single-host deployment model, manual release promotion, no automatic rollback, no production
traffic or load sample, no managed backup retention, and no centralized observability backend.
Address those only from measured operational evidence and explicit authorization.
