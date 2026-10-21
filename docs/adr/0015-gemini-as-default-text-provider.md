# ADR 0015: Use Gemini as the Default Text Generation Provider

## Status

Accepted

## Context

The application already uses a shared LLM boundary for job analysis, matching, and company-specific
self-introduction answers. Ollama was the default, while Gemini was only wired to OCR and resume-draft
generation. That made the configured default inconsistent with the preferred API-key-backed workflow
and left Gemini unable to serve the other structured generation features.

## Decision

Use Gemini `generateContent` as the default text-generation provider by setting `AI_PROVIDER=gemini`.
The Gemini adapter must implement the same application ports as the OpenAI and Ollama adapters for job
analysis, grounded matching, and self-introduction answers. Continue to use the dedicated Gemini resume
draft adapter for resume drafts.

- Keep `GEMINI_API_KEY` server-side and read it from the process environment; do not commit a real key.
- Keep Ollama available as an explicit local option with `AI_PROVIDER=ollama`.
- Keep OpenAI available as an explicit option with `AI_PROVIDER=openai`.
- Keep Ollama embeddings unchanged; `AI_PROVIDER` selects text generation only.
- Set the application, Compose, and example environment defaults consistently to Gemini.
- For Gemini 2.5 self-introduction answers, default `thinkingBudget` to zero so reasoning tokens do
  not consume the structured answer's output-token limit; keep the budget configurable per environment.

## Consequences

Positive:

- All primary structured text-generation features use the Gemini API by default.
- Provider selection remains configuration-driven, and Ollama still supports local-only text processing.
- Existing Gemini key handling and HTTP transport can be reused without an AI framework dependency.

Trade-offs:

- Users must configure `GEMINI_API_KEY` before using Gemini-backed text features.
- Job postings and retrieved career evidence are sent to Google's API under the default configuration;
  users with local-only data requirements must select Ollama.
- Gemini usage is subject to provider quotas, latency, cost, and data-handling terms.
- Ollama remains the embedding provider, so local retrieval indexing still requires the Ollama embedding
  model even when Gemini generates text.
