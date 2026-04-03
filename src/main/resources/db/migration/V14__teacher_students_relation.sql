-- V14: Create join table for teacher-student private lesson preferences.
-- A student can "choose" multiple teachers; a teacher can have a pool of interested students.
-- The composite PK (teacher_id, student_id) already guarantees uniqueness of the pair.

CREATE TABLE teacher_students (
    teacher_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    PRIMARY KEY (teacher_id, student_id),
    CONSTRAINT fk_ts_teacher FOREIGN KEY (teacher_id) REFERENCES teachers (id) ON DELETE CASCADE,
    CONSTRAINT fk_ts_student FOREIGN KEY (student_id) REFERENCES students (id) ON DELETE CASCADE
);

CREATE INDEX idx_ts_teacher_id ON teacher_students (teacher_id);
CREATE INDEX idx_ts_student_id ON teacher_students (student_id);

