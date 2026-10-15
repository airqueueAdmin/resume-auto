# Phase 8 Evaluation Dataset v1

## Purpose

The initial fixture at `src/test/resources/evaluation/phase-8/cases.json` is a small, Git-versioned
contract for evaluating retrieval, grounded matching, and grounded resume drafting. It uses stable
synthetic source identifiers rather than database UUIDs or personal user data, so it can be reviewed and
run without a production account.

The fixture is deliberately separate from operational AI artifacts. A generated `JobMatch` or
`ResumeDraft` remains an immutable application result; an evaluation observation is a measurement input
and must not be written into those source-of-truth tables.

## Case contract

Each case contains:

- `caseId`: stable identifier used in reports
- `task`: `RETRIEVAL`, `MATCHING`, or `RESUME_DRAFT`
- `query`: representative job or resume task input
- `topK`: retrieval cutoff used by the case
- `expectedSourceIds`: human-reviewed relevant source identifiers
- `notes`: annotation guidance

The first fixture intentionally contains one case for each initial task. It is a contract sample, not a
statistically representative quality benchmark. More cases and independently reviewed annotations are
required before making model or provider decisions.

## Initial metrics

`EvaluationMetricCalculator` computes the following from an observation:

- retrieval recall@K: relevant retrieved sources divided by expected sources
- retrieval precision@K: relevant retrieved sources divided by retrieved sources
- citation validity: cited sources present in the retrieved context divided by cited sources
- citation coverage: expected sources cited divided by expected sources
- unsupported claim rate: claims without human- or rule-supported evidence divided by claims
- latency and input/output/total token usage

Citation validity measures linkage to the retrieved context. It does not prove that a cited statement is
factually true. Unsupported claim rate therefore requires a reviewed `supportedClaimCount`; it is not
inferred from citation presence alone.

Monetary cost is intentionally not calculated in Step 1. Ollama token counts are not a provider charge,
and hosted-provider prices vary by model and date. A later step must introduce versioned provider pricing
before reporting cost comparisons.

## Execution boundary

Step 1 is offline and deterministic. It does not call an LLM, read production user data, expose an HTTP
endpoint, or persist evaluation results. Future evaluation runners must record dataset version, prompt
version, provider/model, latency, token usage, and metric definitions with every report.

The current Step 1 suite contains three fixture cases and four contract/metric tests. It is a schema and
measurement baseline, not a quality claim about any provider or model.

## Deterministic run contract

Step 2 adds `EvaluationRunner`. A caller supplies one deterministic execution source per case, and the
runner records dataset version plus provider, model, and prompt version for every case before calculating
the metrics. The source can be a provider stub in tests or a later offline adapter; it cannot bypass the
metric calculator or silently omit a case. This runner contract is now implemented and tested.

The runner does not persist reports or call a live provider. Its purpose is to make the execution contract
explicit before adding real benchmark orchestration, pricing, or human annotation workflows.

The next step must define provider pricing with a source and effective date, then decide how reviewed
annotations are retained without coupling them to user-owned AI artifacts.

Step 3 implements those contracts without claiming any live provider price: pricing is supplied as an
explicit test/catalog input, and annotations are stored as versioned synthetic fixtures. This keeps
cost reports reproducible and makes the price source/effective date visible in every case result.
