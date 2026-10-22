ALTER TABLE users
    ADD COLUMN email_verified_at TIMESTAMPTZ;

ALTER TABLE users
    ALTER COLUMN status TYPE VARCHAR(32);

UPDATE users
SET email_verified_at = created_at
WHERE status = 'ACTIVE';

ALTER TABLE users
    DROP CONSTRAINT ck_users_status;

ALTER TABLE users
    ADD CONSTRAINT ck_users_status
        CHECK (status IN ('PENDING_EMAIL_VERIFICATION', 'ACTIVE', 'LOCKED', 'DELETED'));

CREATE TABLE email_verification_codes
(
    id              UUID        NOT NULL,
    user_id         UUID        NOT NULL,
    code_hash       VARCHAR(64) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    failed_attempts INTEGER     NOT NULL DEFAULT 0,
    max_attempts    INTEGER     NOT NULL,
    consumed_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_email_verification_codes PRIMARY KEY (id),
    CONSTRAINT fk_email_verification_codes_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_email_verification_failed_attempts
        CHECK (failed_attempts >= 0 AND failed_attempts <= max_attempts),
    CONSTRAINT ck_email_verification_max_attempts CHECK (max_attempts > 0)
);

CREATE UNIQUE INDEX uq_email_verification_codes_active_user
    ON email_verification_codes (user_id)
    WHERE consumed_at IS NULL;

CREATE INDEX idx_email_verification_codes_expiry
    ON email_verification_codes (expires_at)
    WHERE consumed_at IS NULL;
