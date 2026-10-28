# Database migrations

Flyway owns the application schema. Add immutable versioned migrations here using
`V<version>__<description>.sql` or Java, for example `V1__create_users.sql`.

The current migration history contains the identity, Experience/Project, Resume/ResumeVersion,
JobPosting, immutable AI job analysis, pgvector embedding chunks, immutable grounded job matches, and
immutable grounded resume drafts. New domain changes continue with forward-only migrations.

`V13__create_resume_application_questions.sql` stores user-authored company-specific application
questions and answers separately from immutable `ResumeVersion` snapshots. AI-generated answers are
previewed and require an explicit user save through the question API.

`V14__index_resume_application_questions_by_created_at.sql` replaces the original V13
`(resume_id, updated_at, id)` index with `(resume_id, created_at, id)` to support the question API's
default list order. V13 remains unchanged for databases that already applied it.

`V15__link_resume_application_questions_to_job_postings.sql` links each company-specific question to
one saved JobPosting. The new column remains nullable for questions created before V15; the API
requires a user-owned active posting for new and updated questions, and generation requires a linked
posting.

`V6__create_embedding_chunks.sql` enables the PostgreSQL `vector` extension and stores 768-dimensional
Ollama embeddings. Each chunk has an explicit foreign key to exactly one `experiences` or `projects`
row; the initial implementation stores one searchable source snapshot per source row. The migration
also creates a cosine-distance HNSW index for Top-K retrieval.

`V7__create_ai_job_matches.sql` stores validated RAG matching results separately from source career
data. The JSONB analysis contains resolved citation snapshots, while `source_references` provides a
queryable artifact-level list of the referenced chunks.

`V8__create_ai_resume_drafts.sql` stores citation-validated resume drafts separately from
`ResumeVersion`. Draft generation cannot overwrite user-authored resume content. Phase 5 approval is
an explicit API operation that creates a new `ResumeVersion` from the stored JSON artifact; it does
not require a schema mutation and never updates an existing version.

`V22__encrypt_personal_data.java` applies application-level AES-GCM protection to personal and
derived text columns. JSON artifacts become encrypted text and are parsed only after decryption;
the migration also creates the encrypted-email blind index used by authentication.

`V23__encrypt_employment_status.java` extends the same protection to the employment-status field
and keeps the existing career response contract through an encrypted boolean converter.

`V10__remove_experience_dates.sql` retires employment start/end dates from the Experience API and
domain. Existing database values are retained in nullable legacy columns for backward compatibility;
new Experience records no longer read or write them. Project records retain their dates, so the two
career evidence types intentionally have different structured fields.

`V11__create_jobkorea_export_audits.sql` stores user-owned export metadata for Phase 10. Records are
append-only while retained, then removed by the configured privacy retention job. It references the
immutable ResumeVersion and records only template/result/file-size/time metadata; PDF bytes, profile
fields, filenames, provider credentials, cookies, and upload sessions are excluded.

`V19__add_experience_tech_stack.sql` adds an optional structured tech stack to Experience. Existing
rows remain valid with a null value; users are guided to fill it before resume generation so retrieval
and JobKorea export can use explicit evidence instead of inventing or guessing technologies.

Rules:

- Never edit a versioned migration after it has been applied to a shared environment.
- Add a new migration for every forward schema change.
- Do not use Hibernate `ddl-auto` to create or update the schema.
- Test migrations against PostgreSQL, not an in-memory substitute.
- Keep vector dimensions and embedding model configuration aligned. Changing either requires a
  deliberate migration/re-indexing decision.
