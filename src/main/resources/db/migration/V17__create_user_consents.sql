CREATE TABLE user_consents
(
    id              UUID        NOT NULL,
    user_id         UUID        NOT NULL,
    terms_version   VARCHAR(50) NOT NULL,
    privacy_version VARCHAR(50) NOT NULL,
    accepted_at     TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_user_consents PRIMARY KEY (id),
    CONSTRAINT fk_user_consents_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_user_consents_versions
        UNIQUE (user_id, terms_version, privacy_version)
);

CREATE INDEX idx_user_consents_user_accepted
    ON user_consents (user_id, accepted_at DESC);
