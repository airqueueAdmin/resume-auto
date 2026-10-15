# Phase 6 Step 5 - Approved JobPosting Creation Tool

## Status

Complete for this step. Phase 6 is complete; see the [Phase 6 completion record](phase-6.md).

## Objective and scope

Complete the first Tool Calling capability set with a user-facing JobPosting creation workflow. A saved
JobPosting is the input boundary for later analysis, matching, and grounded Resume generation, so its
creation is a meaningful mutation with its own source URL validation rule.

## Design reasons and important decisions

- `career.job-posting.create` reuses `JobPostingService.create`, preserving active-user checks,
  normalization, persistence, and the existing source-of-truth boundary.
- The tool publishes a strict schema for title, company, description, and optional source URL. The source
  URL accepts only HTTP/HTTPS, matching the normal JobPosting API contract.
- The tool does not accept a user ID. Ownership comes only from the authenticated JWT subject.
- Creation requires the same short-lived, canonical-argument-bound, one-time approval as the other
  mutating tools.

## Implemented features

- `career.job-posting.create` in the allowlisted catalog
- Typed JobPosting creation arguments and published input schema
- Source URL protocol and length validation
- Explicit approval and authenticated user-scoped creation
- Integration coverage for invalid source URL, missing approval, cross-user approval, and success

## Explicitly excluded features

- JobPosting update/delete tools
- LLM tool-call parsing, Agent workflow, retries, and evaluation
- Direct JobKorea integration or automatic Resume creation

## Tests and executable verification

- Maven compile passed.
- JobPosting-specific PostgreSQL integration test passed.
- Full `mvnw test` passes: 68 tests, including 29 PostgreSQL Testcontainers integration tests.
- `ToolRegistryTest` verifies the expanded catalog and mutability classification.

## Failures encountered and root causes

No new implementation failure occurred in this step. Existing validation and approval behavior remained
compatible with the provider-neutral Tool Calling contract introduced in Step 4.

## Completion criteria

- [x] JobPosting creation is exposed only through the allowlisted tool name.
- [x] Required fields and HTTP/HTTPS source URL validation are enforced.
- [x] The tool never accepts a client-supplied owner ID.
- [x] Mutation requires a matching, unexpired, one-time approval.
- [x] Cross-user approval and missing approval are rejected.
- [x] Full unit and PostgreSQL integration tests pass.

## Next phase

Phase 6 is complete. Phase 7 introduces a bounded deterministic workflow only where dynamic Tool
selection is genuinely needed; it must not weaken the Phase 6 authorization boundary.
