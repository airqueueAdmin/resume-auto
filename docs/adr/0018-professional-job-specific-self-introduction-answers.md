# ADR 0018: Write Professional Answers That Connect Career Evidence to the Saved Role

## Status

Accepted

## Context

The v4 prompt established sentence-level source traceability and reliable provider fallback, but its
writing instructions mostly constrained factuality. The resulting prose could list past tasks without
explaining why those tasks matter for the specific job or directly answering the application question.

## Decision

- Advance the default prompt to `self-introduction-v5`. Plan from the question's intent and two or
  three explicit posting requirements, then select the strongest supporting Experience/Project facts.
- Within the owned, similarity-filtered retrieval results, prioritize distinct explicit posting-term
  matches before vector similarity. Use at most 32 Latin technical terms, exact token boundaries, and
  skip generic words. This keeps directly matching domain/technology evidence ahead of generic SI
  history without adding another model call or discarding the remaining accepted evidence. The
  overlap score is a ranking heuristic, not proof of a claim's correctness.
- Write a coherent Korean application answer: a direct role-relevant opening, concrete responsibilities
  and actions with recorded outcomes, and a bounded explanation of how that experience can contribute
  to the saved role. Adapt the structure to motivation, achievements, collaboration, or other question
  types instead of imposing the same template on every answer.
- Use formal, precise prose and technical terms in the context of actual work. Avoid keyword lists,
  unsupported superlatives, invented company values, inferred personal motivations, and fabricated
  metrics or outcomes. A future contribution is explicitly prospective and grounded in demonstrated
  experience; it is not a claim of already meeting an unproven requirement.
- Retain the structured sentence/citation contract and the shared grounding validator. Aim for a
  substantive primary draft within the existing output budget. The local Ollama fallback keeps its
  measured three-sentence budget and compresses the same role-fit argument.
- When a provider puts multiple complete sentences in one cited item, split its text for rendering
  and carry that item's unchanged citation IDs to each resulting sentence. Do not reject a usable
  evidence block solely because the provider grouped sentences. Missing/unknown citations and empty
  text remain errors; this formatting normalization does not establish semantic entailment.
- Set the self-introduction Ollama request context to 8,192 tokens. Live v5 inputs reached 3,874 tokens,
  leaving too little room for the existing 900-token output budget in its default 4,096-token context.
  The [Ollama API context option](https://docs.ollama.com/faq#how-can-i-specify-the-context-window-size)
  allows this bounded request-specific setting; larger contexts require more memory.
- The API derives `diagnosticsVisible` from the backend Spring `local` profile, including the default
  local profile when no profile is active. The frontend renders provider/model/prompt/chunk metadata
  only when that flag is true. Source evidence remains available for the user's review in every profile.

## Consequences

- Writing quality improves through evidence selection, specificity, and logical connection to the job.
  Factual source requirements and explicit user review continue to apply.
- No extra LLM analysis call is added, so the change does not introduce a second provider round trip.
- Prompt/schema changes still require a backend restart. Explicit older prompt-version environment
  overrides continue to select their requested version.
- Technical generation metadata is visible during local development while other profiles present
  the answer and its reviewable evidence.
