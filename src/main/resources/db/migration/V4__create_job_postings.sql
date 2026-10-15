CREATE TABLE job_postings
(
    id           UUID         NOT NULL,
    user_id      UUID         NOT NULL,
    title        VARCHAR(200) NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    description  TEXT         NOT NULL,
    source_url   VARCHAR(2048),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMPTZ,

    CONSTRAINT pk_job_postings PRIMARY KEY (id),
    CONSTRAINT fk_job_postings_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT
);

CREATE INDEX idx_job_postings_user_active_updated
    ON job_postings (user_id, deleted_at, updated_at DESC, id DESC);
