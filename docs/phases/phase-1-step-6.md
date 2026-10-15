# Phase 1 Step 6 - JobPosting CRUD

## Status

Complete.

## Objective

Add a user-owned JobPosting resource without introducing Job Description analysis or any AI
implementation. The resource stores the original job posting data that later phases will analyze.

## Scope

Included:

- PostgreSQL `job_postings` migration
- JobPosting create, list, get, update, and soft delete
- Authenticated-user ownership checks
- Paginated JobPosting collection API
- Request validation and source URL validation
- Unit-level domain behavior through the existing resource pattern
- PostgreSQL Testcontainers integration tests

Explicitly excluded:

- OpenAPI generation
- Docker Compose
- LLM, Embedding, pgvector, RAG, Citation, Tool Calling, Agent, and Evaluation
- Skill extraction or normalized Job Description fields

## Design reasons

JobPosting is a private source record owned by one user. The client never supplies an authoritative
`user_id`; the application obtains it from the authenticated Access JWT subject.

The Phase 1 schema keeps only source fields: title, company name, description, and an optional source
URL. Adding extracted skills or matching fields now would couple the source-of-truth schema to future
LLM behavior. Those derived results belong to later AI artifacts and evaluation records.

The resource uses the same ownership and soft-delete rules as Experience and Project:

- every read and mutation scopes by resource ID and authenticated user ID
- cross-user and missing resources return the same 404-style result
- collections exclude rows with `deleted_at`
- collections are paginated with the shared page-size limit
- the database retains deleted source rows for future audit and provenance work

## Database

Migration `V4__create_job_postings.sql` creates:

- UUID primary key
- non-null `user_id` foreign key to `users` with `ON DELETE RESTRICT`
- non-null title, company name, and description
- nullable source URL
- created, updated, and soft-delete timestamps
- `(user_id, deleted_at, updated_at DESC, id DESC)` ownership/list index

There is no uniqueness constraint on title or source URL because a user may save the same posting
from different sources or revisit a posting later.

## API

| Method | Endpoint | Behavior |
| --- | --- | --- |
| POST | `/api/v1/job-postings` | Create a JobPosting |
| GET | `/api/v1/job-postings?page=0&size=20` | List active owned postings |
| GET | `/api/v1/job-postings/{id}` | Get one active owned posting |
| PUT | `/api/v1/job-postings/{id}` | Replace editable source fields |
| DELETE | `/api/v1/job-postings/{id}` | Soft delete |

The source URL is optional and, when supplied, must use `http` or `https`. Description content is
kept as plain text in Phase 1. It is not parsed, summarized, embedded, or sent to an LLM.

## Tests and verification

`./mvnw test` result:

```text
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The integration suite verifies:

- migration v4 and the `job_postings` table
- create, retrieve, update, list, and soft delete
- pagination metadata
- missing resource behavior
- invalid title and source URL rejection
- unauthenticated collection rejection
- cross-user read, update, delete, and list isolation
- existing identity, Experience, Project, and Resume behavior remains passing

## Core concepts to understand

- A source JobPosting and future AI-derived analysis must remain separate models.
- User isolation is an application query invariant, not merely a controller check.
- A soft-deleted row remains in PostgreSQL but is excluded by every normal repository query.
- Pagination is applied to collections; a single JobPosting response is not paginated.
- Storing the original Job Description makes later AI analysis reproducible and auditable.

## Completion criteria

- [x] JobPosting migration is forward-only and validated by Flyway.
- [x] CRUD endpoints require authentication.
- [x] All queries are scoped to the authenticated user.
- [x] Collection API is paginated and bounded.
- [x] Delete is soft delete.
- [x] Validation, missing-resource, and cross-user cases are tested.
- [x] No LLM, embedding, vector search, RAG, or AI-derived schema was added.

## Next step

Phase 1 Step 7: OpenAPI documentation and API contract exposure.
