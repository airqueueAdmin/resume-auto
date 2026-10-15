# Phase 3 Step 1 - Embedding Storage and Local Adapter

## Status

Complete.

## Objective and scope

Add the first embedding boundary and a PostgreSQL schema that can store source-linked vectors
without introducing RAG or an external paid embedding provider.

## Design reasons and decisions

- Ollama `nomic-embed-text` is the default embedding provider so local development does not require
  OpenAI API billing or an API key.
- The application requests 768-dimensional embeddings in batches through Ollama `/api/embed`.
- `embedding_chunks` has explicit nullable foreign keys to `experiences` and `projects`, plus a
  database check that exactly one source is present. This keeps provenance and referential integrity
  enforceable by PostgreSQL.
- Vector persistence uses `JdbcTemplate` because the current Spring Data JPA model does not need to
  expose vendor-specific vector types. Vector SQL remains isolated in the retrieval infrastructure.
- Cosine distance is used for semantic similarity, with an HNSW index prepared for Top-K search.

## Implemented features

- `V6__create_embedding_chunks.sql`
- `vector(768)` storage, model/dimension metadata, content hash, timestamps, and ownership
- Explicit Experience/Project foreign keys and source-specific uniqueness indexes
- Ollama embedding client with batch requests, dimension validation, timeout, and bounded transient
  retries
- Embedding provider error mapping for unavailable, rejected, and malformed responses
- `OLLAMA_EMBEDDING_*` configuration and `.env.example` documentation
- `pgvector/pgvector:pg17` for Docker Compose and PostgreSQL Testcontainers

## Tests and executable verification

- `OllamaEmbeddingClientTest`: successful batch parsing, transient retry, and invalid-dimension
  rejection passed.
- PostgreSQL integration tests applied all 6 Flyway migrations using `pgvector/pgvector:pg17`.
- Ollama runtime verification returned one embedding with 768 dimensions for `nomic-embed-text`.

## Explicitly excluded

- RAG prompt construction, citations, matching analysis, and resume generation
- Automatic background indexing, queues, and incremental change capture
- Multi-provider embedding switching

## Concepts to understand

- An embedding is a derived numeric representation; the source text remains the system of record.
- The embedding model and vector dimension are part of the storage contract.
- A source-linked foreign key is stronger than a generic `source_type + source_id` convention because
  PostgreSQL can validate the relationship.
