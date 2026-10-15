# Phase 6 Step 2 - Approved Experience Creation Tool

## Status

Complete for this step. Phase 6 is not complete.

## Objective and scope

Prove that the Phase 6 approval boundary protects a real user-data mutation, not only the existing
resume-draft approval flow. This step adds one narrowly scoped Experience creation tool with strict
arguments and no client-supplied owner identifier.

## Design reasons and important decisions

- `career.experience.create` reuses `ExperienceService.create`, so active-user checks, normalization,
  persistence, and domain behavior remain in the existing application service.
- The tool has its own typed argument record and published schema. It intentionally does not include
  `userId`; the authenticated JWT subject is the only owner source.
- The same short-lived, canonical-argument-bound, one-time approval is required before creation.
- The HTTP request uses `Map<String, Object>` at the Spring Boot 4 boundary and converts to the
  application Jackson tree only inside the tool adapter. This avoids mixing Jackson 3 HTTP binding
  with the existing Jackson 2 AI/application model.

## Implemented features

- `career.experience.create` in the allowlisted tool catalog
- Required and optional Experience field validation, including date and length constraints
- Explicit approval requirement for creation
- User-scoped creation through the existing `ExperienceService`
- Integration coverage for missing approval, cross-user approval, successful creation, and response
  serialization

## Explicitly excluded features

- Experience update/delete tools
- Project or JobPosting mutations
- LLM function-call parsing and Agent workflow
- Bulk operations, asynchronous tool execution, and evaluation

## Tests and executable verification

- Maven compile passed.
- Full `mvnw test` passes: 65 tests, including 27 PostgreSQL Testcontainers integration tests.
- The integration test verifies that the created Experience is persisted only after the owner presents
  the matching approval token; missing or cross-user execution is rejected.

## Failures encountered and root causes

- The first HTTP implementation used Jackson 2 `JsonNode` in request records, but Spring Boot 4's HTTP
  converter uses Jackson 3 types. The request failed before controller execution with an internal
  conversion error. The boundary now accepts `Map<String, Object>` and converts it inside the
  application-owned tool adapter; the integration test then passed.

## Completion criteria

- [x] Experience creation is exposed only through the allowlisted tool name.
- [x] Tool arguments reject unknown fields and invalid required values.
- [x] The tool never accepts a client-supplied owner ID.
- [x] Mutation requires a matching, unexpired, one-time approval.
- [x] Cross-user approval and replay are rejected.
- [x] Full unit and PostgreSQL integration tests pass.

## Next step

Step 3 should add another mutation only if it demonstrates a distinct authorization or domain rule.
Otherwise, stabilize the catalog/approval contract and prepare the boundary for a future LLM tool-call
adapter without adding Agent behavior.
