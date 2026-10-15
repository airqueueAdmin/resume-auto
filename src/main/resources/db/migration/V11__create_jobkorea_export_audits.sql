CREATE TABLE jobkorea_export_audits
(
    id                UUID        NOT NULL,
    user_id           UUID        NOT NULL,
    resume_version_id UUID        NOT NULL,
    template_version  VARCHAR(50) NOT NULL,
    result            VARCHAR(40) NOT NULL,
    file_size_bytes   BIGINT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_jobkorea_export_audits PRIMARY KEY (id),
    CONSTRAINT fk_jobkorea_export_audits_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_jobkorea_export_audits_resume_version
        FOREIGN KEY (resume_version_id) REFERENCES resume_versions (id) ON DELETE RESTRICT,
    CONSTRAINT ck_jobkorea_export_audits_template
        CHECK (template_version IN ('JOBKOREA_STANDARD_V2')),
    CONSTRAINT ck_jobkorea_export_audits_result
        CHECK (result IN ('DOWNLOAD_TRIGGERED', 'GENERATION_FAILED')),
    CONSTRAINT ck_jobkorea_export_audits_file_size
        CHECK (
            (result = 'DOWNLOAD_TRIGGERED' AND file_size_bytes BETWEEN 1 AND 5242880)
            OR (result = 'GENERATION_FAILED' AND file_size_bytes IS NULL)
        )
);

CREATE INDEX idx_jobkorea_export_audits_user_created
    ON jobkorea_export_audits (user_id, created_at DESC, id DESC);

CREATE INDEX idx_jobkorea_export_audits_resume_version
    ON jobkorea_export_audits (resume_version_id, created_at DESC, id DESC);
