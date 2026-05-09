-- V17: Add many-to-many relationship between students and dance groups (enrollment).
-- A student can be enrolled in multiple groups; a group can have multiple students.
CREATE TABLE student_groups (
    student_id BIGINT NOT NULL,
    group_id   BIGINT NOT NULL,
    PRIMARY KEY (student_id, group_id),
    CONSTRAINT fk_sg_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sg_group   FOREIGN KEY (group_id)   REFERENCES dance_groups(id) ON DELETE CASCADE
);

CREATE INDEX idx_sg_student ON student_groups(student_id);
CREATE INDEX idx_sg_group   ON student_groups(group_id);

