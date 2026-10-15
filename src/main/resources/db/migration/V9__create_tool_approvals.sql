create table tool_approvals (
    id uuid primary key,
    user_id uuid not null references users(id),
    tool_name varchar(100) not null,
    arguments_hash varchar(64) not null,
    token_hash varchar(64) not null unique,
    expires_at timestamptz not null,
    used_at timestamptz,
    created_at timestamptz not null
);

create index idx_tool_approvals_user_created_at
    on tool_approvals (user_id, created_at desc);

create index idx_tool_approvals_expires_at
    on tool_approvals (expires_at);
