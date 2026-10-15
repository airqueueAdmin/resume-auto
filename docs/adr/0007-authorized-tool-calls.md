# ADR 0007: Authorized Tool Calls and One-Time Mutation Approval

## Status

Accepted for Phase 6 Step 1.

## Context

Future AI workflows need a narrow way to invoke backend capabilities. A model-generated tool name or
argument must not become an authorization decision, and a mutating call must not change user data
without a distinct user approval event.

## Decision

- Keep tool definitions in a code-owned allowlist with explicit names, mutability, and input schemas.
- Resolve the authenticated user only from the JWT subject and delegate resource ownership checks to
  existing application services.
- Issue mutation approvals through a separate authenticated endpoint after the user reviews the exact
  tool arguments.
- Store only a SHA-256 hash of a random short-lived approval token.
- Bind each approval to the user, tool name, and canonical JSON argument hash.
- Consume approvals under a pessimistic row lock and mark them used in the same transaction as the
  mutation.
- Start with grounded resume-draft approval, then add `career.experience.create` through the same
  boundary as the first ordinary user-data mutation. Do not add LLM function calling or Agent workflow
  in these steps.

## Alternatives considered

- A request-body `approved: true` flag was rejected because a model or untrusted caller could set it.
- A self-contained signed token was rejected for the first step because one-time replay prevention
  needs server-side state.
- Direct reflection over Spring services was rejected because it would expose an unstable and overly
  broad capability surface.

## Consequences

- Mutation execution is auditable and replay-resistant, at the cost of one approval row per call.
- Expired rows require later retention cleanup; cleanup is deferred to production hardening.
- The tool surface remains intentionally small until each new capability has a server-side ownership
  and approval story.
