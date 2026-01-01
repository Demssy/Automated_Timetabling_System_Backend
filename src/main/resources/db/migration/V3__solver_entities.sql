-- Flyway migration: add Timefold Solver entities (timeslots, dance_groups, lessons, resource_unavailability)

CREATE TABLE timeslots (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  day_of_week VARCHAR(10) NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  UNIQUE KEY uk_timeslot (day_of_week, start_time, end_time)
);

CREATE TABLE dance_groups (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  dance_style_id BIGINT,
  dance_level VARCHAR(50),
  min_size INT,
  target_age_range VARCHAR(100),
  CONSTRAINT fk_dancegroup_style FOREIGN KEY (dance_style_id) REFERENCES dance_styles(id) ON DELETE SET NULL
);

CREATE TABLE lessons (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  teacher_id BIGINT NOT NULL,
  dance_group_id BIGINT NOT NULL,
  timeslot_id BIGINT,
  room_id BIGINT,
  duration_minutes INT NOT NULL DEFAULT 60,
  is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
  is_private BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_lesson_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
  CONSTRAINT fk_lesson_group FOREIGN KEY (dance_group_id) REFERENCES dance_groups(id) ON DELETE CASCADE,
  CONSTRAINT fk_lesson_timeslot FOREIGN KEY (timeslot_id) REFERENCES timeslots(id) ON DELETE SET NULL,
  CONSTRAINT fk_lesson_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL
);

CREATE TABLE resource_unavailability (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  teacher_id BIGINT NOT NULL,
  timeslot_id BIGINT NOT NULL,
  reason VARCHAR(500),
  CONSTRAINT fk_unavail_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
  CONSTRAINT fk_unavail_timeslot FOREIGN KEY (timeslot_id) REFERENCES timeslots(id) ON DELETE CASCADE
);

-- Indices for performance
CREATE INDEX idx_lessons_teacher ON lessons(teacher_id);
CREATE INDEX idx_lessons_timeslot ON lessons(timeslot_id);
CREATE INDEX idx_lessons_room ON lessons(room_id);
CREATE INDEX idx_unavail_teacher ON resource_unavailability(teacher_id);
CREATE INDEX idx_unavail_timeslot ON resource_unavailability(timeslot_id);

