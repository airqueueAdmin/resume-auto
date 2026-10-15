# Phase 9 - Production Hardening Baseline

## Status

Complete for the measured baseline scope. The project has configurable abuse controls, health probes,
request-correlation validation, privacy defaults, and explicit decisions to defer distributed/cloud
infrastructure until evidence justifies it. This is not a claim of production deployment readiness.

## Objective and scope

Harden the synchronous application boundary for local and single-instance operation, then document when
Redis, asynchronous processing, streaming, tracing backends, and AWS deployment become necessary.

## Design reasons and important decisions

- A local in-memory limiter is sufficient for the current single-instance development/test boundary and
  avoids an unmeasured Redis dependency. It is explicitly not valid as the final multi-instance policy.
- Liveness/readiness probes support container orchestration without exposing detailed database or
  environment information.
- Request IDs are server-generated or sanitized before echoing, reducing header/log injection risk.
- Existing provider timeouts, bounded retries, untrusted-data prompts, server-side citations, ownership
  checks, and immutable AI artifacts remain the primary AI safety boundaries.
- Synchronous execution, no streaming, and no queue are deliberate decisions because the repository has
  no representative production latency/throughput evidence yet.
- ADR 0009 records migration triggers for shared rate limiting, async execution, tracing, and AWS.

## Implemented features

- Configurable in-memory rate limiting for auth, AI, retrieval, workflow, and Tool routes
- Stable `429 RATE_LIMIT_EXCEEDED` response with `Retry-After`
- Public liveness/readiness health probes
- Request ID sanitization and maximum length enforcement
- Disabled actuator environment disclosure by default
- Phase 9 architecture decision record and migration triggers

## Explicitly excluded features

- Redis/distributed rate limiting
- Queueing, asynchronous workers, streaming, and backpressure infrastructure
- OpenTelemetry backend and centralized log platform
- AWS resources or deployment automation
- Full PII redaction and model-proof prompt-injection prevention

## Tests and executable verification

- Maven compile passed.
- Full `mvnw test` passes: 91 tests, with 0 failures, 0 errors, and 0 skipped tests.
- PostgreSQL/Testcontainers integration coverage contains 31 tests.
- Tests cover fixed-window limit behavior, bucket isolation, disabled test profile, 429 mapping, request ID
  sanitization, health probes, migrations, user isolation, AI grounding, Tool approval, and bounded
  workflow behavior.

## Failures encountered and root causes

- The initial rate-limit integration run produced six `429` failures because the test suite reused one
  localhost IP bucket across test methods. Disabling the web limiter only in `application-test.yml` fixed
  test-state coupling; production remains enabled and unit tests verify the limiter independently.
- No unresolved implementation failure remains.

## Concepts the learner must understand

- Production hardening is an evidence-based trade-off, not a checklist of infrastructure products.
- A local limiter protects one process; distributed correctness needs a shared state or an upstream
  gateway.
- Health probes, request correlation, timeouts, retries, and provenance solve different operational and
  security problems.
- A passing test suite proves the current boundary, not capacity, SLO compliance, or production readiness.

## Completion criteria

- [x] Abuse and request-rate controls exist for the current synchronous boundary.
- [x] Liveness/readiness and privacy-safe correlation defaults are verified.
- [x] AI prompt/provenance and provider error boundaries remain enforced.
- [x] Deferred infrastructure decisions and migration triggers are documented.
- [x] Full unit and PostgreSQL integration tests pass.
- [x] No unmeasured Redis, queue, streaming, or cloud dependency was added.

## Next phase and known limitations

Phase 10 covers JobKorea resume integration. The platform remains single-instance-oriented for rate
limiting, synchronous for AI requests, and without a centralized trace backend. These limitations must be
addressed before a horizontally scaled production deployment.
