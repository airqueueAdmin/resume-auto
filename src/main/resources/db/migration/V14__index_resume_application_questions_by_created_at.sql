DROP INDEX idx_resume_application_questions_resume_updated;

CREATE INDEX idx_resume_application_questions_resume_created
    ON resume_application_questions (resume_id, created_at ASC, id ASC);
