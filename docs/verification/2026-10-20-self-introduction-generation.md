# Self-introduction generation verification — 2026-10-20

This is a defect investigation and verification record, not a phase completion record.

## Confirmed causes

- Grounding validation ran after the fallback client had returned. A parsed Gemini response with
  missing citations, unknown citations, or invalid sentence text therefore failed without trying
  Ollama. Regression tests reproduced this before the change.
- Gemini responses marked `MAX_TOKENS` were accepted when their JSON happened to be parseable.
  A regression test reproduced this before the change.
- The actual local `llama3.1:8b` ran on CPU at approximately 6.4 output tokens/second. A live fallback
  attempt timed out twice at the shared 120-second request deadline; the Ollama log confirmed both
  cancellations. The loaded prompt contained about 3,051 input tokens and was not context-truncated.
- The previous process on port 8080 had started at 16:53, before the v4 prompt and updated classes
  were built after 17:03. Building files alone did not establish that the running application used
  them. It was replaced with the verified application jar.

## Changes

- Apply the shared answer validator within each provider attempt, and recheck at the application
  boundary. Invalid grounding can trigger Ollama; the fallback must pass identical checks.
- Treat Gemini `MAX_TOKENS` and Ollama `done_reason=length` as incomplete even for parseable JSON.
- Keep Gemini as the primary provider. Give Ollama a separate 300-second deadline, a 900-token
  output budget, and a schema allowing at most three concise sentences. Invoke the fallback once.
- Render inline citation markers on the server from validated sentence-level citation IDs.
- Distinguish empty retrieval (`RAG_CONTEXT_EMPTY`) from explicit model abstention after retrieval
  (`RAG_ANSWER_UNSUPPORTED`). An abstention does not trigger forced generation by another provider.
- Log provider, model, prompt version, token counts, and latency without logging source or answer text.
- Update the existing resume-draft request test to the current `responseFormat.text.schema` contract.
  The contract is documented in the [Gemini GenerateContent reference](https://ai.google.dev/api/generate-content#TextResponseFormat).

## Executed verification

- `./mvnw.cmd -q verify`: **125 tests, 0 failures, 0 errors, 0 skipped**, application jar built.
- `npm run build` in `frontend`: passed. Existing bundle-size and React Router directive warnings
  remain; neither prevented the build.
- `git diff --check`: passed; Git reported existing LF/CRLF conversion notices.
- Existing development PostgreSQL Flyway validation: passed without repair or migration edits.
- Authenticated HTTP tests with PostgreSQL + pgvector and real provider adapters verified citation
  failure fallback, token-limit fallback, accurate provider metadata, invalid fallback rejection,
  model abstention classification, and preview generation without saving the answer.

Live checks used the existing saved question and its linked posting, with 5 active Experience records,
2 Project records, and 7 indexed chunks. The question, sources, and reviewed answer were not changed.

| Check | Result |
| --- | --- |
| Actual Gemini, final code | `gemini-2.5-flash`, v4, 509 answer characters, 3 resolved citations, 3.256 seconds |
| Injected uncited Gemini response, actual Ollama fallback | `llama3.1:8b`, v4, 149 answer characters, 4 resolved citations, 19.840 seconds |
| Runtime application | New jar started on port 8080 at 17:27; health `UP`; Gemini primary, Ollama enabled, v4, Ollama timeout `PT5M`, output budget `900` |

The Ollama result used a loaded model and cached prompt prefix. Its observed latency is not a
guarantee for a cold model or another machine. Citations establish source traceability; the user still
reviews whether each claim is actually supported before saving.
