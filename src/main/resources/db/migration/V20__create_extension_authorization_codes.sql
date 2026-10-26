CREATE TABLE extension_authorization_codes
(
    id          UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    code_hash   VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_extension_authorization_codes PRIMARY KEY (id),
    CONSTRAINT uq_extension_authorization_codes_code_hash UNIQUE (code_hash),
    CONSTRAINT fk_extension_authorization_codes_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_extension_authorization_codes_user_id
    ON extension_authorization_codes (user_id);

CREATE INDEX idx_extension_authorization_codes_active_lookup
    ON extension_authorization_codes (code_hash, consumed_at, expires_at);
