# Phase 7 Step 1 - Bounded Job Application Context Workflow

## Status

Complete for this step. Phase 7 remains in progress; this Step record does not replace a Phase 7
completion record.

## Objective and scope

Introduce the first bounded workflow on top of the Phase 6 `ToolCallExecutor`. The workflow collects a
user-selected JobPosting, Experiences, and Projects into one application context for a later matching or
resume workflow. It is intentionally deterministic and does not let an LLM choose tools.

## Design reasons and important decisions

- The workflow accepts resource identifiers selected by the user and invokes only existing read tools.
  This reuses the Phase 6 allowlist and keeps ownership checks in the existing application services.
- Invocation order is fixed: JobPosting, then Experiences in request order, then Projects in request
  order. The result is therefore reproducible and easy to test and explain.
- Each read costs one unit. The default budget is seven steps, seven cost units, and a five-second
  deadline. The limits are configuration properties so deployment policy can tighten them without a code
  change.
- The deadline is cooperative: the workflow checks it before and after every tool call. This bounds the
  workflow orchestration itself but does not cancel a blocking downstream call.
- The request limits each resource list to three identifiers and rejects duplicate or null identifiers.
  This prevents accidental fan-out before a retrieval or agent policy exists.
- The workflow is read-only. It does not issue approvals, invoke mutating tools, call an LLM, or persist
  derived workflow state.

## Implemented features

- `POST /api/v1/workflows/job-application-context`
- User-selected JobPosting, Experience, and Project aggregation
- Deterministic read-tool sequence through `ToolCallExecutor`
- Configurable step, cost-unit, and cooperative deadline limits
- Stable workflow error codes for invalid arguments, limit exhaustion, and timeout
- Ownership isolation inherited from the existing user-scoped career services
- OpenAPI exposure and PostgreSQL integration coverage

## Explicitly excluded features

- LLM-driven tool selection or Agent planning
- Dynamic branching, retries, parallel tool execution, or asynchronous workflow state
- Mutating tool calls and automatic approval issuance
- Job description analysis, matching, resume generation, or JobKorea export
- Hard cancellation of a downstream provider or database call

## Tests and executable verification

- Maven compile passed.
- `WorkflowBudgetTest` passed for step/cost overflow and deadline rejection.
- Targeted PostgreSQL integration test passed for successful aggregation, cross-user isolation,
  duplicate-ID rejection, and request-size validation.
- Full `mvnw test` passes: 71 tests, with 0 failures, 0 errors, and 0 skipped tests.
- PostgreSQL/Testcontainers integration coverage contains 30 tests.
- OpenAPI integration coverage verifies the workflow route is present.

## Failures encountered and root causes

- The first deadline unit test used a fixed clock already five seconds after the nominal start and then
  created a five-second budget, so the budget correctly calculated a deadline five seconds later and did
  not throw. The test was corrected to advance a mutable test clock after budget creation.
- No implementation failure remained after the correction. The workflow's deadline limitation is
  intentional: checks are cooperative and do not interrupt a blocking tool call.

## Concepts the learner must understand

- A bounded workflow is a code-owned sequence with explicit resource, step, cost, and time limits.
- Reusing an authorized application port is safer than allowing workflow code to call repositories or
  domain services around the security boundary.
- A cooperative deadline protects orchestration logic, while provider-level timeouts are still required
  for external calls.
- Deterministic workflows should be established before introducing dynamic Agent behavior; this gives a
  measurable baseline for later comparisons.

## Completion criteria

- [x] A user-scoped workflow endpoint is exposed.
- [x] The workflow invokes only allowlisted read tools through `ToolCallExecutor`.
- [x] Invocation order and request fan-out are deterministic and bounded.
- [x] Step, cost-unit, and deadline limits are enforced.
- [x] Cross-user resources are rejected by the existing ownership boundary.
- [x] Unit, integration, OpenAPI, and full regression tests pass.
- [x] LLM and Agent behavior remain excluded from this step.

## Next step and known limitations

Phase 7 Step 2 should evaluate whether a concrete use case requires dynamic Tool selection. If no such
use case is demonstrated, keep the deterministic workflow boundary and do not add Agent behavior merely
for abstraction completeness. Before any external provider is introduced, add provider-specific timeout,
retry, and cost policies rather than relying only on this cooperative budget.
