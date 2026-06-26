-- Add desired_lessons_per_week preference field to students and teachers
ALTER TABLE students ADD COLUMN desired_lessons_per_week INT NULL;
ALTER TABLE teachers ADD COLUMN desired_lessons_per_week INT NULL;
