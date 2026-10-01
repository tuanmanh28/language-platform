CREATE TABLE listening_tests (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  content JSONB NOT NULL,
  version INT NOT NULL DEFAULT 1,
  published BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_listening_tests_published ON listening_tests (published);
