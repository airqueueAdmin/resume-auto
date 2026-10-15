# Phase 1 Step 3 - User and Authentication

Status: Complete

## Objective

Implement the first identity boundary without implementing career-domain CRUD or AI features.
The step establishes the authentication behavior that later user-owned resources will rely on.

## Scope

Included:

- `users` and `refresh_sessions` PostgreSQL tables
- Flyway V1 migration
- User signup and duplicate-email handling
- Password hashing through Spring Security's delegating password encoder
- Login with short-lived stateless Access JWT
- Persisted Refresh Session with SHA-256 token hash
- Refresh token rotation
- Existing Access JWT remains usable until expiry after logout, by design
- Logout by revoking the Refresh Session
- Authenticated `GET /api/v1/users/me`
- Authentication and validation error responses
- Integration tests against PostgreSQL 17 Testcontainers

Explicitly excluded:

- Experience, Project, Resume, ResumeVersion, and JobPosting CRUD
- Pagination and career-resource soft delete
- LLM, Embedding, pgvector, RAG, Citation, Tool Calling, Agent, and Evaluation
- Docker Compose, which belongs to the later Phase 1 infrastructure step

## Design reasons

### Access JWT

The Access JWT is short-lived and stateless. The API validates its signature and standard claims
without querying a session table on every request. This keeps the request path simple and makes the
trade-off explicit: logout cannot invalidate an already-issued Access JWT before it expires.

### Refresh Session

Refresh tokens are opaque random values rather than self-contained JWTs. Only their SHA-256 hash is
stored in PostgreSQL. A refresh request revokes the current session and creates a new session, so
the same refresh token cannot be reused successfully after rotation. The token lookup uses a
pessimistic write lock inside the transaction so concurrent refresh requests cannot both rotate the
same session successfully.

### Password storage

The application stores only a password hash. The encoder is delegated to Spring Security so the
stored value carries its algorithm identifier and can be upgraded later without changing the API.

### Schema ownership

Flyway owns table creation and Hibernate runs with `ddl-auto: validate`. This prevents an ORM startup
from silently changing production schema.

## Implemented API

| Method | Endpoint | Result |
| --- | --- | --- |
| `POST` | `/api/v1/auth/signup` | Creates a user and returns `201` |
| `POST` | `/api/v1/auth/login` | Returns Access JWT and Refresh token |
| `POST` | `/api/v1/auth/refresh` | Rotates Refresh Session and returns new tokens |
| `POST` | `/api/v1/auth/logout` | Revokes the supplied Refresh Session and returns `204` |
| `GET` | `/api/v1/users/me` | Returns the user identified by the authenticated JWT subject |

The authenticated user ID comes from the validated JWT subject. It is not accepted from a request
body or path as an ownership authority.

## Tests and executable verification

`./mvnw.cmd test` result:

- 10 tests executed
- 0 failures
- 0 errors

The integration suite verifies:

- application context and Hibernate schema validation
- Flyway V1 and PostgreSQL connectivity
- Actuator health
- signup, login, and current-user retrieval
- duplicate signup (`409`)
- invalid credentials (`401`)
- unauthenticated protected endpoint (`401`)
- refresh token rotation and replay rejection
- logout revocation while an already-issued Access JWT remains valid
- invalid signup input (`400`)

## Failure encountered and root cause

The first test compilation failed because Spring Boot 4 uses Jackson 3 packages for its Jackson
starter. The error response writer initially imported `com.fasterxml.jackson.databind.ObjectMapper`;
the correct dependency type in this project is `tools.jackson.databind.ObjectMapper`. A second test
failure came from the old Step 2 assertion that expected zero domain migrations; it was updated to
verify the V1 identity migration and its two tables.

## Concepts to understand before Step 4

- A stateless Access JWT and a stateful Refresh Session solve different lifecycle problems.
- A JWT signature proves token integrity, not that the user is currently allowed to use the system;
  the API still validates expiry, issuer, and user status when appropriate.
- Refresh token rotation limits replay, but production systems also need rate limiting, secure
  transport, client-side storage decisions, and session cleanup.
- Password hashing is one-way; the server verifies a password by running the same password encoder,
  not by decrypting a stored password.
- Authentication (who the caller is) is separate from authorization (whether that caller can access
  a resource). Career-resource ownership checks are the next step.

## Completion criteria

- [x] User signup and login work against PostgreSQL.
- [x] Passwords are never stored as plaintext.
- [x] Access JWT is short-lived and stateless.
- [x] Refresh Session is persisted as a hash and can be rotated/revoked.
- [x] Protected current-user endpoint rejects missing/invalid authentication.
- [x] Validation and duplicate-email errors have stable API responses.
- [x] Integration tests pass with PostgreSQL Testcontainers.
- [x] No AI implementation or premature AI abstraction was added.

## Next step

Phase 1 Step 4: Experience and Project CRUD, including authenticated ownership checks, soft delete,
pagination, validation, and cross-user access tests.
