# ADR 0016: Use Ollama When Gemini Truncates Self-Introduction Answers

## Status

Accepted

## Context

Gemini 2.5 can stop a structured self-introduction answer with `MAX_TOKENS` before completing its
JSON document. The answer is generated from a saved company question, its linked job posting, and
retrieved career evidence. A truncated primary response cannot be safely stored or shown as a draft.
The application already supports the same grounded self-introduction prompt and JSON shape through
Ollama.

## Decision

When the Gemini self-introduction client reports an incomplete or invalid structured response, or a
retryable provider/network outage, retry the same generation once with the configured Ollama model.
Enable this fallback by default for Gemini and allow operators to disable it with
`AI_SELF_INTRODUCTION_OLLAMA_FALLBACK_ENABLED=false`.

- Fall back for `INCOMPLETE_RESPONSE` (Gemini `MAX_TOKENS`), `INVALID_RESPONSE` (unusable structured
  output), `UNAVAILABLE` (network, timeout, or retryable upstream status), or invalid grounding
  (unknown citation, missing citation, invalid sentence). Both providers must pass the same grounding
  validator. Explicit provider rejection and a valid evidence-insufficient abstention remain visible.
- Reuse the same saved job context, question, and retrieved career evidence for the fallback request.
- Record the provider, model, and prompt version of the provider that successfully generated the
  answer, so Ollama output is not mislabeled as Gemini.
- Keep Gemini's self-introduction thinking budget at zero by default to preserve the output budget for
  the structured answer. The Ollama fallback remains useful if the answer still exceeds that limit.
- Give Ollama a separate `AI_SELF_INTRODUCTION_OLLAMA_TIMEOUT` (default `PT300S`) and output budget
  `AI_SELF_INTRODUCTION_OLLAMA_MAX_OUTPUT_TOKENS` (default `900`). Request at most three concise
  sentences and invoke the fallback once. A CPU-only local verification measured about 6.4 output
  tokens/second and timed out twice with the previous shared 120-second deadline.
- Reject Ollama `done_reason=length` as incomplete, even when its JSON happens to be parseable.

## Consequences

Positive:

- A token-truncated Gemini response can still produce a reviewable answer draft when local Ollama is
  available.
- Fallback behavior is bounded to one secondary provider invocation with configured provider retries.
- Users can review and edit the result before saving it, following the existing answer workflow.

Trade-offs:

- Ollama must be running and have a suitable model available for the fallback to succeed.
- The same job posting and retrieved career evidence are sent to the local Ollama endpoint after a
  Gemini truncation.
- The fallback can add local inference latency; unrelated Gemini failures remain visible to the user.
