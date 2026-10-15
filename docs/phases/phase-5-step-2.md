# Phase 5 Step 2 - Explicit Resume Draft Approval

## Status

Complete.

## Objective and scope

Allow an authenticated user to approve a previously generated, citation-grounded resume draft into a
new immutable, user-owned `ResumeVersion`. Approval must preserve ownership, sequential versioning,
source traceability, and the immutability of all previous versions.

## Design reasons and important decisions

- The approval request contains only the target `resumeId`; the server never accepts replacement
  content, preventing a client from bypassing the grounded draft artifact.
- Draft lookup is scoped by both draft ID and authenticated user ID. Cross-user approval therefore
  returns the same 404 resource boundary used by the rest of the career API.
- The stored structured draft is formatted deterministically into plain text. Each non-empty line keeps
  its citation labels (`[C1]`, `[C2]`, ...), while the immutable draft row retains the full source
  snapshots and source IDs.
- Approval delegates to `ResumeVersionService`, preserving active-user validation, target Resume
  ownership, pessimistic locking, next-version numbering, and immutable `ResumeVersion` creation.
- Invalid stored JSON and uncited stored lines are rejected as grounding validation failures rather than
  being converted into an untraceable version.

## Implemented features

- `POST /api/v1/ai/resume-drafts/{draftId}/approve`
- `ApproveResumeDraftRequest` with required target `resumeId`
- User-scoped draft repository lookup
- Deterministic grounded-draft-to-ResumeVersion formatter
- Citation-label preservation in approved version content
- Reuse of existing ResumeVersion ownership, locking, and sequential version rules
- Unit tests for formatting, missing ownership, and uncited stored lines
- PostgreSQL/Testcontainers integration coverage for approval, cross-user isolation, version 2
  creation, citation retention, and attempted version mutation

## Explicitly excluded

- Editing generated lines inside the approval request
- Automatic Resume selection or creation
- Resume PDF/DOCX export
- Claim-level factual verification beyond source-citation validation
- Tool calling, agents, evaluation datasets, and asynchronous processing

## Tests and executable verification

- `ResumeDraftApprovalServiceTest` passes for successful formatting, cross-user/missing draft, and
  uncited-line rejection.
- `AiCareerPlatformApplicationTests#groundedResumeDraftApprovalCreatesNextImmutableVersionAndEnforcesOwnership`
  passes against PostgreSQL 17 + pgvector in Testcontainers.
- The integration flow verifies cross-user approval returns 404, owner approval returns 201 with version
  2 and citation labels, the prior version remains present, and an update attempt returns 405.

## Completion criteria

- [x] Only the authenticated owner can approve a draft.
- [x] Approval creates a new immutable ResumeVersion for the selected owned Resume.
- [x] Version numbering remains sequential under the existing row lock.
- [x] Every non-empty approved line retains a citation label.
- [x] Existing versions and the stored draft artifact are not mutated.
- [x] Unit and PostgreSQL integration tests pass.

## Next phase

Phase 5 is complete. Phase 6 can expose narrowly scoped, server-authorized backend capabilities as
tools, with explicit approval for mutating operations.
