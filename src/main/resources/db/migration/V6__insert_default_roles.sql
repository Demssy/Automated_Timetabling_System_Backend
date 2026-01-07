-- Insert default roles (STUDENT, TEACHER, ADMIN)
-- Using MERGE-like behavior for idempotency

-- Insert STUDENT role if not exists
INSERT INTO roles (name)
SELECT 'STUDENT' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'STUDENT');

-- Insert TEACHER role if not exists
INSERT INTO roles (name)
SELECT 'TEACHER' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'TEACHER');

-- Insert ADMIN role if not exists
INSERT INTO roles (name)
SELECT 'ADMIN' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ADMIN');

