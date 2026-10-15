# ADR 0003: Validate RAG Citations Against Retrieved Context

## Status

Accepted

## Context

Phase 4 must compare a Job Description with user-owned career data without allowing the LLM to invent
source IDs or cite records that were not retrieved. The result also needs enough provenance to audit
what evidence was shown at generation time.

## Decision

- Assign server-side labels (`C1`, `C2`, ...) to the ranked retrieval results.
- Ask the LLM to return those labels on strengths, gaps, and recommendations.
- Resolve labels on the server to the retrieved chunk ID, explicit source type/ID, similarity, model,
  and source content snapshot.
- Reject unknown citation labels and uncited strengths/recommendations with a 422 response.
- Persist the resolved analysis and flattened source references as an immutable `ai_job_matches` row.

## Consequences

Positive:

- The model cannot choose an arbitrary user's source ID through the matching contract.
- Citation references are deterministic and can be inspected by an API client or later evaluator.
- Source snapshots preserve audit context across re-indexing and source updates.

Trade-offs:

- A malformed or poorly following local model response fails instead of producing a partial result.
- Citation labels add prompt and response tokens.
- Citation presence proves linkage to retrieved text, not that the statement is factually correct.
- Synchronous retrieval and generation still hold an HTTP request open.

## Rejected alternatives

- Trusting model-generated UUIDs was rejected because the model could hallucinate or cross-reference
  IDs outside the retrieved context.
- Returning only similarity-ranked chunks was rejected because it would not provide a grounded
  matching decision.
- Writing results directly into JobPosting or ResumeVersion was rejected because derived AI output must
  remain immutable and separate from user-authored source data.
