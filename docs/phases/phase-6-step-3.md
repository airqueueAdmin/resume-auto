# Phase 6 Step 3 - Approved Project Creation Tool

## Status

Complete for this step. Phase 6 is not complete.

## Objective and scope

Extend the Phase 6 tool boundary with Project creation while proving that a tool schema can enforce a
domain-specific rule, not only generic ownership and approval behavior.

## Design reasons and important decisions

- `career.project.create` reuses `ProjectService.create`, keeping active-user checks, normalization, and
  persistence in the existing application layer.
- The tool publishes a strict Project-specific schema with required dates and text fields, length limits,
  unknown-field rejection, and an HTTP/HTTPS-only `repositoryUrl` rule.
- The tool accepts no owner identifier. The authenticated JWT subject remains the only source of ownership.
- Project creation uses the same short-lived, canonical-argument-bound, one-time approval as the other
  mutating tools, so adding a new mutation does not create a weaker authorization path.

## Implemented features

- `career.project.create` in the allowlisted tool catalog
- Typed Project creation arguments and published input schema
- Project field validation, including repository URL protocol validation
- Explicit approval requirement and user-scoped creation through `ProjectService`
- Integration coverage for invalid domain input, missing approval, cross-user approval, and successful
  creation

## Explicitly excluded features

- Project update/delete tools
- JobPosting mutations and bulk tool operations
- LLM function-call parsing, Agent workflow, retries, and evaluation

## Tests and executable verification

- Maven compile passed.
- Full `mvnw test` passes: 66 tests, including PostgreSQL Testcontainers integration tests.
- `ToolRegistryTest` verifies the expanded allowlist and mutability classification.
- The PostgreSQL integration test verifies that an invalid `ftp://` repository URL is rejected before an
  approval is issued, and that a valid Project is created only by the owner presenting the matching token.

## Failures encountered and root causes

No new implementation failure occurred in this step. The established HTTP boundary continues to accept
`Map<String, Object>` and performs application-level Jackson conversion inside the tool adapter, avoiding
the Jackson 3 HTTP converter versus Jackson 2 application model mismatch found in Step 2.

## Completion criteria

- [x] Project creation is exposed only through the allowlisted tool name.
- [x] Project-specific required fields and repository URL protocol are validated.
- [x] The tool never accepts a client-supplied owner ID.
- [x] Mutation requires a matching, unexpired, one-time approval.
- [x] Cross-user approval is rejected.
- [x] Full unit and PostgreSQL integration tests pass.

## Next step

Step 4 should stabilize the provider-neutral tool-call adapter contract and error semantics without
adding an LLM provider, Agent workflow, or evaluation behavior.
