# Phase 7 - Bounded Workflow and Agent-Selection Evaluation

## Status

Complete for the approved bounded deterministic workflow scope. No Agent implementation was added
because the evaluated use case did not require dynamic Tool selection.

## Objective and scope

Establish a safe workflow boundary on top of Phase 6 Tool Calling and determine whether an Agent is
needed to choose tools dynamically. The phase covers deterministic orchestration, user ownership,
bounded execution, and the decision gate for future Agent behavior.

## Design reasons and important decisions

- The first workflow is code-owned and deterministic. It collects a user-selected JobPosting,
  Experiences, and Projects through the existing `ToolCallExecutor`.
- Resource selection remains explicit in the request. The system does not infer which user facts should
  be included in an application context.
- The workflow has configurable step, cost-unit, and cooperative deadline limits. It cannot issue
  approvals or invoke mutating tools.
- Dynamic Tool selection was evaluated and deferred. The current use case has no unresolved selection
  problem: the request names the resources and the workflow has a fixed three-tool read path.
- A future Agent requires a concrete intent that cannot be represented by this bounded request, plus a
  separate planner boundary and evaluation data. The roadmap alone is not sufficient justification.

## Implemented features

- `POST /api/v1/workflows/job-application-context`
- Deterministic JobPosting → Experience → Project read sequence
- User-scoped execution through the Phase 6 `ToolCallExecutor`
- Maximum three Experience IDs and three Project IDs per request
- Configurable default limits of seven steps, seven cost units, and five seconds
- Stable workflow errors for invalid arguments, limit exhaustion, and cooperative timeout
- OpenAPI documentation and PostgreSQL integration coverage
- Dynamic Tool selection evaluation and documented deferral decision

## Explicitly excluded features

- LLM-driven tool selection, Agent planning, and autonomous loops
- Dynamic mutation selection or automatic approval issuance
- Retries, parallel execution, asynchronous workflow state, or hard downstream cancellation
- New AI provider dependencies or Phase 8 evaluation datasets
- JobKorea integration and export behavior

## Tests and executable verification

- Maven compile passed.
- Full `mvnw test` passes: 71 tests, with 0 failures, 0 errors, and 0 skipped tests.
- PostgreSQL/Testcontainers integration coverage contains 30 tests.
- Integration coverage verifies successful workflow aggregation, cross-user isolation, duplicate-ID
  rejection, request-size validation, and the workflow route in OpenAPI.
- Unit coverage verifies workflow step/cost overflow and deadline rejection.

## Failures encountered and root causes

- The first deadline unit test used a fixed clock that caused the budget to calculate a later deadline
  than the assertion assumed. A mutable test clock corrected the test so budget creation and deadline
  expiry are tested separately.
- No unresolved implementation failure remains. Cooperative timeout behavior is a documented
  limitation, not a hard-cancellation guarantee.

## Concepts the learner must understand

- Deterministic orchestration should precede Agent behavior so correctness, latency, cost, and security
  have a measurable baseline.
- Tool authorization remains a server-side boundary even when a workflow or future model requests a
  capability.
- User-provided resource IDs express stronger intent than a model-inferred selection.
- A workflow budget limits orchestration scope; downstream providers still require their own timeouts.

## Completion criteria

- [x] A bounded workflow executes through the Phase 6 authorized Tool boundary.
- [x] User ownership is enforced for every selected resource.
- [x] Step, cost-unit, and deadline bounds are implemented and tested.
- [x] Dynamic Tool selection was evaluated against a concrete use case.
- [x] Agent behavior was deferred because it adds no current user value and increases risk.
- [x] Full unit and PostgreSQL integration tests pass.

## Next phase and known limitations

Phase 8 is the next planned phase and should measure the existing AI and workflow behavior with explicit
datasets and metrics. If a future use case demonstrates genuine dynamic Tool selection, it must be
introduced as a separately bounded, read-only capability first. The current workflow does not cancel a
blocking downstream call after its cooperative deadline is exceeded.
