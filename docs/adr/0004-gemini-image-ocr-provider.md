# ADR 0004: Use Gemini for Job Posting Image OCR

## Status

Accepted

## Context

Image-only job postings need OCR during import preview. Local Ollama vision models added a large
model download and materially higher CPU inference latency, while the normal text LLM and embedding
workloads still need to remain available through Ollama.

## Decision

Use the Gemini API as the image OCR provider, independently of `AI_PROVIDER`:

- `AI_PROVIDER` selects text generation independently of OCR; see [ADR 0015](0015-gemini-as-default-text-provider.md)
  for the current default and the Ollama/OpenAI alternatives.
- `GEMINI_API_KEY` authenticates backend-only OCR requests.
- Each image is downloaded and sent as one Gemini `generateContent` request.
- Successful image results are combined in source image order; a failed image does not discard other
  results.
- The API key is never accepted from a browser request.

The OCR adapter uses inline image data for the bounded single-image requests. The backend rejects
private image hosts and limits downloaded images to 8 MiB before encoding them.

## Consequences

Positive:

- No local vision model is required, downloaded, or loaded into Ollama.
- OCR latency is independent of the local text LLM hardware.
- Per-image failures are isolated and the original image preview remains available.

Trade-offs:

- OCR requires Gemini API access and may incur provider usage costs or quota limits.
- Image bytes are sent to the external Gemini service, so deployment privacy requirements must be
  reviewed before production use.
- Multiple images still produce multiple sequential OCR requests; asynchronous import can be added
  later if an upstream request timeout remains lower than the total OCR duration.
