# Phase 1 Step 5 - Resume and Immutable ResumeVersion

Status: Complete

## Objective

Implement Resume as a user-owned aggregate root and ResumeVersion as an immutable content snapshot.
This step establishes the versioning behavior required before AI-generated resume drafts are added.

## Scope

Included:

- PostgreSQL V3 migration for `resumes` and `resume_versions`
- Resume create, list, read, rename, and soft delete
- Atomic creation of Resume and initial Version 1
- ResumeVersion creation as a new snapshot
- ResumeVersion read and paginated list
- No ResumeVersion update or delete operation
- Sequential version numbering with a pessimistic row lock
- Hibernate `@Immutable` defense for ResumeVersion
- User ownership checks for Resume and nested versions
- Unit and PostgreSQL integration tests

Explicitly excluded:

- JobPosting
- OpenAPI generation
- Docker Compose
- LLM, Embedding, pgvector, RAG, Citation, Tool Calling, Agent, and Evaluation

## Domain and schema decisions

### Resume as aggregate root

`Resume` owns its versions through the `resume_id` foreign key. A Resume cannot be created without
an initial Version 1, so the create operation inserts both rows in one transaction.

The `latest_version_number` field is a small denormalized sequence counter. It avoids scanning the
versions table for every new version and makes the latest version number available in list responses.
It is updated in the same transaction as the new snapshot.

### Immutable version snapshots

`ResumeVersion` contains `version_number`, `content`, and `created_at`, with no update method and no
update/delete endpoint. Hibernate `@Immutable` prevents dirty state changes from being synchronized
to PostgreSQL as a second defense. The business operation for editing is always `POST` to the nested
versions collection.

The Phase 1 content is plain text. This keeps the source contract explicit while allowing Markdown
or other text formats. A structured JSON resume schema will be introduced only when the generation
requirements are known; adding JSONB now would create a schema contract before the AI phase needs it.

### Concurrent version creation

Two concurrent requests must not calculate the same next version number. The service selects the
owning Resume with `PESSIMISTIC_WRITE`, computes the next number, inserts the version, and updates
the counter in one transaction. The unique constraint on `(resume_id, version_number)` is an
additional database safety net.

### Ownership and soft delete

Every Resume and nested ResumeVersion query includes the authenticated `user_id`. A version lookup
also checks the parent Resume is not soft-deleted. Deleting a Resume sets `deleted_at`; versions are
retained but become inaccessible through normal APIs. This preserves future auditability without
allowing deleted resumes to appear in active lists.

## Implemented API

| Method | Endpoint | Result |
| --- | --- | --- |
| `POST` | `/api/v1/resumes` | Creates Resume + Version 1 (`201`) |
| `GET` | `/api/v1/resumes?page=0&size=20` | Paginated Resume metadata |
| `GET` | `/api/v1/resumes/{resumeId}` | Resume metadata and latest version number |
| `PUT` | `/api/v1/resumes/{resumeId}` | Renames Resume metadata |
| `DELETE` | `/api/v1/resumes/{resumeId}` | Soft deletes Resume (`204`) |
| `POST` | `/api/v1/resumes/{resumeId}/versions` | Creates next immutable snapshot (`201`) |
| `GET` | `/api/v1/resumes/{resumeId}/versions?page=0&size=20` | Paginated versions |
| `GET` | `/api/v1/resumes/{resumeId}/versions/{versionId}` | Reads one immutable version |

## Tests and executable verification

`./mvnw.cmd test` result:

- 22 tests executed
- 0 failures
- 0 errors

The integration suite verifies:

- Flyway V1, V2, and V3 migrations
- Hibernate schema validation against PostgreSQL 17
- atomic Resume + Version 1 creation
- Resume list, read, rename, and soft delete
- new Version creation while preserving previous content
- version pagination and sequential numbering
- unsupported Version update returns `405`
- cross-user Resume and nested Version isolation
- deleted Resume and its versions are hidden
- invalid body, UUID, and page-size inputs

Unit tests verify sequential version number rules.

## Failure encountered and root cause

The first immutable-version integration run returned `500` when a `PUT` was attempted on a
ResumeVersion. The resource correctly had no update mapping, but the global exception handler did
not yet map Spring's `HttpRequestMethodNotSupportedException`. The handler now returns a structured
`405 METHOD_NOT_ALLOWED` response.

## Concepts to understand before Step 6

- A Resume is a mutable aggregate root; a ResumeVersion is an immutable historical snapshot.
- Version immutability is an application/API rule backed by ORM and database constraints; no single
  annotation replaces the transaction and authorization design.
- A database row lock is used because version number allocation is a read-modify-write operation.
- Soft-deleting the parent while retaining child snapshots is useful for auditability, but every
  child query must explicitly enforce the parent's active state.
- Plain text is intentionally the Phase 1 source contract. Structured sections, generated drafts,
  and provenance metadata belong to later AI design decisions.

## Completion criteria

- [x] Resume and ResumeVersion tables are owned by Flyway.
- [x] Resume creation atomically creates Version 1.
- [x] Existing versions cannot be updated or deleted through the API.
- [x] New versions preserve previous snapshots.
- [x] Version numbering is sequential and transactionally protected.
- [x] Resume and nested versions are user-isolated.
- [x] Resume soft delete hides the parent and its versions.
- [x] Unit and PostgreSQL integration tests pass.
- [x] No JobPosting or AI implementation was added.

## Next step

Phase 1 Step 6: JobPosting CRUD.
