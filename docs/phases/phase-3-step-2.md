# Phase 3 Step 2 - Indexing and Filtered Vector Search

## Status

Complete.

## Objective and scope

Create a synchronous indexing use case for the authenticated user's active Experience and Project
records, then expose filtered cosine Top-K retrieval over the derived vectors.

## Design reasons and decisions

- Indexing is explicitly user-scoped from the JWT subject. The request cannot choose another owner.
- The initial chunking strategy stores one normalized source snapshot per Experience or Project. It
  makes provenance unambiguous and keeps the first retrieval flow easy to inspect; finer chunking is
  a later quality improvement.
- Re-indexing replaces only the authenticated user's derived rows. It never mutates or deletes source
  career records.
- The Career UI automatically requests a full re-index after a successful Experience/Project create,
  update, or soft delete. The explicit index button remains as a retry and recovery action.
- Search embeds the query locally, filters by authenticated user and active source rows, supports
  `EXPERIENCE`/`PROJECT` filters, and applies a bounded `topK` of 1 to 20.
- Deterministic ID ordering breaks equal-distance ties and makes tests and debugging repeatable.

## Implemented API

- `POST /api/v1/retrieval/index`
  - Access JWT required
  - Reads active owned Experience/Project rows and replaces that user's embedding chunks
- `POST /api/v1/retrieval/search`
  - Access JWT required
  - Request: `query`, optional `topK`, optional `sourceTypes`
  - Response: similarity, source type, source ID, chunk index, content, and embedding model

## Tests and executable verification

- Full Maven test suite: 36 tests passed, 0 failures, 0 errors.
- Integration coverage proves a user cannot retrieve another user's vector and that a
  `PROJECT`/`EXPERIENCE` source filter is enforced.
- Spring OpenAPI generation includes the retrieval endpoints and bearer security requirement.

## Explicitly excluded

- Retrieval context assembly for an LLM
- Citation validation or claim-to-source verification
- Async indexing, incremental updates, hybrid keyword/vector search, and reranking

## Concepts to understand

- Top-K search ranks derived vectors; it does not itself establish that an LLM statement is true.
- User isolation must be applied in the SQL query, not only in the controller.
- Index freshness is an application concern: the Career UI normally follows source changes with a
  subsequent index operation, while API callers remain responsible for invoking the index endpoint.
