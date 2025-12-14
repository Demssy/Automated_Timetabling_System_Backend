-- Flyway migration: create roles and joined inheritance tables (users + admins/students/teachers)

CREATE TABLE roles (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE
);

-- Base users table (shared fields)
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(255),
  role_id BIGINT NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- Admin-specific table
CREATE TABLE admins (
  id BIGINT PRIMARY KEY,
  has_billing_access BOOLEAN,
  CONSTRAINT fk_admins_user FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

-- Student-specific table
CREATE TABLE students (
  id BIGINT PRIMARY KEY,
  birth_date DATE,
  dance_level VARCHAR(50),
  parent_contact VARCHAR(255),
  CONSTRAINT fk_students_user FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

-- Teacher-specific table
CREATE TABLE teachers (
  id BIGINT PRIMARY KEY,
  max_daily_hours INT,
  color_code VARCHAR(24),
  CONSTRAINT fk_teachers_user FOREIGN KEY (id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indices
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role_id ON users(role_id);
