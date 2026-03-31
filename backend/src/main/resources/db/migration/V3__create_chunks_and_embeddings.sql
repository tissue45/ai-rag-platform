CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE documents
    ADD COLUMN IF NOT EXISTS ingest_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS ingest_error TEXT,
    ADD COLUMN IF NOT EXISTS ingested_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS document_chunks (
    id         BIGSERIAL PRIMARY KEY,
    document_id BIGINT      NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER     NOT NULL,
    content    TEXT         NOT NULL,
    char_count INTEGER      NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_document_chunks_document_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id ON document_chunks(document_id);

CREATE TABLE IF NOT EXISTS chunk_embeddings (
    chunk_id   BIGINT       PRIMARY KEY REFERENCES document_chunks(id) ON DELETE CASCADE,
    embedding  VECTOR(1536) NOT NULL,
    model      VARCHAR(64)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

