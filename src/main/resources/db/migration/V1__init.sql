CREATE TABLE user_storage (
    user_id        UUID PRIMARY KEY,
    username       VARCHAR(32)  NOT NULL,
    quota_bytes    BIGINT       NOT NULL,
    used_bytes     BIGINT       NOT NULL DEFAULT 0,
    reserved_bytes BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (used_bytes >= 0 AND reserved_bytes >= 0 AND used_bytes + reserved_bytes <= quota_bytes)
);

CREATE UNIQUE INDEX uq_user_storage_username ON user_storage (lower(username));

CREATE TABLE user_file (
    id             UUID         PRIMARY KEY,
    owner_id       UUID         NOT NULL REFERENCES user_storage (user_id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    size_bytes     BIGINT       NOT NULL DEFAULT 0,
    received_bytes BIGINT       NOT NULL DEFAULT 0,
    content_type   VARCHAR(128),
    status         VARCHAR(16)  NOT NULL DEFAULT 'UPLOADING' CHECK (status IN ('UPLOADING', 'COMPLETE')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_user_file_name ON user_file (owner_id, lower(name));
CREATE INDEX idx_user_file_owner ON user_file (owner_id, status);

CREATE TABLE share (
    id               UUID         PRIMARY KEY,
    code             VARCHAR(6)      NOT NULL UNIQUE,
    owner_id         UUID,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at       TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_share_expires ON share (expires_at);

CREATE TABLE share_file (
    id             UUID         PRIMARY KEY,
    share_id       UUID         NOT NULL REFERENCES share (id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    size_bytes     BIGINT       NOT NULL DEFAULT 0,
    received_bytes BIGINT       NOT NULL DEFAULT 0,
    content_type   VARCHAR(128),
    status         VARCHAR(16)  NOT NULL DEFAULT 'UPLOADING' CHECK (status IN ('UPLOADING', 'COMPLETE')),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_share_file_name ON share_file (share_id, lower(name));

CREATE TABLE feedback_message (
    id         BIGSERIAL   PRIMARY KEY,
    body       TEXT        NOT NULL CHECK (length(body) BETWEEN 1 AND 8192),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at    TIMESTAMPTZ
);

CREATE INDEX idx_feedback_unread ON feedback_message (created_at) WHERE read_at IS NULL;
