# Phase 7 Step 2 - Dynamic Tool Selection Evaluation

## Status

Complete. The evaluation concludes that the current application-context use case does not require
dynamic Tool selection or Agent planning. Phase 7 is complete for the approved bounded-workflow scope;
see the [Phase 7 completion record](phase-7.md).

## Objective and scope

Evaluate whether the first Phase 7 workflow needs an LLM or Agent to select tools dynamically. The
evaluation covers the existing `job-application-context` workflow, its request contract, available Tool
Catalog, security boundary, and bounded execution policy.

## Evaluation

| Question | Finding | Decision |
| --- | --- | --- |
| Does the user intent require interpretation before choosing a tool? | No. The request already contains the JobPosting, Experience, and Project IDs. | Keep selection in the API request. |
| Can the workflow's tool order vary based on data? | No. The useful context is reproducibly collected as JobPosting, Experiences, then Projects. | Keep a fixed sequence. |
| Would an Agent choose among materially different capabilities? | No. The workflow needs only three existing read tools. | Do not add an Agent router. |
| Are mutations or approvals selected dynamically? | No. The workflow is read-only and must not issue approvals. | Keep mutation authorization outside the workflow. |
| Is there a measurable benefit from dynamic selection now? | No. It would add latency, token cost, prompt-injection exposure, and a larger authorization surface without reducing user input. | Defer dynamic selection. |

## Design decision

Phase 7 keeps the deterministic `job-application-context-v1` workflow as the execution baseline. Any
future dynamic Tool selection must first present a concrete user intent that cannot be represented by an
explicit bounded request. If that condition is met, a later step must add a separate read-only planner
boundary, a strict allowlist, per-step authorization, and evaluation data before enabling Agent behavior.

The current workflow therefore does not receive an LLM client, prompt, Agent framework, tool-selection
loop, retry policy, or workflow persistence. This keeps the Phase 6 authorization boundary and Phase 7
budget controls easy to audit.

## Explicitly excluded features

- LLM-driven tool selection and function-call parsing
- Autonomous Agent loops or recursive planning
- Dynamic mutation selection or automatic approval issuance
- Prompt-injection defenses for an external Agent input channel
- Phase 8 evaluation datasets and production hardening

## Tests and executable verification

- The Phase 7 Step 1 targeted workflow integration test passed.
- The full Maven suite passes: 71 tests, with 0 failures, 0 errors, and 0 skipped tests.
- PostgreSQL/Testcontainers integration coverage contains 30 tests.
- The existing Tool Registry and workflow tests verify the allowlist, read-only workflow path, ownership
  isolation, duplicate-ID rejection, request bounds, and step/cost/deadline enforcement.

## Failures encountered and root causes

No new implementation failure occurred. The evaluation intentionally stopped before adding dynamic Agent
behavior because the current request contract already supplies the information needed for deterministic
tool selection.

## Concepts the learner must understand

- Agent behavior is justified by unresolved tool-selection uncertainty, not by the presence of an LLM in
  the architecture roadmap.
- Explicit user-selected identifiers provide stronger authorization intent than inferred resource
  selection.
- A deterministic workflow is a measurable baseline for future Agent experiments.
- Deferring an abstraction is an architectural decision: it reduces token cost, latency, security
  surface, and operational failure modes until a concrete need appears.

## Completion criteria

- [x] A concrete Phase 7 workflow use case was evaluated.
- [x] Dynamic Tool selection was rejected for a documented, testable reason.
- [x] The bounded workflow remains the only Phase 7 execution path.
- [x] Phase 6 authorization and approval boundaries are unchanged.
- [x] Existing unit and PostgreSQL integration tests pass.

## Next phase and known limitations

Phase 8 may introduce evaluation datasets and metrics for the existing deterministic and AI-assisted
features. It must not be used as a reason to add Agent behavior without a concrete dynamic-selection
use case. The current workflow still has cooperative rather than hard cancellation semantics for a
blocking downstream call.
