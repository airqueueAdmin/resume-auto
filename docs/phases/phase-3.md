# Phase 3 - Embedding and Vector Search

## Status

Complete for the initial synchronous, local-embedding retrieval scope.

## Objective and scope

Embed Experience and Project source data, store explicit source-linked chunks in PostgreSQL with
pgvector, and implement authenticated user-filtered Top-K vector search.

## Design reasons and important decisions

- Local Ollama is used for both Phase 2 chat and Phase 3 embeddings. This preserves the no-paid-API
  local development path requested for the project.
- PostgreSQL remains the relational source of truth. Embedding rows are derived data and are safe to
  rebuild for one user.
- `EmbeddingChunk` uses explicit Experience/Project foreign keys instead of an unconstrained
  polymorphic reference. This preserves database-level provenance and ownership relationships.
- The first chunking strategy is one source snapshot per active Experience or Project. The schema
  still supports multiple `chunk_index` values for a source when later chunking requirements are
  measured.
- Cosine distance and an HNSW index provide the initial vector search path. Search SQL always scopes
  by authenticated user ID and active source rows before ranking.
- Vector SQL is isolated behind a retrieval repository using `JdbcTemplate`; the domain/application
  layers do not depend on PostgreSQL vector implementation details.

## Implemented features

- `V6__create_embedding_chunks.sql` with pgvector extension, `vector(768)` column, explicit source
  foreign keys, content hash, model metadata, and HNSW cosine index
- Ollama `nomic-embed-text` batch embedding adapter with validation, timeout, bounded retry, and
  global error responses
- Authenticated `POST /api/v1/retrieval/index`
- Authenticated `POST /api/v1/retrieval/search` with Top-K and source-type filtering
- User ownership and active-source filtering in the vector SQL query
- Docker Compose and Testcontainers migration to `pgvector/pgvector:pg17`
- README, migration guidance, OpenAPI description, step records, and ADR documentation

## Explicitly excluded features

- RAG context construction and direct LLM matching from retrieved results
- Citation/provenance claims in generated text
- Resume generation, tool calling, agents, evaluation, queues, and production hardening
- Automatic incremental indexing; callers must re-index after source changes

## Tests and executable verification

- Full Maven suite: 36 tests passed, 0 failures, 0 errors.
- Flyway applied all 6 migrations in PostgreSQL Testcontainers.
- Integration tests verified pgvector storage, user isolation, and source-type filtering.
- Ollama runtime check verified `nomic-embed-text` returns one 768-dimensional vector.

## Failures encountered and root causes

- The first Phase 3 integration run expected the old five-migration schema and failed after V6 was
  correctly added. The test assertions were updated to six migrations and nine tables; the complete
  suite then passed.
- Existing local PostgreSQL containers created from `postgres:17-alpine` cannot apply the `vector`
  extension migration. The Compose image is now `pgvector/pgvector:pg17`; an existing container must
  be recreated without deleting its named volume.

## Concepts the learner must understand

- Embeddings and cosine similarity are retrieval mechanisms, not truth verification.
- The model and dimensions are schema-level compatibility constraints.
- Derived indexes can be rebuilt, while user-authored source rows and provenance must be preserved.
- Database-level ownership filters are required even when the controller already has an authenticated
  principal.
- Synchronous indexing is intentionally simple but holds an application request open during embedding;
  measured latency can justify asynchronous processing in Phase 9.

## Completion criteria

- [x] Experience and Project source data can be embedded locally.
- [x] Embeddings are stored in PostgreSQL + pgvector with explicit source links.
- [x] Authenticated user-scoped Top-K search is implemented.
- [x] Source-type filtering and cross-user isolation are tested.
- [x] Docker/Testcontainers use a pgvector-capable PostgreSQL image.
- [x] Documentation records scope, trade-offs, limitations, and verification.
- [x] RAG and citation generation remain excluded until Phase 4.

## Next phase and known limitations

Phase 4 will retrieve relevant chunks for a Job Description and pass only that bounded context to a
matching prompt, with validated source references. The current implementation uses one chunk per
source and synchronous full-user re-indexing; both choices can be revisited based on measured search
quality and latency.
