# Professional self-introduction drafting and local diagnostics — 2026-10-20

This records a writing-quality and UI change, not a phase completion.

## Implemented behavior

- The default `self-introduction-v5` prompt selects relevant posting requirements and one or two
  concrete career examples, then connects actual actions and recorded results to the role.
- Motivation-and-aspiration questions explicitly cover the professional reason for applying, a
  supporting example, and a feasible future contribution. Administrative posting labels and vague
  suitability claims are discouraged. No invented historical achievement or metric is permitted.
- Retrieved evidence is prioritized by explicit posting terms before vector similarity. Review
  exposed confusion between generic SI work and domain-specific work; a regression test verifies
  that directly matching project evidence precedes a higher-similarity generic SI example.
- Ollama keeps the bounded three-item draft. Its API request specifies an 8,192-token context because
  the expanded writing guidance, evidence, and 900-token output budget no longer fit comfortably in
  the observed 4,096-token default context.
- Multiple sentences grouped under one citation set are rendered individually with that same set.
  Missing/unknown citations and empty or punctuation-only text still fail validation.
- The server derives `diagnosticsVisible` from its Spring environment. The editor stores and renders
  the provider/model/prompt/chunk-count line only when the flag is true. Career citations remain
  available for review.

## Verification

The 39 affected backend tests passed, including authenticated PostgreSQL + pgvector HTTP tests,
provider wire-format tests, citation normalization/rejection tests, and the following profile cases:

| Backend profile | Technical generation line |
| --- | --- |
| `local` | Visible |
| Default `local`, no explicitly active profile | Visible |
| `docker` | Hidden |
| `production` | Hidden |
| `test` | Hidden; also verified through the HTTP response |

The frontend TypeScript/Vite build passed. The tested backend jar was built and started on port 8080;
startup confirmed v5 and the health check returned `UP`.

Live review used the existing saved posting/question and 7 source-linked Career chunks. The first
Ollama sample still focused on past work, so the short-draft instructions were made explicit about
covering both motivation and post-entry plans. A subsequent response grouped sentences within an
item; this exposed the formatting rejection and led to the citation-preserving normalization above.

The live Gemini probe returned a retryable provider failure during this check. Live writing review
therefore used the actual local Ollama model, with a deliberately invalid primary fixture to select
the fallback. This is not a claim that a live Gemini v5 generation passed.

The generated previews were not saved over the user's reviewed answer or Career sources. Citation
validation establishes traceability; semantic support still requires comparison with the source text.
