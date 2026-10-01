CREATE TABLE reading_tests (
  id TEXT PRIMARY KEY,
  module TEXT NOT NULL,
  title TEXT NOT NULL,
  time_limit_minutes INT NOT NULL,
  content JSONB NOT NULL,
  version INT NOT NULL DEFAULT 1,
  published BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reading_tests_module_published ON reading_tests (module, published);

CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  firebase_uid TEXT NOT NULL,
  email TEXT,
  display_name TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_users_firebase_uid UNIQUE (firebase_uid)
);

CREATE TABLE reading_attempts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL,
  test_id TEXT NOT NULL,
  correct_count INT NOT NULL,
  total_questions INT NOT NULL,
  band NUMERIC(2, 1) NOT NULL,
  answers JSONB NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_reading_attempts_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_reading_attempts_reading_tests FOREIGN KEY (test_id) REFERENCES reading_tests (id) ON DELETE RESTRICT,
  CONSTRAINT ck_reading_attempts_counts CHECK (correct_count BETWEEN 0 AND total_questions),
  CONSTRAINT ck_reading_attempts_band CHECK (band BETWEEN 0 AND 9)
);

CREATE INDEX idx_reading_attempts_user_id_created_at ON reading_attempts (user_id, created_at DESC);
CREATE INDEX idx_reading_attempts_test_id ON reading_attempts (test_id);
