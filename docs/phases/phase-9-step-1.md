# Phase 9 Step 1 - Rate Limiting and Health Probes

## Status

Complete for this step. Phase 9 remains in progress; this Step record does not replace a Phase 9
completion record.

## Objective and scope

Add the first production-hardening controls for expensive and authentication-sensitive routes: a
configurable rate limit and public liveness/readiness probes.

## Design reasons and important decisions

- AI, retrieval, workflow, Tool approval/call, and authentication routes share a bounded request window.
  Authenticated requests use the JWT subject; public authentication requests use the remote IP address.
- The default is 60 requests per minute per subject and route group. A rejected request returns `429
  Too Many Requests` with a `Retry-After` header and stable `RATE_LIMIT_EXCEEDED` code.
- The first implementation is an in-memory single-instance limiter. It avoids adding Redis before a
  measured multi-instance requirement exists, but it must be replaced or coordinated through Redis before
  horizontal scaling.
- Actuator liveness and readiness probes are public and expose only health status. Detailed health output
  remains disabled by the default profile.
- The test profile disables the web limiter because integration tests intentionally reuse localhost and
  perform many signup requests; limiter behavior is independently covered by deterministic unit tests.

## Implemented features

- Configurable `app.rate-limit.enabled`, `window`, and `max-requests` properties
- Per-user/IP, route-group in-memory window counter
- `429` error mapping with `Retry-After`
- Public `/actuator/health/liveness` and `/actuator/health/readiness`
- Public health probe integration verification

## Explicitly excluded features

- Redis/distributed rate limiting
- Per-tenant quotas, billing-aware limits, and adaptive throttling
- Queueing, asynchronous processing, and streaming
- Production deployment or autoscaling configuration

## Tests and executable verification

- Maven compile passed.
- Rate-limit and error-handler unit tests passed.
- Full `mvnw test` after Step 1 passed: 85 tests, 0 failures, 0 errors, 0 skipped.
- PostgreSQL/Testcontainers integration coverage contained 31 tests.
- Integration test verified public liveness/readiness probes.

## Failures encountered and root causes

- The first full run returned six unexpected `429` responses because the Testcontainers integration suite
  shared one localhost IP bucket across test methods. The test profile now disables the web limiter, while
  unit tests retain deterministic limiter coverage. Production defaults remain enabled.

## Concepts the learner must understand

- In-memory rate limiting is process-local and is not a distributed consistency mechanism.
- Authentication and rate limiting solve different problems: the former identifies a subject, while the
  latter bounds resource consumption.
- Liveness answers whether the process is alive; readiness answers whether it should receive traffic.
- Test isolation is necessary when a security control intentionally tracks request history.

## Completion criteria

- [x] Expensive and authentication-sensitive routes have configurable limits.
- [x] User/IP bucket identity is explicit and tested.
- [x] Rejections expose stable API error and retry metadata.
- [x] Liveness/readiness probes are public and tested.
- [x] Single-instance limitation is documented.
- [x] Unit and PostgreSQL integration tests pass.

## Next step and known limitations

Step 2 hardens request-correlation and privacy boundaries. Before multi-instance deployment, the local
limiter must be replaced with a shared store or an equivalent gateway policy.
