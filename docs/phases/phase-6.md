# Phase 6 - Authorized Tool Calling

## Status

Complete for the approved synchronous application boundary. Agent workflow is deferred to Phase 7.

## Objective and scope

Expose a small, code-owned Tool Calling boundary that can be used by an authenticated client or a future
provider adapter without granting arbitrary service access. Mutating tools require explicit user approval,
and every call remains scoped to the authenticated user.

## Design reasons and important decisions

- A code-owned allowlist resolves tool names and publishes strict per-tool schemas. Unknown tools and
  unknown fields never reach application services.
- Read tools delegate to existing user-scoped services, so a resource owned by another user returns the
  same not-found boundary as the normal API.
- Mutating calls require a separate approval request. The database stores only a SHA-256 token hash and
  binds each approval to the user, tool name, and canonical argument hash.
- Approval consumption uses a pessimistic row lock and is consumed in the same transaction as the
  mutation. Missing, cross-user, expired, mismatched, and replayed approvals are rejected.
- The HTTP adapter maps into the provider-neutral `ToolCallExecutor` application port. Future LLM or
  workflow code must use this port rather than call domain services directly.
- Tool exceptions expose stable `TOOL_*` error codes. Transport parsing errors remain separate from tool
  schema and authorization errors.
- The mutation set is intentionally narrow: grounded Resume draft approval and creation of user-owned
  Experience, Project, and JobPosting resources. Update/delete tools are deferred until a concrete
  workflow requires them.

## Implemented features

- `GET /api/v1/tools` allowlisted catalog with mutability flags and input schemas
- `POST /api/v1/tools/approvals` short-lived, one-time, argument-bound approval issuance
- `POST /api/v1/tools/calls` authenticated allowlisted execution
- User-scoped read tools:
  - `career.experience.get`
  - `career.project.get`
  - `career.job-posting.get`
- Explicitly approved mutating tools:
  - `career.resume-draft.approve`
  - `career.experience.create`
  - `career.project.create`
  - `career.job-posting.create`
- PostgreSQL V9 `tool_approvals` migration with hashed tokens and row-locked consumption
- Provider-neutral application call and result contracts
- Stable Tool error-code contract and global API error mapping

## Explicitly excluded features

- LLM provider function/tool-call integration
- Agent planning, dynamic tool selection, workflow state, and evaluation
- Experience/Project/JobPosting update/delete tools
- Bulk or asynchronous tool execution
- JobKorea integration and automatic JobKorea-form Resume generation
- Phase 8 evaluation datasets and Phase 9 production hardening

## Tests and executable verification

- Maven compile passed.
- Full `mvnw test` passes: 68 tests, with 0 failures, 0 errors, and 0 skipped tests.
- PostgreSQL/Testcontainers integration coverage contains 29 tests.
- Integration tests cover catalog access, unauthenticated access, owned reads, cross-user reads,
  unknown tools, unknown fields, malformed identifiers, invalid domain values, missing approval,
  cross-user approval, approval replay, successful Experience/Project/JobPosting creation, and grounded
  Resume draft approval.
- Unit tests cover allowlist ordering, mutability classification, canonical argument hashing, Tool error
  codes, and approval-domain expiry/one-time behavior.

## Failures encountered and root causes

- The first HTTP implementation used Jackson 2 `JsonNode` directly in request records, while Spring Boot 4
  uses Jackson 3 for HTTP conversion. Requests failed before reaching the Controller. The final boundary
  accepts `Map<String, Object>` in HTTP DTOs and converts it to the application Jackson model inside the
  Controller.
- No additional failure occurred while adding the provider-neutral executor or JobPosting creation tool.

## Concepts the learner must understand

- A tool name is an allowlisted capability identifier, not a service reflection target.
- Authentication identifies the user, while explicit approval authorizes a specific mutation and exact
  argument set.
- Canonical argument hashing prevents approval reuse with reordered or changed arguments.
- Application ports should remain independent from HTTP and model-provider transport formats.
- User ownership must be enforced again inside application services; a tool schema is not an authorization
  substitute.
- Tool output provenance and grounded Resume approval remain separate from future Agent behavior.

## Completion criteria

- [x] Tool names and schemas are code-owned and allowlisted.
- [x] Read calls enforce authenticated user ownership.
- [x] Mutations require server-checked, one-time explicit approval.
- [x] Approval tokens are hashed, short-lived, argument-bound, and row-locked on consumption.
- [x] Provider-neutral execution and stable Tool error contracts are established.
- [x] Unit and PostgreSQL integration tests pass.
- [x] LLM function calling and Agent behavior remain excluded until their planned phases.

## Next phase and known limitations

Phase 7 may introduce a bounded deterministic workflow where dynamic Tool selection is genuinely useful.
It must preserve the Phase 6 allowlist, authenticated ownership checks, explicit approval for mutations,
and step/timeout/cost limits. There is no Agent implementation in Phase 6.
