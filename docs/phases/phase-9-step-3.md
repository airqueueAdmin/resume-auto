# Phase 9 Step 3 - Production Architecture Decision

## Status

Complete. Phase 9 is complete for the measured baseline scope; see the [Phase 9 completion record](phase-9.md).

## Objective and scope

Review the remaining Phase 9 options—Redis, asynchronous jobs, queueing, streaming, tracing, privacy
controls, prompt-injection defenses, and AWS deployment—and select only the changes justified by the
current evidence.

## Decision matrix

| Area | Current evidence | Decision |
| --- | --- | --- |
| Rate limiting | Synchronous endpoints and no multi-instance runtime | Implement local configurable baseline; defer Redis. |
| Async/queueing | No production latency or throughput sample | Keep synchronous bounded calls. |
| Streaming | Structured JSON artifacts and approval boundaries require complete output | Defer streaming. |
| Tracing | Request ID, provider response ID, prompt/model metadata already exist | Keep lightweight correlation; defer trace backend. |
| Privacy | Provider bodies are not logged; environment info can disclose configuration | Sanitize request IDs and disable env info. |
| Prompt injection | External text is marked untrusted and citations are server-validated | Preserve layered controls; no claim of model-proof safety. |
| AWS deployment | No target account, SLO, traffic, or cost evidence | Defer cloud-specific resources. |

## Explicitly excluded features

- Redis or another distributed rate-limit store
- Queue/broker, worker fleet, asynchronous API, and streaming transport
- OpenTelemetry collector/backend and AWS infrastructure
- Automatic PII redaction or model-side factual verification

## Tests and executable verification

- Phase 9 baseline full suite passes: 91 tests, 0 failures, 0 errors, 0 skipped.
- PostgreSQL/Testcontainers integration coverage contains 31 tests.
- ADR 0009 records the decision and known migration triggers.

## Failures encountered and root causes

The first rate-limit integration run failed because test methods shared one localhost IP bucket. The test
profile was isolated from production rate limiting, and the limiter itself remained covered by fixed-clock
unit tests.

## Completion criteria

- [x] A rate-limit baseline is implemented and its single-instance boundary is explicit.
- [x] Health probes and request-correlation/privacy controls are implemented.
- [x] Remaining production-hardening options are evaluated against current evidence.
- [x] Deferred infrastructure has documented migration triggers.
- [x] Full unit and PostgreSQL integration tests pass.

## Next phase and known limitations

Phase 10 is the separately planned JobKorea integration. It must first verify an official personal-resume
API or use the user-controlled export/handoff flow; it must not collect credentials or bypass CAPTCHA and
anti-automation controls. Phase 9 does not claim production deployment readiness.
