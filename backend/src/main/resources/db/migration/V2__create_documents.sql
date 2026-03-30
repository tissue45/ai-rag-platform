CREATE TABLE IF NOT EXISTS documents (
    id               BIGSERIAL PRIMARY KEY,
    owner_user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title            VARCHAR(255) NOT NULL,
    source_type      VARCHAR(32)  NOT NULL,
    content          TEXT         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_documents_owner_user_id ON documents(owner_user_id);

