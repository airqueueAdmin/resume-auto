# ADR 0010: Use Gemini as the Default Resume Draft Provider

## Status

Accepted

## Context

The local `llama3.1:8b` Ollama model is available and works for short structured requests, but long
job postings plus retrieved evidence can exceed the local latency budget or produce truncated JSON.
The existing Gemini integration already has a backend-only API key and `generateContent` transport for
OCR. Resume draft generation is an explicit user action from a saved JobPosting, so this flow can use
Gemini as its default while the global provider remains unchanged for job analysis and matching.

## Decision

Use Gemini as the default primary provider for resume drafts through `AI_RESUME_DRAFT_PROVIDER=gemini`.
Keep the global `AI_PROVIDER` for job analysis and matching. When Gemini cannot complete a resume draft,
use the configured global provider as a bounded fallback when it is a different provider. If both are
Gemini, do not repeat the same request as a fallback. `AI_RESUME_DRAFT_PROVIDER=configured` restores
the global provider as the resume-draft primary.

- The fallback uses the existing Gemini `generateContent` endpoint and JSON MIME response mode.
- The backend sends the saved job posting and retrieved, user-owned career evidence; the browser never
  supplies a provider key.
- Provider metadata is selected for the actual successful request, so a Gemini fallback is recorded as
  Gemini rather than being mislabeled as Ollama.
- Request failures caused by invalid provider configuration are not hidden by fallback.
- Users who require local-only processing can set `AI_RESUME_DRAFT_PROVIDER=configured` and use
  `AI_PROVIDER=ollama`. Gemini is the global default per ADR 0015.

## Consequences

Positive:

- Gemini is used for the user-triggered resume generation flow by default.
- Local Ollama remains usable as a configured fallback or explicit primary.
- Existing OCR credentials and transport are reused without adding an AI framework.

Trade-offs:

- Career evidence and job postings leave the local machine during the default Gemini request.
- Gemini usage can incur cost, quota limits, and provider-specific retention/privacy requirements.
- The free allocation may return `503` for quota/capacity exhaustion. The current deployment uses the
  bounded Ollama fallback and manual server-side key replacement; automated key creation, key pooling,
  quota-aware routing, and secret rotation are deferred operational enhancements.
- Fallback remains bounded and does not silently change provider metadata for the saved draft.
