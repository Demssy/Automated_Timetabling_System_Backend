-- Flyway migration: add dictionaries (dance_styles, rooms, teacher_dance_style)

CREATE TABLE dance_styles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE rooms (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  capacity INT NOT NULL,
  allows_parallel_private BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE teacher_dance_style (
  dance_style_id BIGINT NOT NULL,
  teacher_id BIGINT NOT NULL,
  PRIMARY KEY (dance_style_id, teacher_id),
  CONSTRAINT fk_tds_dancestyle FOREIGN KEY (dance_style_id) REFERENCES dance_styles(id) ON DELETE CASCADE,
  CONSTRAINT fk_tds_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
);

