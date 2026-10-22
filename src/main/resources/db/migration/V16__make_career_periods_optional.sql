ALTER TABLE experiences
    ADD COLUMN currently_employed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE experiences
    ADD CONSTRAINT ck_experiences_dates
        CHECK ((start_date IS NULL OR end_date IS NULL OR end_date >= start_date)
            AND (NOT currently_employed OR end_date IS NULL));

ALTER TABLE projects
    ALTER COLUMN start_date DROP NOT NULL;

ALTER TABLE projects
    DROP CONSTRAINT ck_projects_dates;

ALTER TABLE projects
    ADD CONSTRAINT ck_projects_dates
        CHECK (start_date IS NULL OR end_date IS NULL OR end_date >= start_date);
