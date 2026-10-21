# ADR 0013: Company-Specific Self-Introduction Questions

## Status

Superseded by [ADR 0014](0014-link-self-introduction-questions-to-job-postings.md)

## Context

Some companies collect self-introduction answers directly in their own application form instead
of using JobKorea's fixed resume fields. Their questions are explicit and may differ for every
application. A single ResumeVersion text field is not a suitable place to manage these reusable,
question-specific answers.

## Decision

Store each company-specific question and its user-authored answer as a child of the authenticated
user's Resume in `resume_application_questions`. The question API is scoped by both `resume_id` and
the authenticated `user_id`; cross-user access returns `404`.

Saving a question in the Resume screen automatically calls a preview-only AI endpoint. It accepts
the Resume and question, retrieves the user's indexed Experience/Project evidence, and returns a
Korean answer draft with resolved source citations. The AI endpoint does not write the answer.
The user must review, edit, and explicitly save it through the question API.

## Consequences

- One Resume can contain multiple company-specific questions and answers without changing immutable
  `ResumeVersion` snapshots.
- The same source-grounded generation boundary is reusable for company forms outside JobKorea.
- AI generation requires the user's Experience/Project embedding index to be current; a missing
  index returns `RAG_CONTEXT_EMPTY`.
- The answer is currently a reviewable preview, not an immutable AI artifact. Persisted content is
  user-approved source data; storing generation history can be added later if audit requirements
  justify the additional schema and retention cost.
- A failed AI call leaves the saved question available so the user can retry generation.
