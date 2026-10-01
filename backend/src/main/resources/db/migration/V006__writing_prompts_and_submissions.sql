CREATE TABLE writing_prompts (
  id TEXT PRIMARY KEY,
  content JSONB NOT NULL,
  version INT NOT NULL DEFAULT 1,
  visibility TEXT NOT NULL DEFAULT 'private',
  published BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_writing_prompts_visibility CHECK (visibility IN ('public', 'private'))
);

CREATE INDEX idx_writing_prompts_published ON writing_prompts (published);

CREATE TABLE writing_submissions (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL,
  prompt_id TEXT NOT NULL,
  text TEXT NOT NULL,
  word_count INT NOT NULL,
  status TEXT NOT NULL DEFAULT 'pending',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT fk_writing_submissions_users FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
  CONSTRAINT fk_writing_submissions_writing_prompts
    FOREIGN KEY (prompt_id) REFERENCES writing_prompts (id) ON DELETE RESTRICT,
  CONSTRAINT ck_writing_submissions_word_count CHECK (word_count >= 0),
  CONSTRAINT ck_writing_submissions_status CHECK (status IN ('pending', 'grading', 'graded', 'failed'))
);

CREATE INDEX idx_writing_submissions_user_id_created_at ON writing_submissions (user_id, created_at DESC);
CREATE INDEX idx_writing_submissions_prompt_id ON writing_submissions (prompt_id);
