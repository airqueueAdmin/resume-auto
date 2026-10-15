# Phase 5 Step 1 - Grounded Resume Draft Artifact

## Status

Complete for synchronous local-provider draft generation. Resume approval is intentionally deferred
to the next Step.

## Objective and scope

Generate a structured resume draft from a saved JobPosting and the authenticated user's indexed
Experience/Project evidence. Persist the result as an immutable AI artifact without modifying a
Resume or ResumeVersion.

## Design reasons and decisions

- The JobPosting remains the source of the target role and is loaded through the existing ownership-
  aware service.
- Retrieval runs before generation. The LLM receives only bounded, user-owned career evidence and
  the saved JobPosting.
- The server assigns citation labels (`C1`, `C2`, ...) and resolves them after generation. Model-
  returned UUIDs are never trusted directly.
- Headline, summary, experience bullets, project bullets, and skills are all represented as cited
  lines. Empty sections are allowed, but every generated line must have at least one valid source.
- Drafts are stored in `ai_resume_drafts` with provider metadata, usage, source snapshots, and the
  structured JSON draft. This is separate from the user-authored `ResumeVersion` table.
- Ollama remains the default provider, so local generation does not require a paid OpenAI API key.

## Implemented features

- `POST /api/v1/ai/generate-resume-draft`
- Versioned `resume-draft-v1` prompt and strict structured-output schema
- Ollama and OpenAI structured resume-draft adapters
- Bounded retrieval context and configurable draft Top-K
- Server-side validation for unknown, missing, and invalid citations
- Immutable `V8__create_ai_resume_drafts.sql` artifact storage
- Ownership and no-evidence handling
- Unit, provider-stub, and PostgreSQL integration coverage

## Structured output shape

The provider returns `headline`, `summary`, `experienceBullets`, `projectBullets`, and `skills`.
Each line has `text` and `citationIds`. The API response replaces citation IDs with resolved source
objects containing source type, source ID, chunk ID, similarity, embedding model, and the source
content snapshot.

## Explicitly excluded

- Approving a draft into a ResumeVersion
- Editing generated lines inside the approval transaction
- Resume PDF/DOCX export
- Tool calling, agents, evaluation datasets, and asynchronous processing
- Claim-level factual verification beyond source-citation validation

## Tests and executable verification

- Full Maven suite: 45 tests passed, 0 failures, 0 errors.
- Flyway applied and Hibernate validated all 8 migrations against PostgreSQL 17 + pgvector.
- Integration tests verify draft endpoint ownership isolation and `RAG_CONTEXT_EMPTY` without
  calling a real provider.
- Ollama and OpenAI HTTP stub tests verify structured draft parsing, citation fields, and usage.
- `ResumeDraftServiceTest` verifies source resolution and rejection of an unknown citation.

## Completion criteria

- [x] A saved JobPosting can trigger grounded resume draft generation.
- [x] Only authenticated user's retrieved evidence is supplied to the provider.
- [x] Every generated line is validated against retrieved citation labels.
- [x] Resolved source snapshots are stored in an immutable draft artifact.
- [x] Resume and ResumeVersion source data are not mutated.
- [x] Provider, migration, ownership, empty-context, and citation tests pass.

## Next Step

Implement explicit user approval that converts a selected draft into a new immutable ResumeVersion,
preserving the existing version numbering and ownership rules.
