CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE embedding_chunks
(
    id                   UUID         NOT NULL,
    user_id              UUID         NOT NULL,
    experience_id        UUID,
    project_id           UUID,
    chunk_index          INTEGER      NOT NULL,
    content              TEXT         NOT NULL,
    content_hash         VARCHAR(64)  NOT NULL,
    embedding            vector(768)  NOT NULL,
    embedding_model      VARCHAR(200) NOT NULL,
    embedding_dimensions  INTEGER      NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_embedding_chunks PRIMARY KEY (id),
    CONSTRAINT fk_embedding_chunks_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_embedding_chunks_experience
        FOREIGN KEY (experience_id) REFERENCES experiences (id) ON DELETE RESTRICT,
    CONSTRAINT fk_embedding_chunks_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE RESTRICT,
    CONSTRAINT ck_embedding_chunks_one_source
        CHECK ((experience_id IS NOT NULL AND project_id IS NULL)
            OR (experience_id IS NULL AND project_id IS NOT NULL)),
    CONSTRAINT ck_embedding_chunks_index
        CHECK (chunk_index >= 0),
    CONSTRAINT ck_embedding_chunks_dimensions
        CHECK (embedding_dimensions = 768)
);

CREATE UNIQUE INDEX uq_embedding_chunks_experience
    ON embedding_chunks (experience_id, chunk_index)
    WHERE experience_id IS NOT NULL;

CREATE UNIQUE INDEX uq_embedding_chunks_project
    ON embedding_chunks (project_id, chunk_index)
    WHERE project_id IS NOT NULL;

CREATE INDEX idx_embedding_chunks_user_source
    ON embedding_chunks (user_id, experience_id, project_id);

CREATE INDEX idx_embedding_chunks_embedding_hnsw
    ON embedding_chunks USING hnsw (embedding vector_cosine_ops);
