# Phase 1 Step 4 - Experience and Project CRUD

Status: Complete

## Objective

Implement the first user-owned career resources without introducing Resume, JobPosting, or AI
features. This step establishes the ownership, pagination, and soft-delete patterns that later
career resources will reuse.

## Scope

Included:

- PostgreSQL V2 migration for `experiences` and `projects`
- Experience CRUD
- Project CRUD
- JWT-subject-based ownership checks
- Soft delete through `deleted_at`
- Paginated collection APIs
- Stable server-side ordering by `updated_at DESC, id DESC`
- Request validation and domain date-range validation
- Cross-user read/update/delete isolation
- Integration and domain unit tests

Explicitly excluded:

- Resume and ResumeVersion
- JobPosting
- OpenAPI generation
- Docker Compose
- LLM, Embedding, pgvector, RAG, Citation, Tool Calling, Agent, and Evaluation

## Domain and schema decisions

### Explicit ownership foreign keys

Both tables contain a non-null `user_id` foreign key to `users`. The API never accepts an owner ID
as authoritative input. The controller extracts the user ID from the validated Access JWT and the
application service scopes every lookup by both resource ID and that user ID.

This makes a cross-user request return the same `404` result as a missing resource. It avoids
leaking whether another user's resource exists.

### Soft delete

Delete updates `deleted_at` and `updated_at` instead of physically deleting the row. Repository
queries include `deleted_at IS NULL`, so deleted records disappear from normal collection and single
resource reads while their source data remains available for future audit/provenance work.

### Pagination

Collection APIs accept `page` and `size`. The shared `PageQuery` limits page size to 100, and the
server owns the sort order. Allowing arbitrary client sort paths would make the API contract harder
to control and could produce unstable page boundaries when records have equal timestamps.

The response is a project-owned `PageResponse`, not Spring Data's `Page` serialization, so the
external JSON contract is explicit. Spring Data repositories provide `Page<T>` and `Pageable` for
the database query. See the [Spring Data repository pagination documentation](https://docs.spring.io/spring-data/commons/reference/repositories/core-concepts.html).

## Implemented API

| Method | Endpoint | Result |
| --- | --- | --- |
| `POST` | `/api/v1/experiences` | Creates an Experience (`201`) |
| `GET` | `/api/v1/experiences?page=0&size=20` | Paginated Experiences |
| `GET` | `/api/v1/experiences/{id}` | Single Experience |
| `PUT` | `/api/v1/experiences/{id}` | Replaces editable fields |
| `DELETE` | `/api/v1/experiences/{id}` | Soft deletes (`204`) |
| `POST` | `/api/v1/projects` | Creates a Project (`201`) |
| `GET` | `/api/v1/projects?page=0&size=20` | Paginated Projects |
| `GET` | `/api/v1/projects/{id}` | Single Project |
| `PUT` | `/api/v1/projects/{id}` | Replaces editable fields |
| `DELETE` | `/api/v1/projects/{id}` | Soft deletes (`204`) |

## Tests and executable verification

`./mvnw.cmd test` result:

- 16 tests executed
- 0 failures
- 0 errors

The PostgreSQL integration suite verifies:

- Flyway V1 and V2 migrations
- Hibernate schema validation against PostgreSQL 17
- Experience and Project create, retrieve, update, and delete
- pagination metadata and size boundaries
- deleted resources are hidden from normal reads
- invalid request fields, date ranges, URLs, UUIDs, and page parameters
- unauthenticated collection access
- cross-user read, update, and delete isolation

Unit tests verify the domain invariant that `endDate` cannot precede `startDate`.

## Failure encountered and root cause

The first compile after adding the career services failed because both services used the shared
`CareerResourceNotFoundException` without importing it. The compiler caught the missing dependency
before runtime; explicit imports fixed the issue and no behavior change was needed.

## Concepts to understand before Step 5

- Authentication identifies the owner; authorization is enforced by owner-scoped repository queries.
- A path resource ID is not sufficient authorization data. The query must combine it with the
  authenticated user ID.
- Soft delete is an application invariant: every normal read query must exclude `deleted_at` rows.
- Offset pagination is simple and appropriate for this MVP, but very deep or frequently changing
  collections may later need keyset/cursor pagination.
- Domain validation belongs in the domain model when the rule must hold regardless of transport;
  request validation handles shape and size constraints at the API boundary. Spring MVC supports
  Bean Validation for controller arguments through `@Valid` and `@Validated`.

## Completion criteria

- [x] Experience and Project tables are owned by Flyway.
- [x] All normal resource queries are scoped by authenticated user ID.
- [x] Create, read, update, and soft delete APIs work.
- [x] Collection APIs are paginated with a bounded page size.
- [x] Cross-user access returns `404` and does not mutate data.
- [x] Validation and domain invariants are tested.
- [x] No Resume, JobPosting, or AI implementation was added.

## Next step

Phase 1 Step 5: Resume and immutable ResumeVersion CRUD.
