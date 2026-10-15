# Phase 8 Step 1 - Evaluation Dataset and Metric Contract

## Status

Complete for this step. Phase 8 remains in progress; this Step record does not replace a Phase 8
completion record.

## Objective and scope

Create the first reviewable evaluation dataset and a pure metric calculator for retrieval, grounded
matching, and grounded resume drafting. The step establishes a reproducible offline contract without
calling an LLM, reading production user data, exposing an evaluation API, or persisting evaluation rows.

## Design reasons and important decisions

- Evaluation cases are versioned in Git under test resources and use synthetic stable source IDs. This
  keeps the initial benchmark reviewable and safe to run without a production account.
- Evaluation observations are separate from immutable operational `JobMatch` and `ResumeDraft` artifacts.
  Evaluation measurements must not become business source data.
- Retrieval recall@K and precision@K measure source selection. Citation validity measures whether a
  cited source existed in the retrieved context; citation coverage measures how much of the reviewed
  expected evidence was cited.
- Unsupported claim rate requires a reviewed supported-claim count. Citation presence alone cannot prove
  factual correctness, so the metric does not pretend to detect hallucination automatically.
- Latency and token usage are carried as observation metadata. Monetary cost is deferred because Ollama
  token counts are not charges and hosted-provider rate cards vary by provider, model, and date.
- The metric calculator is framework-free and deterministic so future runners can use it for provider
  comparisons without coupling evaluation to HTTP, persistence, or a model SDK.

## Implemented features

- `EvaluationCase` and `EvaluationTask` contracts for Retrieval, Matching, and Resume Draft cases
- Versioned fixture at `src/test/resources/evaluation/phase-8/cases.json`
- `EvaluationObservation`, `EvaluationMetrics`, and `EvaluationReport` records
- `EvaluationMetricCalculator` for per-case metrics and macro-averaged reports
- Dataset contract test and metric calculation unit tests
- Evaluation documentation at `docs/evaluation/phase-8/README.md`

## Explicitly excluded features

- Live LLM/provider evaluation runs
- Production user data or PII in the fixture
- Evaluation HTTP endpoints, database tables, or asynchronous jobs
- Automatic factual truth verification from citations
- Provider monetary pricing, cost comparison, and dashboarding
- Large statistically representative datasets or human annotation workflows

## Tests and executable verification

- Maven compile passed.
- Targeted evaluation tests passed: 4 tests, 0 failures, 0 errors.
- Full `mvnw test` passes: 75 tests, with 0 failures, 0 errors, and 0 skipped tests.
- PostgreSQL/Testcontainers integration coverage contains 30 tests.
- The dataset contract test verifies three unique cases, one for each initial evaluation task.
- Metric tests verify recall, precision, citation validity, citation coverage, unsupported-claim rate,
  latency, token totals, macro averages, and invalid input rejection.

## Failures encountered and root causes

- The first targeted Maven command passed a comma-separated `-Dtest` value without PowerShell quoting;
  PowerShell parsed the comma and rejected the command before Maven started. The command was rerun with
  the test selector quoted.
- The first dataset test converted `EvaluationTask[]` to `Object[]`, which did not match AssertJ's typed
  varargs method. Passing the enum array directly fixed the test.
- The first citation metric fixture used an unrelated source that was still in the retrieved list, so
  `citationValidity` correctly returned 1.0. The fixture was corrected to use a source absent from the
  retrieved context for the invalid-citation case.

## Concepts the learner must understand

- A dataset is a versioned measurement contract, not a collection of arbitrary examples.
- Retrieval relevance, citation linkage, and factual correctness are different properties and need
  different annotations or metrics.
- Macro averages prevent a large case from silently dominating a small benchmark, while token and
  latency totals expose operational cost.
- Evaluation must remain separate from user-owned source data and immutable AI result artifacts.

## Completion criteria

- [x] A Git-versioned fixture exists for retrieval, matching, and resume-draft tasks.
- [x] The fixture uses synthetic stable identifiers and contains no production user data.
- [x] Per-case retrieval, citation, claim-support, latency, and token metrics are calculable.
- [x] Aggregate macro metrics and token totals are calculable.
- [x] Invalid observations and empty reports are rejected.
- [x] Unit, compile, and full PostgreSQL integration tests pass.

## Next step and known limitations

Phase 8 Step 2 connects this contract to deterministic provider stubs and records dataset/provider/model/
prompt metadata. The current fixture remains a contract sample, not enough evidence for model or provider
selection; broader reviewed cases and pricing are deferred to later steps.
