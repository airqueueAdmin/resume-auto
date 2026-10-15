# ADR 0005: Explicit Approval for Grounded Resume Drafts

## Status

Accepted

## Context

Phase 5 produces a citation-grounded resume draft from a saved JobPosting and retrieved, user-owned
career evidence. A generated artifact must not silently become user-authored resume content: the user
must be able to inspect it first, and an existing `ResumeVersion` must remain immutable.

## Decision

- Expose `POST /api/v1/ai/resume-drafts/{draftId}/approve` with only the target `resumeId` in the
  request body.
- Resolve the draft by both draft ID and authenticated user ID. A draft belonging to another user is
  indistinguishable from a missing draft and returns 404.
- Read and format the server-stored grounded draft; never accept replacement content from the approval
  request.
- Preserve citation labels in the generated version content so each approved line remains traceable to
  the immutable draft's source snapshots.
- Reuse `ResumeVersionService` for active-user checks, target-resume ownership, pessimistic row locking,
  sequential version numbering, and immutable snapshot creation.
- Keep the `ai_resume_drafts` row as the derived artifact and never mutate or delete it during approval.

## Consequences

Positive:

- Approval is an explicit user action and has a narrow, auditable API contract.
- Concurrent approvals cannot create duplicate version numbers for the same Resume.
- Previous ResumeVersions remain readable and unchanged.
- Citation labels survive the transition from structured draft JSON to plain-text ResumeVersion content.

Trade-offs:

- ResumeVersion content is plain text, so detailed source metadata remains in the approved draft
  artifact rather than a normalized foreign-key column.
- A user must choose a target Resume before approval; automatic Resume creation is intentionally out of
  scope.
- Approval is synchronous and does not provide editing, PDF/DOCX export, or claim-level factual
  verification.

## Rejected alternatives

- Accepting edited content in the approval request was rejected because it would bypass server-side
  grounding and provenance validation.
- Updating the latest ResumeVersion in place was rejected because versions are immutable snapshots.
- Adding a draft foreign key to `resume_versions` was deferred; the existing artifact JSON plus citation
  labels provide provenance without changing the Phase 5 schema.
