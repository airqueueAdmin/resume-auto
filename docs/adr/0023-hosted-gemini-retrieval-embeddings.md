# ADR 0023: Use Hosted Gemini Embeddings for Render Retrieval

## Status

Accepted for the Render deployment profile.

## Context

Career review and resume generation re-index a user's Experience and Project data before retrieval.
The original adapter called local Ollama, which is available in local development but is not running
inside the free Render web service. As a result, the pre-generation career review failed with an
embedding-provider-unavailable error in Render.

## Decision

- Keep Ollama as the default local embedding provider.
- Add a configuration-selected Gemini embedding provider using the backend-only `GEMINI_API_KEY`.
- Use `gemini-embedding-001` with `outputDimensionality=768` in Render.
- Preserve the existing `EmbeddingClient` boundary and batch indexing flow.
- Use retrieval task types: `RETRIEVAL_QUERY` for search queries and `RETRIEVAL_DOCUMENT` for stored
  career evidence.
- Normalize the hosted 768-dimensional vectors before persistence and query search.

## Consequences

- Render no longer requires a separately hosted Ollama endpoint for career review.
- Local and hosted environments use different providers, so vector data is model-bound. A provider,
  model, or dimension change requires a full re-index of all active source documents.
- Career evidence and search queries are sent to Google from the backend in the Render profile, so the
  deployment must follow the product's privacy and provider data-handling policy.
- Provider errors remain mapped to the existing unavailable, rejected, and invalid-response API codes;
  the frontend presents an actionable Korean message during the career review step.
