-- V15: Add student_id to scheduled_lessons to persist solver-assigned student for private lessons.
-- Null for group lessons and unmatched private templates.
ALTER TABLE scheduled_lessons
    ADD COLUMN student_id BIGINT NULL,
    ADD CONSTRAINT fk_scheduled_lessons_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_scheduled_lessons_student ON scheduled_lessons(student_id);

