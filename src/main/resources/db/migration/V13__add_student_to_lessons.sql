-- V13: Support Private Lessons — make dance_group_id nullable and add student_id FK
-- dance_group_id is now NULL for private lessons (student is assigned instead)
ALTER TABLE lessons
    MODIFY COLUMN dance_group_id BIGINT NULL;

-- Add student_id foreign key for private lessons (NULL for group lessons)
ALTER TABLE lessons
    ADD COLUMN student_id BIGINT NULL,
    ADD CONSTRAINT fk_lesson_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE SET NULL;

-- Index for performance on student-based lookups
CREATE INDEX idx_lessons_student ON lessons(student_id);

