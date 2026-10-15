ALTER TABLE experiences
    DROP CONSTRAINT ck_experiences_dates;

ALTER TABLE experiences
    ALTER COLUMN start_date DROP NOT NULL;

-- Existing values are retained for backward compatibility, but the Experience API and domain
-- no longer read or write these legacy columns.
