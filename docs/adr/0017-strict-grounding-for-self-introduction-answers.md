# ADR 0017: Require Relevant Career Evidence for Self-Introduction Answers

## Status

Accepted

## Context

Self-introduction generation already retrieved user-owned Experience and Project chunks and asked
the model not to invent facts. However, it passed every top-K result regardless of similarity and
accepted an answer when it had any valid citation, even if most sentences had no citation. This let
the model produce generic or weakly grounded prose when retrieval was only loosely related to the
question.

## Decision

- Build the retrieval query from the company question and role title. Keep the full job posting in
  the generation context for tailoring, but do not let a long posting dominate retrieval of candidate
  history.
- Discard retrieved chunks below a configurable cosine-similarity threshold. The default is `0.45`,
  controlled by `AI_SELF_INTRODUCTION_MIN_SIMILARITY`; tune it against measured retrieval quality.
- In prompt version `self-introduction-v4`, request a structured list of Korean sentences, each paired
  with citation IDs for its retrieved Experience/Project evidence. If the evidence cannot answer the
  question, the model must return an empty sentence list.
- Require each structured sentence to contain exactly one sentence and at least one citation ID,
  reject empty text, and resolve every ID against retrieved chunks. The server renders inline
  citation markers after validation, so providers do not need to reproduce citation syntax in free text.
- Treat an empty answer or no sufficiently relevant chunks as insufficient evidence; do not save or
  return generic substitute text.
- Validate the structured answer inside each provider attempt. Unknown citations, uncited sentences,
  invalid sentence text, provider failures, and token truncation can trigger the configured Gemini to
  Ollama fallback. Validate the fallback with the same rules before returning it.
- A syntactically valid JSON response with Gemini finish reason `MAX_TOKENS` is still incomplete.
  Route it to fallback instead of returning a possibly truncated draft.
- Distinguish an empty retrieval result (`RAG_CONTEXT_EMPTY`) from a model abstaining after receiving
  evidence (`RAG_ANSWER_UNSUPPORTED`). A model output-format error does not imply an index failure.

## Consequences

- Weak retrieval produces an actionable error asking the user to add or re-index Experience/Project
  data instead of receiving a fluent but unsupported draft.
- Server-rendered inline references make it easier to compare each sentence with its source in the
  review UI and avoid rejecting otherwise valid answers because a provider omitted citation syntax.
- A citation and a similarity threshold still do not prove semantic entailment. The user must review
  each claim against the displayed source text before saving the answer.
- The threshold can reject valid evidence when embedding similarity is low, so it is configurable and
  should be tuned with retrieval examples before being raised.
