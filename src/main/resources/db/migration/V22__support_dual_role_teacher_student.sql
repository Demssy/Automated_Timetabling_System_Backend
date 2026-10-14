-- V22: Dual-role support (a teacher may also be a student within the same account).
-- teachers.id and students.id are both shared primary keys referencing users.id,
-- so one user row can already own both profile rows. This migration adds multi-role
-- assignment and role-scoped availability.

-- 1. Multiple roles per user. users.role_id stays as the "primary" role.
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE INDEX idx_user_roles_role_id ON user_roles(role_id);

INSERT INTO user_roles (user_id, role_id)
SELECT id, role_id FROM users;

-- Users that already own a student profile also get the STUDENT role.
INSERT INTO user_roles (user_id, role_id)
SELECT s.id, r.id
FROM students s
JOIN roles r ON r.name = 'STUDENT'
WHERE NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = s.id AND ur.role_id = r.id);

-- 2. Role-scoped availability.
ALTER TABLE weekly_availability
    ADD COLUMN availability_role VARCHAR(20) NOT NULL DEFAULT 'TEACHER';

ALTER TABLE resource_unavailability
    ADD COLUMN availability_role VARCHAR(20) NOT NULL DEFAULT 'TEACHER';

-- Existing rows of pure students belong to the STUDENT role.
UPDATE weekly_availability
SET availability_role = 'STUDENT'
WHERE user_id NOT IN (SELECT id FROM teachers);

UPDATE resource_unavailability
SET availability_role = 'STUDENT'
WHERE user_id NOT IN (SELECT id FROM teachers);

CREATE INDEX idx_wa_user_role_day
    ON weekly_availability(user_id, availability_role, day_of_week);

CREATE INDEX idx_ru_user_role_date
    ON resource_unavailability(user_id, availability_role, unavailability_date);

-- 3. Student-side dance style preferences (used for dual-role registration).
CREATE TABLE student_dance_style (
    student_id     BIGINT NOT NULL,
    dance_style_id BIGINT NOT NULL,
    PRIMARY KEY (student_id, dance_style_id),
    CONSTRAINT fk_sds_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_sds_style   FOREIGN KEY (dance_style_id) REFERENCES dance_styles(id) ON DELETE CASCADE
);