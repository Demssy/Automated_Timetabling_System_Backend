-- Flyway migration: create table for lessons scheduled into concrete schedule versions

CREATE TABLE scheduled_lessons (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  lesson_id BIGINT NOT NULL,
  schedule_id BIGINT NOT NULL,
  timeslot_id BIGINT NOT NULL,
  status VARCHAR(50) NOT NULL,
  room_id BIGINT NOT NULL,
  CONSTRAINT fk_scheduled_lessons_lesson FOREIGN KEY (lesson_id) REFERENCES lessons(id) ON DELETE CASCADE,
  CONSTRAINT fk_scheduled_lessons_schedule FOREIGN KEY (schedule_id) REFERENCES schedules(id) ON DELETE CASCADE,
  CONSTRAINT fk_scheduled_lessons_timeslot FOREIGN KEY (timeslot_id) REFERENCES timeslots(id),
  CONSTRAINT fk_scheduled_lessons_room FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE INDEX idx_scheduled_lessons_schedule ON scheduled_lessons(schedule_id);
CREATE INDEX idx_scheduled_lessons_lesson ON scheduled_lessons(lesson_id);
CREATE INDEX idx_scheduled_lessons_timeslot ON scheduled_lessons(timeslot_id);
CREATE INDEX idx_scheduled_lessons_room ON scheduled_lessons(room_id);

