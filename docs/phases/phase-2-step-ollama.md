# Phase 2 Step - Local Ollama Provider

## Status

Complete. This is an extension step for the completed Phase 2 direct LLM integration.

## Objective

Run structured Job Description analysis locally through Ollama so local development does not require
an OpenAI API key or paid API usage.

## Design decisions

- Keep the application-level `LlmClient` port unchanged as the provider boundary.
- Select `ollama` or `openai` through `AI_PROVIDER`; default to `ollama` for local development.
- Keep provider HTTP and JSON details in infrastructure adapters.
- Use Ollama's `/api/chat` endpoint with `stream: false` and the same Job Analysis JSON Schema used
  by the OpenAI adapter.
- Store Ollama prompt and generation counts in the existing token usage columns. These counts are
  operational metadata, not a local provider bill.
- Generate a local synthetic provider response ID because Ollama does not return an OpenAI-style
  response ID.
- Preserve timeout, bounded retry, invalid-response handling, immutable analysis artifacts, and
  authenticated JobPosting ownership checks.

## Implemented

- `AiProperties` for provider-neutral AI settings
- `OllamaProperties` for local endpoint and model
- `OllamaChatClient` with structured JSON Schema output
- Runtime provider selection in `AiConfiguration`
- `llama3.1:8b` as the default local model
- Ollama adapter tests for successful parsing, usage extraction, retry, and non-retryable errors
- README and `.env.example` local setup instructions

## Local setup

Ensure Ollama is running and the configured model exists:

```powershell
ollama pull llama3.1:8b
ollama list
```

Start the backend normally. No `OPENAI_API_KEY` is required when `AI_PROVIDER` is omitted or set to
`ollama`.

To switch back to OpenAI explicitly:

```powershell
$env:AI_PROVIDER = "openai"
$env:OPENAI_API_KEY = "your-key"
```

## Verification

- `OllamaChatClientTest`: 3 tests passed
- `OpenAiResponsesClientTest`: 3 tests passed
- PostgreSQL integration suite: 21 tests passed
- Full Maven suite: 32 tests passed, 0 failures, 0 errors
- Local `ollama list` confirmed `llama3.1:8b` is installed

## Limitations

- Ollama quality and latency depend on the local model and available CPU/GPU memory.
- Ollama local token counts do not represent monetary cost.
- Embeddings, pgvector, and RAG remain Phase 3 and later work.
- The current adapter uses synchronous, non-streaming generation.

## Next step

Phase 3 will add local or provider-backed embeddings and filtered vector search. The embedding
provider should be selected explicitly before implementing retrieval so that local-only operation
remains possible.
