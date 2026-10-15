# Phase 2 - First LLM API Integration

## Status

Complete for the direct structured Job Description analysis scope.

## Objective and scope

Connect the existing user-owned `JobPosting` source record to one direct LLM API call. The phase
teaches provider request construction, prompt messages, strict structured output, token usage,
timeouts, bounded retries, and failure handling without introducing embeddings, RAG, agents, or an
AI framework.

## Design reasons and decisions

- The API is `POST /api/v1/ai/analyze-job` with a `jobPostingId`, so the server can load the source
  record and enforce ownership from the authenticated JWT rather than trusting request content.
- The provider boundary is an application `LlmClient` port with an OpenAI Responses API adapter.
  Vendor JSON and HTTP behavior stay in infrastructure and can be replaced or tested independently.
- The request uses a versioned classpath prompt and strict `json_schema` output. The schema is small,
  explicit, and limited to summary, role family, seniority, responsibilities, skills, and keywords.
- Each successful call creates an immutable `ai_job_analyses` row. Source `JobPosting` data is never
  overwritten. The structured result is stored as JSONB, while model, prompt version, provider
  response ID, timestamps, and token usage remain queryable metadata.
- Only network failures, timeouts, HTTP 408/429, and HTTP 5xx responses are retried. Client errors
  and invalid structured responses fail without retrying because repeating them cannot repair the
  request or schema contract.
- API keys are read only from server-side configuration. A missing key is a request-time 503 rather
  than a startup failure, keeping local development and non-AI endpoints available.

The wire contract follows the official OpenAI Responses API reference for JSON Schema text output,
response output text, and usage metadata:
[Create a model response](https://developers.openai.com/api/reference/resources/responses/methods/create).

## Implemented features

- `ai` feature package with API, application, domain, infrastructure configuration, provider, and
  persistence boundaries
- `POST /api/v1/ai/analyze-job` with authenticated-user ownership isolation
- `V5__create_ai_job_analyses.sql` PostgreSQL JSONB migration and immutable JPA entity
- `job-analysis-v1.txt` prompt resource
- OpenAI Responses API HTTP adapter using Bearer authentication and `X-Client-Request-Id`
- Structured output parsing and token usage extraction
- Configurable endpoint, model, timeout, retry count/delay, output budget, temperature, and prompt
  version
- Global errors for unavailable provider, rejected provider request, and invalid provider response
- OpenAPI security metadata for the AI endpoint

## Explicitly excluded

- Embedding API, pgvector, vector search, and retrieval filtering
- RAG context construction and citations/provenance references
- Resume generation or mutation
- Tool calling, agents, workflows, evaluation datasets, and AI frameworks
- Streaming, queues, Redis, rate limiting, and production secret management
- Frontend AI analysis screen

## Tests and executable verification

- `OpenAiResponsesClientTest`: 3 tests passed for successful structured parsing, transient retry,
  and non-retryable provider client errors.
- `AiCareerPlatformApplicationTests`: 21 PostgreSQL/Testcontainers integration tests passed.
- Flyway validated and applied all 5 migrations; Hibernate validated the new JSONB entity mapping.
- Integration coverage verifies the AI migration/table count, cross-user 404 behavior, unauthenticated
  401 behavior, and missing-provider-key 503 behavior.
- The real provider was not called during tests because no API key is committed. The HTTP contract is
  tested with a local HTTP server and the application is ready for a configured key.

## Failures encountered and root causes

- Initial compilation failed because `AiConfiguration.java` was created with a path separator typo.
  The file was corrected; no behavior changed.
- The first application-context run found no `ObjectMapper` bean in this Spring Boot 4.1.1 setup.
  An explicit shared Jackson `ObjectMapper` bean was added because both the provider adapter and
  JSONB conversion need the same serialization contract.
- Two sandboxed Testcontainers attempts could not access Docker's Windows named pipe. The same
  PostgreSQL integration suite passed in an approved Docker-access environment; the failure was
  environmental, not a test or schema failure.

## Concepts to understand before Phase 3

- A provider adapter isolates changing vendor protocols from application use cases.
- Structured output constrains syntax, but it does not prove that extracted facts are correct.
- Token usage is operational metadata for latency/cost analysis, not business truth.
- Retrying only transient failures avoids multiplying invalid requests and provider-side costs.
- Source records and derived AI artifacts have different mutation and audit rules.

## Completion criteria

- [x] Authenticated `POST /api/v1/ai/analyze-job` exists.
- [x] JobPosting ownership is checked with authenticated `user_id`.
- [x] Direct provider call, prompt, strict structured output, and token usage are implemented.
- [x] Timeout, bounded retry, and provider error handling are implemented.
- [x] Successful AI results are stored as immutable artifacts separate from source data.
- [x] Unit and PostgreSQL integration tests pass.
- [x] Embeddings, RAG, agents, and AI frameworks remain excluded.

## Next phase and known limitations

Phase 3 will embed Experience and Project source data and add user-filtered PostgreSQL + pgvector
Top-K retrieval. Phase 2 currently has no analysis-history read endpoint, no live-provider test
credential, and no UI action; these are deliberate follow-up concerns rather than source mutation.
