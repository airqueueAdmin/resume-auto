# Phase 4 Step 1 - Grounded Job Matching and Citation Validation

## Status

Complete.

## Objective and scope

Use the Phase 3 vector search results as bounded context for a Job Description matching request. The
LLM may describe only retrieved career evidence, and the server must resolve and validate every
returned source reference before persistence.

## Design reasons and decisions

- `POST /api/v1/ai/match-job` accepts a saved JobPosting ID so ownership is checked from the
  authenticated JWT before retrieval or LLM work.
- The server assigns short citation labels (`C1`, `C2`, ...) to the retrieved chunks. The model
  returns labels instead of generating UUIDs, which makes the contract practical for a local model.
- The server maps labels back to immutable chunk/source metadata and rejects unknown labels or
  uncited strengths/recommendations. Gaps may have no citation when the source data does not show a
  requirement.
- The context is capped by `AI_MATCH_MAX_CONTEXT_CHARACTERS` and retrieval count by
  `AI_MATCH_TOP_K` to keep latency and prompt size bounded.
- Matching output is stored as a new `ai_job_matches` artifact. It includes resolved source content
  snapshots and never overwrites JobPosting, Experience, Project, or ResumeVersion data.

## Implemented features

- `LlmMatchClient` provider boundary for Ollama and OpenAI structured output
- Versioned `job-match-v1` prompt and strict matching JSON schema
- Strength, gap, and recommendation findings with citation IDs
- Resolved `SourceCitation` objects containing chunk ID, source type, source ID, content snapshot,
  similarity, and embedding model
- `V7__create_ai_job_matches.sql` and immutable JPA artifact
- `RAG_CONTEXT_EMPTY`, `RAG_CITATION_INVALID`, `RAG_FINDING_INVALID`, and
  `RAG_FINDING_UNCITED` error responses

## Tests and executable verification

- Ollama matching adapter test verifies request context, schema, citation label parsing, and usage.
- `RagMatchingServiceTest` verifies unknown citation rejection and valid citation resolution.
- PostgreSQL/Testcontainers integration verifies V7 migration, matching endpoint ownership isolation,
  and empty-context behavior without calling a real provider.
- Full Maven suite: 40 tests passed, 0 failures, 0 errors.

## Explicitly excluded

- Resume draft generation or ResumeVersion mutation
- Multi-step agents, tools, queues, streaming, reranking, or hybrid search
- Automated factual correctness evaluation; a citation proves referenced evidence, not truth
