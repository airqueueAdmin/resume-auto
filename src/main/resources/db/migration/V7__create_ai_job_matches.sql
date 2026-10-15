CREATE TABLE ai_job_matches (
    id UUID NOT NULL,
    user_id UUID NOT NULL,
    job_posting_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(100) NOT NULL,
    provider_response_id VARCHAR(200) NOT NULL,
    analysis JSONB NOT NULL,
    source_references JSONB NOT NULL,
    retrieved_chunk_count INTEGER NOT NULL,
    input_tokens BIGINT NOT NULL,
    output_tokens BIGINT NOT NULL,
    total_tokens BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_ai_job_matches PRIMARY KEY (id),
    CONSTRAINT fk_ai_job_matches_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_ai_job_matches_job_posting
        FOREIGN KEY (job_posting_id) REFERENCES job_postings (id),
    CONSTRAINT ck_ai_job_matches_retrieved_chunks CHECK (retrieved_chunk_count > 0),
    CONSTRAINT ck_ai_job_matches_input_tokens CHECK (input_tokens >= 0),
    CONSTRAINT ck_ai_job_matches_output_tokens CHECK (output_tokens >= 0),
    CONSTRAINT ck_ai_job_matches_total_tokens CHECK (total_tokens >= 0)
);

CREATE INDEX idx_ai_job_matches_user_job_created
    ON ai_job_matches (user_id, job_posting_id, created_at DESC);
