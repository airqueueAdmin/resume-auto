CREATE TABLE experiences
(
    id           UUID         NOT NULL,
    user_id      UUID         NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    role         VARCHAR(200) NOT NULL,
    start_date   DATE         NOT NULL,
    end_date     DATE,
    description  TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMPTZ,

    CONSTRAINT pk_experiences PRIMARY KEY (id),
    CONSTRAINT fk_experiences_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_experiences_dates
        CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_experiences_user_active_updated
    ON experiences (user_id, deleted_at, updated_at DESC, id DESC);

CREATE TABLE projects
(
    id             UUID         NOT NULL,
    user_id        UUID         NOT NULL,
    name           VARCHAR(200) NOT NULL,
    role           VARCHAR(200) NOT NULL,
    start_date     DATE         NOT NULL,
    end_date       DATE,
    description    TEXT         NOT NULL,
    tech_stack     VARCHAR(1000) NOT NULL,
    repository_url VARCHAR(2048),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at     TIMESTAMPTZ,

    CONSTRAINT pk_projects PRIMARY KEY (id),
    CONSTRAINT fk_projects_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT ck_projects_dates
        CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE INDEX idx_projects_user_active_updated
    ON projects (user_id, deleted_at, updated_at DESC, id DESC);
