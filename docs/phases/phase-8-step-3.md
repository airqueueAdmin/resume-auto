# Phase 8 Step 3 - Versioned Cost and Human Annotation Contracts

## Status

Complete for this step. Phase 8 is complete for the offline evaluation-contract scope; see the [Phase 8
completion record](phase-8.md).

## Objective and scope

Add reproducible provider cost estimation and versioned human-reviewed claim-support annotations without
mixing evaluation data into operational AI artifacts.

## Design reasons and important decisions

- Pricing is supplied explicitly by provider/model with currency, effective date, and input/output price
  per million tokens. The code does not hardcode or claim a live vendor rate card.
- A report cannot mix currencies, and missing or duplicate provider/model pricing is rejected. This keeps
  cost comparisons auditable rather than silently applying the wrong unit.
- Human annotations use synthetic, versioned fixtures with reviewer and reviewed-at metadata. They are
  separate from `ai_job_matches` and `ai_resume_drafts`.
- An annotation can be applied only when its claim count matches the observation. A reviewer cannot
  silently change the evaluated output shape while marking support.
- Cost results remain in memory in this step. Retention, access control, and a persistent evaluation
  store are deferred until a real benchmark workflow requires them.

## Implemented features

- Provider/model pricing contract with effective date and currency
- Input/output token cost calculation at per-million-token rates
- Per-case and aggregate estimated cost reports
- Mixed-currency and missing/duplicate pricing rejection
- Versioned annotation fixture with reviewer, timestamp, claim count, and support count
- Annotation-to-observation claim-count consistency validation
- Unit and fixture contract tests

## Explicitly excluded features

- Live provider pricing retrieval or automatic rate-card synchronization
- Claim truth verification beyond reviewed support annotations
- Evaluation database tables, dashboards, HTTP endpoints, and scheduled runs
- PII or production user data in evaluation fixtures
- Agent evaluation or JobKorea integration

## Tests and executable verification

- Maven compile passed.
- Phase 8 evaluation tests passed: 10 tests, 0 failures, 0 errors.
- Full `mvnw test` passes: 81 tests, with 0 failures, 0 errors, and 0 skipped tests.
- PostgreSQL/Testcontainers integration coverage contains 30 tests.
- Cost tests verify per-million pricing, effective-date retention, aggregate totals, missing/duplicate
  pricing, and mixed-currency rejection.
- Annotation tests verify fixture loading, reviewed support application, and claim-count mismatch
  rejection.

## Failures encountered and root causes

No new implementation failure occurred in Step 3. The pricing and annotation contracts passed targeted
and full regression verification.

## Concepts the learner must understand

- Token usage is not monetary cost until a versioned provider/model pricing rule is applied.
- Currency and effective date are part of cost provenance, not display-only fields.
- Human support annotations are evaluation labels and must not mutate generated AI artifacts.
- Evaluation correctness depends on matching annotations to the exact observed output shape.

## Completion criteria

- [x] Versioned provider/model pricing can estimate input and output token cost.
- [x] Effective date and currency are retained in every case cost result.
- [x] Missing, duplicate, and mixed-currency pricing are rejected.
- [x] Human-reviewed support annotations are versioned and separated from operational data.
- [x] Annotation application validates claim-count consistency.
- [x] Unit, compile, and PostgreSQL integration tests pass.

## Next phase and known limitations

Phase 9 introduces measured production-hardening boundaries. It should preserve the offline evaluation
separation, and it should not turn the cost catalog into an unverified live pricing service.
