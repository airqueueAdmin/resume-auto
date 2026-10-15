# Phase 6 Step 1 - Authorized Tool Calling Boundary

## Status

Complete for the first Phase 6 step. Phase 6 is not complete.

## Objective and scope

Expose a small, explicit tool boundary that a future agent can call without granting the model direct
access to application services. This step covers the allowlist, input schemas, authenticated user
scoping, and explicit approval for the first mutating tool. It does not add an LLM tool-call parser or
Agent workflow.

## Design reasons and important decisions

- Tool names are resolved through a code-owned registry. Unknown names never reach a service.
- Tool arguments are parsed against a per-tool schema with unknown fields rejected. This prevents a
  caller from smuggling an authoritative `userId` into a request.
- Read tools delegate to the existing user-scoped application services, so resource ownership remains
  enforced in the normal application layer.
- Mutating calls require a separate approval request. The server stores only a hash of a random,
  short-lived token and binds it to the authenticated user, tool name, and canonical argument hash.
- Approval consumption uses a pessimistic row lock and marks the token used in the same transaction as
  the mutation. A replay or a different user cannot execute the call.
- The first mutation is grounded resume-draft approval because Phase 5 already provides the required
  ownership, citation, and immutable-version rules. New CRUD mutations are intentionally deferred.

## Implemented features

- `GET /api/v1/tools` catalog with mutability flags and JSON input schemas
- `POST /api/v1/tools/approvals` for authenticated user approval of a mutating call
- `POST /api/v1/tools/calls` for allowlisted execution
- Read tools: `career.experience.get`, `career.project.get`, and `career.job-posting.get`
- Mutating tool: `career.resume-draft.approve`
- V9 PostgreSQL migration for one-time approvals
- Global errors for unknown tools, invalid arguments, missing approval, and invalid/replayed approval

## Explicitly excluded features

- LLM provider function/tool-call integration
- Agent planning, dynamic tool selection, retries, or workflow state
- Arbitrary service reflection or client-supplied owner IDs
- Additional mutating CRUD tools
- Evaluation datasets and production hardening

## Tests and executable verification

- Maven compile passed.
- `ToolRegistryTest` verifies the allowlist, mutability classification, unknown-field rejection,
  malformed UUID rejection, and order-independent argument hashing.
- `ToolApprovalTest` verifies expiry and one-time consumption at the domain boundary.
- The PostgreSQL integration suite includes catalog, ownership, validation, approval, replay, and
  cross-user mutation checks.
- Full `mvnw test` passes: 65 tests, including 27 PostgreSQL Testcontainers integration tests.

## Completion criteria for this step

- [x] Tool names and input schemas are code-owned and allowlisted.
- [x] Every execution derives ownership from the authenticated JWT subject.
- [x] Resource reads return not-found for another user's resource.
- [x] Mutations require a short-lived, argument-bound, one-time approval.
- [x] Invalid arguments and approval replay are rejected with explicit error codes.
- [x] Full PostgreSQL integration verification in an environment with Docker available.

## Next step

Step 2 adds a second mutating tool through the same approval boundary. Keep Agent behavior and
evaluation work deferred to Phases 7 and 8.
