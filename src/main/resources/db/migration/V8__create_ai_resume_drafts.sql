CREATE TABLE ai_resume_drafts (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    job_posting_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(100) NOT NULL,
    provider_response_id VARCHAR(200) NOT NULL,
    draft JSONB NOT NULL,
    source_references JSONB NOT NULL,
    retrieved_chunk_count INTEGER NOT NULL,
    input_tokens BIGINT NOT NULL,
    output_tokens BIGINT NOT NULL,
    total_tokens BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_ai_resume_drafts PRIMARY KEY (id),
    CONSTRAINT fk_ai_resume_drafts_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ai_resume_drafts_job_posting
        FOREIGN KEY (job_posting_id) REFERENCES job_postings (id),
    CONSTRAINT ck_ai_resume_drafts_retrieved_chunks CHECK (retrieved_chunk_count > 0),
    CONSTRAINT ck_ai_resume_drafts_input_tokens CHECK (input_tokens >= 0),
    CONSTRAINT ck_ai_resume_drafts_output_tokens CHECK (output_tokens >= 0),
    CONSTRAINT ck_ai_resume_drafts_total_tokens CHECK (total_tokens >= 0)
);

CREATE INDEX idx_ai_resume_drafts_user_job_created
    ON ai_resume_drafts (user_id, job_posting_id, created_at DESC);
