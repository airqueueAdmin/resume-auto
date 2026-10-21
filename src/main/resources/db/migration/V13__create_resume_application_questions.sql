CREATE TABLE resume_application_questions
(
    id           UUID         NOT NULL,
    resume_id    UUID         NOT NULL,
    question     VARCHAR(2000) NOT NULL,
    answer       TEXT         NOT NULL DEFAULT '',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_resume_application_questions PRIMARY KEY (id),
    CONSTRAINT fk_resume_application_questions_resume
        FOREIGN KEY (resume_id) REFERENCES resumes (id) ON DELETE RESTRICT,
    CONSTRAINT ck_resume_application_questions_question_not_blank
        CHECK (length(btrim(question)) > 0)
);

CREATE INDEX idx_resume_application_questions_resume_updated
    ON resume_application_questions (resume_id, updated_at ASC, id ASC);
