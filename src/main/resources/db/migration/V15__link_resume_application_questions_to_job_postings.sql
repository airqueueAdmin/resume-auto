ALTER TABLE resume_application_questions
    ADD COLUMN job_posting_id UUID;

ALTER TABLE resume_application_questions
    ADD CONSTRAINT fk_resume_application_questions_job_posting
        FOREIGN KEY (job_posting_id) REFERENCES job_postings (id) ON DELETE RESTRICT;

CREATE INDEX idx_resume_application_questions_job_posting
    ON resume_application_questions (job_posting_id)
    WHERE job_posting_id IS NOT NULL;
