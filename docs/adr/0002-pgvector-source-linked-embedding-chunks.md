# ADR 0002: Store Explicit Source-Linked Embedding Chunks in pgvector

## Status

Accepted

## Context

Phase 3 needs local, no-cost embedding and semantic retrieval over user-owned Experience and Project
records. The project already uses PostgreSQL as its source of truth and must preserve source
provenance for later RAG and citation work.

## Decision

- Use the official pgvector-enabled PostgreSQL image for local and integration environments.
- Enable the PostgreSQL `vector` extension through Flyway and store `nomic-embed-text` vectors as
  `vector(768)`.
- Store each derived chunk with explicit nullable `experience_id` and `project_id` foreign keys,
  constrained so exactly one is populated.
- Isolate vector SQL in a retrieval repository using `JdbcTemplate` and use cosine distance with an
  HNSW index for the initial Top-K query.
- Use local Ollama for embedding requests and rebuild only the authenticated user's derived rows.

## Consequences

Positive:

- No OpenAI API key or API billing is required for local indexing and search.
- PostgreSQL enforces source existence and makes provenance joins explicit.
- The same database stores source facts and their searchable derived representation.
- HNSW leaves room for efficient search as the user's career data grows.

Trade-offs:

- The vector dimension is coupled to the selected embedding model.
- Re-indexing is currently synchronous and replaces all derived rows for one user. The Career UI
  triggers this operation after a successful Experience/Project save or delete. The API endpoint
  remains available for operational recovery without exposing a routine manual action to users.
- A pgvector-specific image is required; the plain PostgreSQL image cannot create the extension.
- `JdbcTemplate` requires carefully reviewed SQL because JPA does not abstract the vector column here.

## Rejected alternatives

- A generic `source_type + source_id` reference was rejected because PostgreSQL could not enforce the
  referenced table or preserve a real foreign key.
- H2 was rejected because it does not represent the PostgreSQL vector extension used in development.
- A paid hosted embedding API was rejected for the local-first, no-additional-billing requirement.
- An AI/vector framework was rejected because the Phase 3 learning goal is the provider and SQL
  boundary itself.
