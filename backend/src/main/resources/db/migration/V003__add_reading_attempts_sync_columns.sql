-- No endpoint has written reading_attempts before this migration, so the table is empty everywhere.
ALTER TABLE reading_attempts ADD COLUMN client_id TEXT NOT NULL;
ALTER TABLE reading_attempts ADD COLUMN synced_at TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE reading_attempts ADD CONSTRAINT uq_reading_attempts_user_id_client_id UNIQUE (user_id, client_id);

CREATE INDEX idx_reading_attempts_user_id_synced_at ON reading_attempts (user_id, synced_at DESC);
