# AI Career Platform - Agent Guidance

## Project Purpose

AI Career Platform is a portfolio-grade backend project for expanding a Backend Developer career toward LLM/AI Engineering.

The platform manages a user's resumes, experiences, projects, and job postings. Later phases will analyze job descriptions, retrieve relevant experience with embeddings and pgvector, generate grounded resume drafts, expose backend capabilities as tools, and evaluate AI quality.

The project is designed for learning and interview readiness. Every important technical decision must be explainable in terms of correctness, maintainability, latency, cost, security, and operational trade-offs.

## Development Philosophy

- Do not generate the entire project at once.
- Explain the design reason before implementing each step.
- Implement only the current Phase and Step scope.
- After implementation, run relevant tests and executable checks.
- Explain failures and their root causes instead of hiding them.
- Summarize the concepts that must be understood before moving forward.
- Do not advance to the next Step until the current completion criteria are verified.
- Prefer simple, explicit implementations over premature frameworks or abstractions.

## Approved Architecture

The initial architecture is a Spring Boot Modular Monolith.

```text
API -> Application -> Domain -> Infrastructure
```

Use feature-oriented packages. Avoid a large global `controller`, `service`, or `repository` package that mixes unrelated domains.

The planned domains are:

- `identity`: User and authentication
- `career`: Resume, ResumeVersion, Experience, Project
- `job`: JobPosting
- `ai`: added only when Phase 2 starts
- `retrieval`: added only when embedding and search are implemented
- `evaluation`: added only when evaluation work starts

The AI boundary is a future architectural boundary, not a reason to add unused interfaces or fake providers during Phase 1.

## Phase Roadmap

### Phase 0 - Design

Requirements, User Scenarios, Domain Model, ERD, API resources, AI/RAG architecture, provenance, ADRs, project structure, and Phase 1 scope were approved.

### Phase 1 - Backend Foundation

Build a stable backend without AI:

- Spring Boot project
- PostgreSQL and Flyway
- User
- Authentication and Authorization
- Resume and immutable ResumeVersion
- Experience
- Project
- JobPosting
- Validation and global error handling
- User data isolation
- Pagination for collection APIs
- OpenAPI
- Unit and Integration Tests
- Docker Compose
- README

Phase 1 is complete. The project contains the Spring Boot and PostgreSQL foundation, User /
Authentication, user-owned Experience/Project/JobPosting CRUD, Resume with immutable ResumeVersion
snapshots, runtime OpenAPI/Swagger documentation, and Docker Compose PostgreSQL startup. It does not
contain AI code by design.

### Phase 2 - First LLM API

Implement direct LLM API integration for structured Job Description analysis. Learn Prompt, System/User messages, Structured Output, Token usage, timeout, retry, and error handling.

### Phase 3 - Embedding and Vector Search

Embed Experience and Project source data, store explicit source-linked chunks in PostgreSQL + pgvector, and implement filtered Top-K search.

### Phase 4 - RAG and Matching

Use Job Description retrieval context to produce matching analysis with validated source references and citations.

### Phase 5 - Grounded Resume Generation

Generate Resume drafts from existing user facts only. Every generated bullet must be traceable to source evidence and approved by the user before becoming a ResumeVersion.

### Phase 6 - Tool Calling

Expose narrowly scoped backend capabilities as tools. Server-side authorization and argument validation are mandatory. Mutating tools require explicit user approval.

### Phase 7 - Agent Workflow

Use a bounded deterministic workflow first. Add Agent behavior only where dynamic Tool selection is genuinely needed. Enforce step, timeout, and cost limits.

### Phase 8 - Evaluation

Build datasets and measure extraction, retrieval, matching, citation correctness, hallucination, latency, token usage, and cost.

### Phase 9 - Production Hardening

Consider Redis, asynchronous processing, queueing, streaming, rate limiting, tracing, privacy controls, prompt injection defenses, and AWS deployment based on measured needs.

### Phase 10 - JobKorea Resume Integration

Integrate approved ResumeVersion data with the JobKorea resume workflow. First verify an official
personal-resume API or partnership contract. Without one, provide a user-controlled JobKorea-compatible
export and handoff flow; never collect JobKorea passwords, refresh tokens, cookies, or bypass CAPTCHA/
anti-automation controls. The currently observed upload contract is one PDF/PNG/JPG/JPEG file up to 5MB;
validate that contract and the downstream field mapping before implementing an adapter. Warn users about
unnecessary personal information and require review of AI-generated content before upload.

### Phase 11 - Deployment Packaging and Operability

Package the frontend, backend, and PostgreSQL stack as reproducible containers, then add automated
delivery checks and a measured deployment runbook. Keep cloud deployment, distributed infrastructure,
and image publication out of scope until explicitly authorized and justified by measurements.

### Browser Extension Application Assistance - Authorized Product Track

The Chrome extension is an optional companion client for non-standard company application forms. JobKorea
is the first target site because the existing service already owns the JobKorea-compatible resume review
and export flow. It does not replace the existing web application: direct visits to the service URL keep the current Career,
JobPosting, Resume, ResumeVersion, AI review, and export workflows.

The extension's primary purpose is to assist with a JobKorea posting that the user has not saved in the
service. Read the currently visible posting as ephemeral request context and do not require or automatically
create a JobPosting record. The page posting must not be persisted unless the user invokes a separate,
explicit save action. Do not require a saved ResumeVersion or saved JobPosting selection for page-assisted
answers.

The extension may read the active application page's visible posting, questions, and form fields, show a
compact Career Note panel at the bottom-right of supported JobKorea pages, request a review-only resume
draft or grounded answer from the existing backend using the user's Experience/Project evidence, show
the generated content and its source context for review, and fill only fields explicitly approved by the
user. A page-generated draft is ephemeral and must not create a JobPosting, ResumeDraft, Resume, or
ResumeVersion automatically. Final submission always happens on the target company's site through the
user's own action.

Grounded generation still requires user-owned Experience or Project data in the service and active retrieval
evidence built from that data. The extension must show readiness counts, the detected posting's source,
character counts, and an expandable full-text preview so the user can verify that the page content was read.
It must guide an unprepared user to the service Career pages and the explicit search-data update action;
it must not generate unsupported drafts or answers when no grounded evidence is available.

The extension must not collect or automate passwords, cookies, refresh tokens, CAPTCHA, site login,
automatic submission, or anti-automation controls. It must not automate JobKorea login or upload. Keep
browser permissions minimal, prefer active-tab or user-granted host access, and keep the backend as the
source of truth for user data, AI generation, provenance, and audit records. Do not embed backend secrets
in the extension.

The first extension step is a requirements and field-contract slice: inventory representative non-standard
application forms, define a normalized question/field model, define authentication and backend boundaries,
and verify detection behavior with static test pages. Steps 2-3 add one-time service connection,
ResumeVersion selection, grounded evidence context, and review-only answer proposals with explicit
text-field fill. Step 4 now includes the JobKorea bottom-right panel and semantic role baseline; live
JobKorea measurement is still required. Do not automate final submission.

## Approved Decisions

- Java 25 LTS is the current JDK baseline.
- Spring Boot 4.1.1 is the current framework baseline.
- Maven Wrapper is used so contributors do not need a global Maven installation.
- PostgreSQL is the relational source of truth.
- PostgreSQL + pgvector is the initial vector search choice.
- Career data uses Soft Delete by default.
- Collection APIs use pagination; single-resource APIs do not.
- Access JWT is short-lived and stateless.
- Logout revokes the Refresh Session.
- Already-issued Access JWTs remain valid until expiration; this limitation must be documented.
- User ownership is verified using the authenticated `user_id`, never a client-supplied owner ID.
- User resource queries must be scoped by both resource ID and authenticated user ID.
- ResumeVersion is immutable; edits create a new version.
- AI results are versioned immutable artifacts and are not written directly over source data.
- Source provenance is required for RAG-based output.
- `EmbeddingChunk` must not use an unconstrained `source_type + source_id` polymorphic FK in the initial schema.
- Prompt files and structured-output schemas are versioned in Git initially.
- No AI framework, LLM client, embedding client, fake LLM, Agent, or future abstraction is added before its Phase.
- The Chrome extension is a thin, user-approved application-assistance client; the existing web service remains the primary UI and source of truth.
- Extension-assisted answers must be reviewed before insertion, and final application submission remains manual on the target site.
- Extension authentication uses short-lived, least-privilege access; never store or collect site passwords, cookies, refresh tokens, or backend secrets in the extension.

## Phase 1 Scope Guardrails

During Phase 1, do not implement or add dependencies for:

- LLM API
- Embedding API
- pgvector
- Vector Search
- RAG
- Citation
- Tool Calling
- Agent
- AI Evaluation

Phase 1 may establish general Application/Infrastructure boundaries, but it must not add unused AI ports or fake implementations.

## Data Ownership Rules

- Never accept `user_id` as authoritative input from a request body.
- Obtain the current user from the authenticated security context.
- Scope every user-owned query by authenticated user ID.
- Validate ownership for nested resources before creating relationships.
- Return `404` for resources that do not exist or belong to another user.
- Add tests for cross-user read, update, delete, and relationship creation.

## Versioning and Provenance Rules

- Do not mutate an existing ResumeVersion to represent an edit.
- Create a new version and preserve the previous one.
- Keep user-authored source data separate from derived embeddings and AI output.
- Store source snapshots when provenance is required for later auditability.
- A citation proves which source was referenced; it does not by itself prove that an LLM claim is factually correct.

## Testing Expectations

Tests must cover more than happy paths:

- normal creation, retrieval, update, and soft delete
- invalid input
- missing resource
- unauthenticated request
- unauthorized cross-user request
- database constraints
- version immutability
- pagination boundaries
- migration behavior

Use the same database engine in integration tests as in local development. Do not use H2 as a substitute for PostgreSQL once PostgreSQL work begins.

## Documentation Expectations

Keep README and `docs/` updated as implementation progresses. Document:

- what was built
- why it was built that way
- how to run it
- how to test it
- known limitations
- trade-offs
- changes from the approved architecture

When a design decision changes, add or update an ADR before relying on the new behavior.

### Phase Completion Records

When a Phase is genuinely complete, create one immutable completion record at
`docs/phases/phase-<number>.md`. Each record must include:

- phase objective and scope
- design reasons and important decisions
- implemented features and explicitly excluded features
- tests, executable verification, and results
- failures encountered and their root causes
- concepts the learner must understand
- completion criteria
- next Phase or known limitations

Do not mark an in-progress Phase as complete. Step-level work may be recorded at
`docs/phases/phase-<number>-step-<number>.md` when useful, but it must be clearly labeled
as a Step record and must not replace the final Phase record.

## Current Next Step

Phases 5-10 are complete for their approved scopes. The JobKorea-first browser extension track has
completed authentication, read-only context, review-only answer/fill steps, and the automatic bottom-right
page panel; live semantic adapter measurement and quality evaluation remain next. JobKorea has no supported public personal-resume
write API for individual use, so Phase 10 ends at reviewed file generation and user-controlled upload
handoff. Phase 11 Step 1 adds reproducible full-stack Docker packaging and is complete. Phase 11 Step 2
now has a non-deploying CI quality gate for backend tests, frontend build, Compose validation, container
builds, and vulnerability reporting; its first remote CI run is still pending. Do not publish images,
create cloud resources, add distributed infrastructure, or automate JobKorea login/upload without
explicit authorization.
