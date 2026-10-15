# Phase 1 - Backend Foundation

## Status

Complete.

## Objective

Build a stable, production-oriented backend foundation without AI behavior. The phase establishes
authentication, user-owned career data, PostgreSQL persistence, validation, testing, API
documentation, and repeatable local infrastructure.

## Scope delivered

- Spring Boot 4.1.1 with Java 25 and Maven Wrapper
- PostgreSQL 17 and Flyway migrations
- User signup, login, Access JWT, Refresh Session rotation, and logout revocation
- Stateless short-lived Access JWT behavior documented
- Experience CRUD
- Project CRUD
- Resume and immutable ResumeVersion snapshots
- JobPosting CRUD
- Soft Delete for career data
- Pagination for collection APIs
- Authenticated-user ownership checks and cross-user isolation
- Validation and global error responses
- OpenAPI JSON and Swagger UI
- PostgreSQL Testcontainers integration tests
- Docker Compose local PostgreSQL service
- README, AGENTS.md, migration notes, and step completion records

## Architecture

```text
HTTP API
  -> Application Services
  -> Domain Entities
  -> Spring Data JPA Repositories
  -> PostgreSQL
```

Flyway is the only schema owner. Hibernate runs with `ddl-auto: validate`. User-owned queries scope
by the authenticated JWT subject and resource ID. Career rows use Soft Delete. ResumeVersion is
immutable and editing creates a new row.

## Important decisions

### Authentication

Access JWTs are short-lived and stateless. Logout revokes the persisted Refresh Session; an already
issued Access JWT remains valid until expiration. Refresh token plaintext is returned only to the
client while the database stores a hash.

### Ownership

The client never supplies an authoritative `user_id`. Services derive it from the authenticated JWT
and apply it to every user-owned repository query. Missing and cross-user resources return the same
404-style response to reduce resource enumeration.

### Resume versioning

Resume metadata can change, but ResumeVersion content is an immutable snapshot. Initial Version 1 and
later sequential versions are created transactionally. A row lock and unique constraint protect the
version sequence.

### JobPosting as source data

JobPosting stores the user-provided title, company, description, and optional source URL. It does not
store extracted skills or matching results. Later AI-derived data must be modeled separately from this
source of truth.

### OpenAPI

The API contract is generated at runtime from controllers, validation annotations, and explicit
OpenAPI metadata. Public documentation endpoints do not make business endpoints public; Spring
Security still protects user data APIs.

### Local infrastructure

Docker Compose manages PostgreSQL only. Running Spring Boot through Maven keeps Java debugging simple
while still making the stateful dependency reproducible.

## Explicitly excluded

- LLM API integration
- Prompt management
- Structured LLM output
- Embeddings and pgvector
- Vector Search and RAG
- Citation and provenance implementation
- Tool Calling and Agent workflows
- Evaluation datasets and pipelines
- Redis, queues, streaming, and production tracing
- AWS deployment

## Tests and executable verification

Final Maven verification:

```text
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The integration suite uses PostgreSQL 17 through Testcontainers and verifies Flyway, JPA startup,
Actuator, authentication, career CRUD, pagination, Soft Delete, validation, ownership isolation,
ResumeVersion immutability, and OpenAPI endpoints.

Local verification also confirmed:

- PostgreSQL health/readiness succeeds
- all four migrations apply
- application starts with the local profile
- `/actuator/health` returns `UP`
- `/v3/api-docs` returns `200`
- Swagger UI returns `200`
- protected APIs reject unauthenticated requests with `401`

## Known limitations

- The default Compose password is for local development only.
- Host port `5432` forwarding can conflict with local Docker Desktop or PostgreSQL state; the
  `POSTGRES_HOST_PORT` override was verified successfully with port `55435`.
- No TLS, secret manager, production network policy, or database backup policy exists yet.
- Access JWT revocation is not immediate by design; expiry is the revocation boundary.
- OpenAPI is runtime-generated and not committed as a static artifact.
- Error response documentation is foundational and may need richer per-operation response metadata.
- Resume content is plain text in Phase 1; future AI schemas must preserve source provenance.

## Phase completion criteria

- [x] Backend foundation runs with Java, Spring Boot, and Maven Wrapper.
- [x] PostgreSQL schema is migration-owned and validated against PostgreSQL.
- [x] Authentication and user data isolation are implemented.
- [x] Required non-AI domain resources are available.
- [x] Soft Delete and pagination rules are enforced.
- [x] Validation and global error handling are tested.
- [x] OpenAPI and Swagger UI are available.
- [x] Docker Compose provides repeatable local PostgreSQL startup.
- [x] Unit and integration tests pass.
- [x] No AI implementation was added before Phase 2.

## Next phase

Phase 2: direct LLM API integration for `POST /api/v1/ai/analyze-job`. The first AI step will focus
on provider calls, prompt messages, structured output, timeout, retry, token usage, and error
handling before embeddings or RAG are introduced.
