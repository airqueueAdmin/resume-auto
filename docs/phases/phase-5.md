# Phase 5 - Grounded Resume Generation

## Status

Complete for the synchronous local-provider scope.

## Objective and scope

Generate resume drafts from existing, user-owned career facts with enforceable source provenance, then
require explicit user approval before a draft becomes a new immutable `ResumeVersion`.

## Design reasons and important decisions

- Retrieval and generation use only the authenticated user's active Experience/Project evidence and a
  saved JobPosting.
- Server-assigned citation labels are resolved against retrieved chunks before a draft is persisted.
- Drafts are immutable derived artifacts with provider metadata, usage, and source snapshots; they do
  not overwrite user-authored career data.
- Approval is a separate, explicit operation that accepts only a target Resume ID and reuses existing
  ownership, row-locking, and immutable version rules.
- Citation labels remain in approved plain-text content while detailed provenance remains in the draft
  artifact for audit and later evaluation.

## Implemented features

- Structured grounded resume draft generation from a saved JobPosting
- Ollama default provider and optional OpenAI structured-output provider
- Bounded retrieval context and server-side citation validation
- Immutable `ai_resume_drafts` persistence with source snapshots
- Explicit draft approval into a new immutable ResumeVersion
- User isolation, invalid-input handling, no-evidence handling, and immutable-version behavior
- Runtime OpenAPI documentation and PostgreSQL integration coverage

## Explicitly excluded features

- Editing generated lines during approval
- Automatic Resume selection or creation
- PDF/DOCX export
- Tool calling, agents, evaluation datasets, asynchronous processing, and production hardening
- Claim-level factual verification beyond validating source linkage

## Tests and executable verification

- Full Maven test suite passes with unit, provider-stub, migration, ownership, and PostgreSQL
  Testcontainers integration coverage.
- Integration tests verify draft generation boundaries, explicit approval, cross-user isolation,
  sequential version creation, citation retention, and immutable prior versions.
- Flyway and Hibernate validate the eight-migration PostgreSQL schema.

## Failures encountered and root causes

- Approval initially had no persistence path from the draft artifact to a user-authored version. The
  missing path was resolved by adding a user-scoped approval service and delegating version creation to
  the existing locked `ResumeVersionService` rather than duplicating versioning logic.
- ResumeVersion stores plain text while draft provenance is structured JSON. Citation labels are retained
  in approved content and the original draft remains available for complete source snapshots.

## Concepts the learner must understand

- A generated artifact and user-approved source data have different trust and mutation boundaries.
- User approval is an authorization event, not a signal to trust client-provided content.
- Immutable versions require sequential numbering and concurrency control on the parent Resume.
- Citation linkage improves auditability but does not itself prove factual correctness.

## Completion criteria

- [x] Drafts are generated only from authenticated user's retrieved evidence.
- [x] Every generated line is validated against server-resolved citations.
- [x] Draft artifacts retain source snapshots and provider metadata.
- [x] Users explicitly approve drafts before a ResumeVersion is created.
- [x] Approved content is traceable and existing versions remain immutable.
- [x] Ownership, validation, migration, unit, and PostgreSQL integration tests pass.

## Next phase and known limitations

Phase 6 will expose narrowly scoped backend capabilities as tools. Mutating tools must perform their
own server-side authorization and require explicit user approval. Approval remains synchronous and
plain-text; editing workflows, export formats, and evaluation are deferred.
