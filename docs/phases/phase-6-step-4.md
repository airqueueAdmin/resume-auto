# Phase 6 Step 4 - Provider-Neutral Tool Call Contract

## Status

Complete for this step. Phase 6 is not complete.

## Objective and scope

Separate the HTTP representation of a tool request from the application boundary that validates,
authorizes, and executes it. This step prepares a future provider adapter without adding an LLM client,
function-call parser, or Agent workflow.

## Design reasons and important decisions

- `ToolCall`, `ToolApprovalCommand`, and `ToolCallResult` are application records using the existing
  application Jackson model. HTTP DTOs are converted at the Controller boundary.
- `ToolCallExecutor` is the single application port for execution. The current implementation remains
  `ToolExecutionService`; a future model provider must call this port instead of receiving direct service
  access.
- Approval consumption now receives the already `ValidatedToolCall`, so the exact canonical arguments are
  validated once and then used for both approval matching and execution.
- Tool application exceptions expose a stable `ToolErrorCode`. The HTTP adapter preserves the existing
  status and error-code contract, while non-HTTP adapters can interpret the same application failure
  categories without depending on Spring MVC.
- Request-shape validation remains a transport concern (`VALIDATION_ERROR` or `MALFORMED_REQUEST`), while
  allowlist/schema/approval failures use the `TOOL_*` codes.

## Implemented features

- Provider-neutral `ToolCallExecutor` application port
- Provider-neutral call, approval-command, and result records
- HTTP-to-application mapping in `ToolController`
- Single validation path before approval consumption and execution
- Stable tool error-code enum and exception contract
- Unit and PostgreSQL integration coverage retained for catalog, ownership, approval, replay, and domain
  validation behavior

## Explicitly excluded features

- LLM provider integration or model-generated tool-call parsing
- Agent planning, dynamic selection, retries, workflow state, or evaluation
- New tool capabilities or external JobKorea integration

## Tests and executable verification

- Maven compile passed.
- Full `mvnw test` passes: 67 tests, including 28 PostgreSQL Testcontainers integration tests.
- `ToolErrorCodeTest` verifies the stable `TOOL_*` application error contract.
- Existing integration tests verify that the HTTP adapter still returns the expected error codes for
  unknown tools, invalid arguments, missing approval, cross-user approval, and replayed approval.

## Failures encountered and root causes

No new implementation failure occurred in this step. The earlier Jackson 3 HTTP converter versus Jackson 2
application model boundary remains isolated in `ToolController` through `Map<String, Object>` conversion.

## Completion criteria

- [x] HTTP DTOs do not define the application execution contract.
- [x] A provider-neutral executor port is used by the HTTP adapter.
- [x] Authorization and schema validation remain inside the application boundary.
- [x] Approval matching uses the exact validated canonical arguments.
- [x] Tool error codes are stable and tested independently of HTTP.
- [x] Full unit and PostgreSQL integration tests pass.

## Next step

Step 5 should add another tool only if it has a clear user workflow and distinct domain validation rule;
otherwise keep the boundary stable and defer further expansion until the Phase 6 completion review.
