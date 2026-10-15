# ADR 0001: Use Local Ollama as the Default Phase 2 Provider

## Status

Accepted

## Context

The Phase 2 analysis endpoint needs a direct LLM provider for structured Job Description analysis.
The project is also intended to run locally without requiring a paid API subscription or exposing a
provider secret during development.

## Decision

Keep the `LlmClient` application port and support two infrastructure adapters:

- `ollama`: default local provider at `http://localhost:11434/api/chat`
- `openai`: optional provider selected explicitly with `AI_PROVIDER=openai`

Both adapters use the same prompt, JSON Schema, timeout, bounded retry, response validation, and
immutable analysis artifact flow. Provider-specific HTTP and response details remain outside the
application layer. Image OCR is a separate Gemini integration and is documented in ADR 0004. Gemini
is also an opt-in fallback for resume draft generation when the primary provider times out or returns
an invalid structured response; that boundary is documented in ADR 0010.

## Consequences

Positive:

- Local Job Description analysis does not require OpenAI API billing or an API key.
- Provider switching is configuration-driven and testable.
- The application can record usage metadata for both providers.

Trade-offs:

- Local quality and latency depend on the installed Ollama model and hardware.
- Ollama does not return an OpenAI-style response ID, so the adapter stores a synthetic local ID.
- Phase 3 embeddings must make an explicit provider choice if local-only operation is required.

## Rejected alternatives

- Making OpenAI mandatory would block local development for users without API billing.
- Adding an LLM framework would add dependencies before they are needed and hide the provider wire
  contract being learned in Phase 2.
