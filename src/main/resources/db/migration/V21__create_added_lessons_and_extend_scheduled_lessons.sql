-- V21: Add schedule-scoped one-time lessons and link them to snapshot rows.

CREATE TABLE added_lessons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    dance_group_id BIGINT NULL,
    student_id BIGINT NULL,
    timeslot_id BIGINT NULL,
    room_id BIGINT NULL,
    duration_minutes INT NOT NULL DEFAULT 60,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_added_lessons_schedule FOREIGN KEY (schedule_id) REFERENCES schedules (id) ON DELETE CASCADE,
    CONSTRAINT fk_added_lessons_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE CASCADE,
    CONSTRAINT fk_added_lessons_group FOREIGN KEY (dance_group_id) REFERENCES dance_groups (id) ON DELETE SET NULL,
    CONSTRAINT fk_added_lessons_student FOREIGN KEY (student_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_added_lessons_timeslot FOREIGN KEY (timeslot_id) REFERENCES timeslots (id) ON DELETE SET NULL,
    CONSTRAINT fk_added_lessons_room FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE SET NULL
);

CREATE INDEX idx_added_lessons_schedule ON added_lessons (schedule_id);
CREATE INDEX idx_added_lessons_teacher ON added_lessons (teacher_id);
CREATE INDEX idx_added_lessons_student ON added_lessons (student_id);

ALTER TABLE scheduled_lessons
    MODIFY COLUMN lesson_id BIGINT NULL,
    ADD COLUMN added_lesson_id BIGINT NULL,
    ADD CONSTRAINT fk_scheduled_lessons_added_lesson
        FOREIGN KEY (added_lesson_id) REFERENCES added_lessons (id) ON DELETE CASCADE,
    ADD CONSTRAINT uk_scheduled_lessons_schedule_added_lesson UNIQUE (schedule_id, added_lesson_id);

CREATE INDEX idx_scheduled_lessons_added_lesson ON scheduled_lessons (added_lesson_id);

