# Phase 8 - Evaluation

## Status

Complete for the approved offline evaluation-contract scope. The project now has versioned evaluation
cases, deterministic execution, grounding metrics, token/latency reporting, provider pricing inputs, and
human-reviewed support annotations. It does not run live benchmark jobs or persist evaluation reports.

## Objective and scope

Build a reproducible evaluation foundation for retrieval, matching, grounded resume drafting, citation
linkage, unsupported claims, latency, token usage, and estimated cost.

## Design reasons and important decisions

- Evaluation cases are synthetic, Git-versioned fixtures rather than production user data.
- The metric calculator is framework-free and deterministic, with per-case results and macro aggregates.
- Retrieval relevance, citation validity, citation coverage, and factual support are separate metrics.
  Citation linkage alone is not treated as proof of truth.
- Deterministic execution sources are injected into `EvaluationRunner`, so provider comparison does not
  require a live LLM dependency.
- Provider/model pricing is explicit input with currency and effective date. Local Ollama usage can be
  represented with zero cost without pretending local token counts are vendor charges.
- Human-reviewed annotations remain separate from immutable operational AI artifacts and must match the
  observed claim count.
- Reports remain in memory until retention, authorization, and dataset lifecycle requirements justify a
  persistent evaluation store.

## Implemented features

- Three initial evaluation tasks: Retrieval, Matching, and Resume Draft
- Versioned fixture at `src/test/resources/evaluation/phase-8/cases.json`
- `EvaluationMetricCalculator` for recall@K, precision@K, citation validity/coverage, unsupported claim
  rate, latency, and token usage
- `EvaluationRunner` with per-case dataset/provider/model/prompt metadata
- `EvaluationProviderPricing` and `EvaluationCostCalculator` for explicit estimated cost
- Versioned human annotation fixture and claim-count consistency checks
- Offline documentation and tests without new database migrations or API endpoints

## Explicitly excluded features

- Live provider benchmark execution and automatic price retrieval
- Large statistically representative datasets and annotation UI
- Persistent evaluation reports, dashboards, scheduling, and data-retention policies
- Automatic factual truth verification
- Agent behavior and dynamic Tool selection
- Phase 9 production hardening

## Tests and executable verification

- Maven compile passed.
- Full `mvnw test` passes: 81 tests, with 0 failures, 0 errors, and 0 skipped tests.
- PostgreSQL/Testcontainers integration coverage contains 30 tests.
- Evaluation-specific coverage contains 10 tests for dataset contracts, metric calculation, deterministic
  runner behavior, pricing, and human annotations.
- Existing AI, retrieval, ownership, migration, Tool Calling, and workflow tests remain green.

## Failures encountered and root causes

- Early Step 1 test commands exposed PowerShell quoting and AssertJ enum-array typing issues; both were
  test invocation/typing problems and were corrected without changing the evaluation design.
- An initial citation test used a source that was retrieved but not gold-relevant. The metric correctly
  treated it as valid linkage, so the fixture was changed to use a source absent from retrieved context
  for the invalid-citation case.
- No unresolved implementation failure remains.

## Concepts the learner must understand

- Evaluation is a separate measurement system, not an extension of user-owned business data.
- A benchmark must preserve dataset, prompt, provider, model, pricing, and annotation provenance.
- Aggregate metrics are useful summaries but per-case results are required to diagnose regressions.
- Cost, latency, citation linkage, and factual support are related operational signals, not substitutes for
  one another.

## Completion criteria

- [x] Versioned cases exist for retrieval, matching, and resume drafting.
- [x] Deterministic evaluation execution and metric reporting are implemented.
- [x] Citation, unsupported-claim, latency, and token metrics are tested.
- [x] Explicit provider pricing and estimated cost are tested.
- [x] Human-reviewed annotation contracts are versioned and tested.
- [x] Evaluation remains offline, non-persistent, and separated from user data.
- [x] Full unit and PostgreSQL integration tests pass.

## Next phase and known limitations

Phase 9 is production hardening. The current evaluation runner is synchronous and in-memory, and the
fixture is a quality-contract sample rather than sufficient evidence for model selection.
