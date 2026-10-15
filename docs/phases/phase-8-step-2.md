# Phase 8 Step 2 - Deterministic Evaluation Runner

## Status

Complete for this step. Phase 8 remains in progress; this Step record does not replace a Phase 8
completion record.

## Objective and scope

Connect the Phase 8 dataset and metric contract to a deterministic execution source. The runner executes
each case, records dataset/provider/model/prompt metadata, calculates per-case metrics, and returns an
aggregate report. Tests use in-memory provider stubs so no live LLM call or production data is required.

## Design reasons and important decisions

- `EvaluationRunner` receives an execution function rather than a model client. This keeps evaluation
  independent of HTTP, persistence, and provider SDKs while allowing deterministic stubs now and an
  offline adapter later.
- Each case result records provider, model, and prompt version. Matching and resume-draft cases can use
  different prompt versions in the same dataset run without losing provenance.
- Duplicate case IDs, missing execution results, blank dataset versions, and incomplete metadata are
  rejected before a report is returned.
- The runner calculates through the same `EvaluationMetricCalculator` used by Step 1; callers cannot
  silently bypass metric definitions by constructing an aggregate manually.
- The report remains in memory. Evaluation output is measurement data, not a new user-owned artifact or
  operational AI result.

## Implemented features

- `EvaluationExecutionMetadata` for provider/model/prompt version
- `EvaluationCaseExecution` for deterministic observation sources
- `EvaluationRunner` for ordered case execution and metric calculation
- `EvaluationCaseResult` and `EvaluationRunReport` with dataset version and aggregate summary
- Deterministic stub test covering Retrieval, Matching, and Resume Draft cases
- Duplicate-case and missing-execution validation

## Explicitly excluded features

- Live Ollama/OpenAI/Gemini benchmark calls
- Evaluation API, persistence, scheduling, or dashboarding
- Human annotation UI and statistically representative dataset expansion
- Provider pricing and monetary cost calculation
- Automatic factual verification beyond the supplied supported-claim annotation

## Tests and executable verification

- Maven compile passed.
- Targeted evaluation tests passed: 6 tests, 0 failures, 0 errors.
- Full `mvnw test` passes: 77 tests, with 0 failures, 0 errors, and 0 skipped tests.
- PostgreSQL/Testcontainers integration coverage contains 30 tests.
- Runner coverage verifies per-task metadata, aggregate token totals, average latency, duplicate case
  rejection, and missing stub execution rejection.

## Failures encountered and root causes

No new implementation failure occurred in Step 2. The deterministic runner tests passed after the
existing Step 1 metric contract was reused.

## Concepts the learner must understand

- Dependency injection of an observation source makes evaluation deterministic without making the
  production application depend on test doubles.
- Dataset version and prompt/provider metadata are necessary to compare reports across changes.
- A per-case metric result preserves outliers that aggregate averages would hide.
- Keeping reports in memory avoids mixing evaluation measurements with immutable business artifacts until
  retention and access rules are designed.

## Completion criteria

- [x] Every fixture case can be executed through a deterministic observation source.
- [x] Provider, model, prompt, and dataset metadata are retained in the report.
- [x] Per-case and aggregate metrics are calculated through the Step 1 calculator.
- [x] Missing and duplicate cases are rejected.
- [x] No live provider, production data, HTTP endpoint, or database table is required.
- [x] Unit, compile, and full PostgreSQL integration tests pass.

## Next step and known limitations

Phase 8 Step 3 should define versioned provider pricing and cost estimation, with an effective date and
currency/unit contract. It should also decide how human-reviewed supported-claim annotations are stored
without placing evaluation data in user-owned AI artifact tables. The current runner is synchronous and
in-memory.
