# Phase 1 Step 7 - OpenAPI Documentation

## Status

Complete.

## Objective

Expose the current backend API as a discoverable OpenAPI contract and Swagger UI without changing
business behavior or introducing any AI API.

## Scope

Included:

- springdoc OpenAPI Web MVC UI integration
- API title, version, and description
- Controller tags for Authentication, Users, Experiences, Projects, Resumes, and Job Postings
- JWT Bearer security scheme
- Protected resource documentation markers
- Public OpenAPI JSON, YAML, and Swagger UI endpoints
- Integration tests for generated documentation and security boundaries

Explicitly excluded:

- Docker Compose
- LLM, Embedding, pgvector, RAG, Citation, Tool Calling, Agent, and Evaluation
- Generated client SDKs
- Static checked-in OpenAPI specification files
- AI endpoint schemas

## Dependency decision

The project uses:

```xml
org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1
```

Springdoc documents that its 3.x line supports Spring Boot 4 and provides the Web MVC UI starter.
The version is explicitly pinned instead of using a floating or latest version so dependency changes
remain reviewable and reproducible.

## Design reasons

The API contract is generated at runtime from the actual Spring MVC controllers, validation
annotations, and a small number of explicit OpenAPI annotations. This avoids maintaining a second
manual specification that can drift from the implementation.

The JWT security scheme is defined once in `OpenApiConfiguration`. Protected controllers declare the
`bearerAuth` requirement; the public Authentication controller does not. This makes the distinction
between public token lifecycle endpoints and protected user data visible in Swagger UI.

The documentation endpoints are public, but business endpoints remain protected by Spring Security.
This allows developers to inspect the contract without weakening data access authorization.

## Configuration

`application.yml` defines:

- `/v3/api-docs` as the JSON contract path
- `/swagger-ui.html` as the Swagger UI path
- method sorting for operations
- alphabetical tag sorting

`SecurityConfiguration` permits only the documentation paths needed by springdoc:

- `/v3/api-docs/**`
- `/v3/api-docs.yaml`
- `/swagger-ui/**`
- `/swagger-ui.html`

No application data or JWT is included in the generated document.

## Verification

The full Maven test suite passed:

```text
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

The integration test verifies that:

- `/v3/api-docs` returns `200`
- the contract contains Authentication, Resume, and JobPosting paths
- the `bearerAuth` security scheme is present
- `/swagger-ui/index.html` returns `200`
- protected business APIs still return `401` without an Access JWT

Local profile verification also returned:

```text
Actuator health: UP
OpenAPI JSON: 200
Swagger UI: 200
Unauthenticated JobPosting API: 401
```

## Core concepts to understand

- OpenAPI describes an HTTP contract; it does not implement authorization.
- Swagger UI is a client of the generated contract, not a replacement for API tests.
- Public documentation endpoints and protected business endpoints are separate security decisions.
- Runtime generation reduces specification drift but means the generated contract should be reviewed
  as part of API changes.
- DTO validation annotations contribute to the published schema, but complex business invariants may
  still require explicit descriptions or tests.

## Completion criteria

- [x] Spring Boot 4 compatible springdoc dependency is pinned.
- [x] OpenAPI JSON and Swagger UI are available locally.
- [x] JWT Bearer authentication is represented in the contract.
- [x] Protected resource controllers declare the security requirement.
- [x] Documentation paths are public while business APIs remain protected.
- [x] Integration and local executable checks pass.
- [x] No AI API or AI dependency was added.

## Next step

Phase 1 Step 8: Docker Compose for repeatable local PostgreSQL startup.
