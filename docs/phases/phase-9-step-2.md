# Phase 9 Step 2 - Request Correlation and Privacy Baseline

## Status

Complete for this step. Phase 9 remains in progress; this Step record does not replace a Phase 9
completion record.

## Objective and scope

Prevent untrusted request IDs from becoming response/log correlation injection data and reduce accidental
environment disclosure. Preserve the existing prompt-injection defenses for job postings and career
evidence.

## Design reasons and important decisions

- Incoming `X-Request-Id` values are accepted only when they contain safe identifier characters and are at
  most 100 characters. Unsafe values are replaced with a server-generated UUID before being echoed or
  attached to error responses.
- Actuator environment info is disabled by default. Application version/name remain available through the
  explicit `info.app` values.
- Public auth routes are rate-limited by remote IP, while authenticated expensive routes use the JWT
  subject. The implementation does not trust `X-Forwarded-For` without an explicit proxy deployment
  policy.
- Existing prompts mark job postings and retrieved evidence as untrusted data, and server-side citation
  validation remains the enforcement boundary. No model-generated citation or instruction is trusted by
  this step.
- Provider request bodies and career content are not added to application logs. Provider response IDs and
  immutable artifact metadata remain the permitted operational references.

## Implemented features

- Request ID character/length validation and regeneration
- Auth/IP distinction for rate-limit subjects
- Authentication and Tool approval endpoints included in the rate-limit route groups
- Actuator environment disclosure disabled by default
- Request ID unit tests and expanded limiter bucket tests

## Explicitly excluded features

- Full PII detection or redaction inside user-authored career text
- Model-side prompt-injection guarantees
- OpenTelemetry deployment, log shipping, or a centralized trace backend
- Trusting forwarded proxy headers without deployment configuration

## Tests and executable verification

- Maven compile passed.
- Request ID, rate-limit, and error mapping tests passed.
- Full `mvnw test` after Step 2 passed: 91 tests, 0 failures, 0 errors, 0 skipped.
- PostgreSQL/Testcontainers integration coverage contained 31 tests.
- Existing provider tests continue to verify that provider error responses are not exposed as API detail.

## Failures encountered and root causes

No new implementation failure occurred in Step 2. The shared localhost rate-limit failure from Step 1 was
resolved by test-profile isolation and is recorded in the Step 1 record.

## Concepts the learner must understand

- Correlation metadata is input data and needs validation just like business input.
- Prompt-injection defense is layered: treat external text as untrusted, constrain the prompt, validate
  structured output, and enforce provenance on the server.
- Hiding environment details reduces accidental disclosure but does not replace secret management.
- A request ID helps trace a failure; it is not an authentication or authorization credential.

## Completion criteria

- [x] Unsafe request IDs cannot be echoed by the request filter.
- [x] Auth routes have IP-based limiter identity and authenticated routes have subject-based identity.
- [x] Actuator environment information is disabled by default.
- [x] Existing prompt and citation safety boundaries remain unchanged.
- [x] Unit, compile, and PostgreSQL integration tests pass.

## Next step and known limitations

Step 3 records the production architecture decision for distributed limiting, asynchronous execution,
streaming, tracing, and cloud deployment. No infrastructure is added without measured load or latency
evidence.
