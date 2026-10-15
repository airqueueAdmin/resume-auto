CREATE TABLE resumes
(
    id                    UUID         NOT NULL,
    user_id               UUID         NOT NULL,
    name                  VARCHAR(200) NOT NULL,
    latest_version_number INTEGER      NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at            TIMESTAMPTZ,

    CONSTRAINT pk_resumes PRIMARY KEY (id),
    CONSTRAINT fk_resumes_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_resumes_latest_version_number
        CHECK (latest_version_number >= 0)
);

CREATE INDEX idx_resumes_user_active_updated
    ON resumes (user_id, deleted_at, updated_at DESC, id DESC);

CREATE TABLE resume_versions
(
    id             UUID        NOT NULL,
    resume_id      UUID        NOT NULL,
    version_number INTEGER     NOT NULL,
    content        TEXT        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_resume_versions PRIMARY KEY (id),
    CONSTRAINT fk_resume_versions_resume
        FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE RESTRICT,
    CONSTRAINT uq_resume_versions_resume_number UNIQUE (resume_id, version_number),
    CONSTRAINT ck_resume_versions_version_number CHECK (version_number >= 1)
);

CREATE INDEX idx_resume_versions_resume_number
    ON resume_versions (resume_id, version_number DESC, id DESC);
