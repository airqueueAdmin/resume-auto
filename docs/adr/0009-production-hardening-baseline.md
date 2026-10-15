# ADR 0009: Measured Production-Hardening Baseline

## Status

Accepted for Phase 9. Revisit before multi-instance or production deployment.

## Context

The platform now has synchronous AI calls with provider timeouts/retries, user-scoped Tool Calling,
bounded workflows, request IDs, and immutable AI artifacts. It does not yet have measured production load,
multi-instance deployment, or a trace backend. Adding Redis, queues, streaming, or cloud-specific services
without those measurements would increase operational complexity prematurely.

## Decision

Adopt a minimal hardening baseline:

- Enable configurable in-memory rate limiting for authentication, AI, retrieval, workflow, and Tool
  approval/call route groups.
- Expose liveness/readiness health probes with limited details.
- Sanitize incoming request IDs and keep environment information disabled by default.
- Keep provider calls synchronous with existing timeouts and bounded retries.
- Keep prompt-injection and provenance controls in the prompt/server validation boundary.
- Defer Redis, queueing, streaming, OpenTelemetry deployment, and AWS-specific resources until measured
  latency, throughput, failure, or horizontal-scaling requirements justify them.

## Consequences

The baseline improves abuse resistance and operational diagnosis without adding a distributed dependency.
The in-memory limiter is not correct across multiple application instances and may retain keys for the
process lifetime of active windows; a shared limiter or gateway policy is mandatory before horizontal
scaling. Synchronous provider calls still occupy request threads and must move behind a queue only when
observed latency/load warrants it.
