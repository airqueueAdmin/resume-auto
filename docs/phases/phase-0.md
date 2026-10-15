# Phase 0 - Design Record

Status: Complete and approved

## Objective

Define the product scope, domain model, database boundaries, API resources, AI architecture,
RAG/provenance design, ADRs, project structure, and Phase 1 implementation boundary before
writing application code.

## Product Direction

AI Career Platform is a portfolio-grade backend for expanding a Backend Developer career toward
LLM/AI Engineering. It manages resumes, experiences, projects, and job postings, then gradually
adds grounded AI analysis, retrieval, resume generation, tool calling, agent workflows, evaluation,
and production observability.

The project is designed for understanding and interview readiness. Every important technical
choice must be explainable through correctness, maintainability, latency, cost, security, and
operational trade-offs.

## Requirements Prioritization

### Must Have

- User signup, login, authorization, and user data isolation
- Resume and immutable ResumeVersion management
- Experience and Project management
- JobPosting management with pagination for collections
- Soft delete for career data
- PostgreSQL, Flyway, validation, consistent error responses, and integration tests
- Short-lived stateless Access JWT
- Refresh Session persistence and revoke-on-logout behavior
- Direct LLM API integration for structured Job Description analysis
- Embedding and user-filtered vector search
- RAG matching with source citations
- Grounded resume generation that cannot invent unsupported experience
- Tool calling with server-side authorization
- Bounded deterministic workflow before dynamic Agent behavior
- Evaluation dataset and metrics for retrieval, matching, hallucination, citations, latency, tokens,
  and cost
- Logging, metrics, tracing, token/cost tracking, and security controls

### Should Have

- Reranking
- Redis caching
- Async processing and queueing
- Streaming responses
- Rate limiting
- AWS deployment and operational dashboards
- Expanded document ingestion and resume export formats

### Later

- PyTorch or Python/FastAPI services
- Fine-tuning or model training
- Multi-agent orchestration
- Complex workflow engines
- Dedicated vector database migration if PostgreSQL + pgvector becomes insufficient

## Core User Scenarios

The approved scenarios are:

- User signup and login
- Experience registration and retrieval
- Project registration and retrieval
- Resume creation and immutable version creation
- Job Posting registration and retrieval
- Job Description analysis
- Experience matching through retrieval
- Grounded Resume generation and improvement

Each scenario must be implemented and documented as:

`Input -> Application Logic -> Database -> AI Call -> Output -> Error Cases`

## Domain Model Decisions

The initial domain model contains:

- `User`
- `RefreshSession`
- `Resume`
- `ResumeVersion`
- `Experience`
- `Project`
- `JobPosting`
- `EmbeddingChunk`
- `AIRun`
- `AIStep`
- `AIOutput`
- `Citation`

`RefreshSession` was added because logout revokes a server-side session while Access JWTs remain
valid until their short expiry. AI artifacts remain immutable and versioned separately from source
career data.

`EmbeddingChunk` must not use an unconstrained `source_type + source_id` polymorphic foreign key.
PostgreSQL cannot enforce referential integrity across arbitrary tables through that pattern, and
JPA mappings become ambiguous. The implementation will use explicit source relationships or a
source snapshot/provenance table with real foreign keys.

## Data and Security Rules

- Every user-owned query is scoped by authenticated `user_id` and resource ID.
- A client-provided owner ID is never authoritative.
- Missing and cross-user resources return the same 404-style resource result.
- Nested resource relationships verify ownership before creation.
- Career data is soft-deleted by default.
- Collection APIs are paginated; single-resource APIs are not.
- ResumeVersion is immutable; edits create a new version.
- Access JWTs are short-lived and stateless.
- Logout revokes Refresh Session records.
- Already-issued Access JWTs remain valid until expiry.

## AI and RAG Architecture

The planned AI boundary is:

```text
Spring Boot API
  -> AI Application Layer
  -> LLM Provider Adapter
  -> Embedding Provider Adapter
  -> Retrieval Service
  -> PostgreSQL + pgvector
```

Provider adapters isolate vendor SDKs and make model/provider changes testable. Prompt files and
structured output schemas are versioned in Git initially. `AIRun` represents one user-visible AI
operation; `AIStep` represents individual model, retrieval, reranking, or tool steps. Token usage,
latency, provider metadata, and estimated cost are recorded at the AI run/step boundary.

The initial RAG flow is:

```text
Source Experience/Project
  -> EmbeddingChunk
  -> user-filtered similarity search
  -> Retrieval Result
  -> LLM context
  -> AI Output
  -> Citation
```

Every generated resume bullet must be traceable to one or more source experiences/projects. A
citation identifies the referenced source; it does not alone prove factual correctness, so output
validation and evaluation remain necessary.

## Approved ADRs

- ADR-001: Modular Monolith
- ADR-002: PostgreSQL + pgvector
- ADR-003: Direct LLM API
- ADR-004: Deterministic Workflow before Agent
- ADR-005: JSONB for AI Output
- ADR-006: Source of Truth and Provenance

## Phase 1 Boundary

Phase 1 builds the non-AI backend foundation:

- Spring Boot
- PostgreSQL and Flyway
- User and Authentication
- Resume and ResumeVersion
- Experience and Project
- JobPosting
- Validation and global errors
- User data isolation
- OpenAPI
- Unit and integration tests
- Docker Compose

Phase 1 excludes LLM, embeddings, pgvector, vector search, RAG, citations, tool calling, agents,
and AI evaluation.

## Completion Criteria

Phase 0 was complete when the requirements, scenarios, domain model, ERD, resource design, AI/RAG
architecture, provenance rules, ADR list, project structure, and Phase 1 boundary were reviewed
and approved before implementation.

## Next Phase

Phase 1 starts with the backend foundation. The current repository has completed Step 1 and Step 2;
the Phase 1 final completion record will be created only after all Phase 1 steps are finished.
