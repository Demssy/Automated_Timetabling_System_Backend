ALTER TABLE lessons
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_lessons_is_active ON lessons(is_active);

