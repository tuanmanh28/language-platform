-- Existing rows are the public samples; rows inserted later without a visibility stay private.
ALTER TABLE reading_tests ADD COLUMN visibility TEXT NOT NULL DEFAULT 'public';
ALTER TABLE reading_tests ALTER COLUMN visibility SET DEFAULT 'private';
ALTER TABLE reading_tests ADD CONSTRAINT ck_reading_tests_visibility CHECK (visibility IN ('public', 'private'));

ALTER TABLE listening_tests ADD COLUMN visibility TEXT NOT NULL DEFAULT 'public';
ALTER TABLE listening_tests ALTER COLUMN visibility SET DEFAULT 'private';
ALTER TABLE listening_tests ADD CONSTRAINT ck_listening_tests_visibility CHECK (visibility IN ('public', 'private'));
