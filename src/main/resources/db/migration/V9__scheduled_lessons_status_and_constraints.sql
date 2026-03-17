-- Flyway migration: support UNASSIGNED rows and enforce status values in scheduled_lessons

ALTER TABLE scheduled_lessons
  MODIFY COLUMN timeslot_id BIGINT NULL,
  MODIFY COLUMN room_id BIGINT NULL,
  MODIFY COLUMN status ENUM('ASSIGNED', 'UNASSIGNED') NOT NULL;

ALTER TABLE scheduled_lessons
  ADD CONSTRAINT uk_scheduled_lessons_schedule_lesson UNIQUE (schedule_id, lesson_id);

CREATE INDEX idx_scheduled_lessons_status ON scheduled_lessons(status);

