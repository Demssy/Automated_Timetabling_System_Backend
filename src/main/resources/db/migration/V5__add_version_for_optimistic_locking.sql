-- Add version column for Optimistic Locking to all mutable entities
-- This migration adds the @Version field required by JPA for concurrency control

-- Teachers table
ALTER TABLE teachers ADD COLUMN version BIGINT DEFAULT 0;

-- Students table
ALTER TABLE students ADD COLUMN version BIGINT DEFAULT 0;

-- Dance groups table
ALTER TABLE dance_groups ADD COLUMN version BIGINT DEFAULT 0;

-- Lessons table
ALTER TABLE lessons ADD COLUMN version BIGINT DEFAULT 0;

-- Rooms table
ALTER TABLE rooms ADD COLUMN version BIGINT DEFAULT 0;

-- Dance styles table
ALTER TABLE dance_styles ADD COLUMN version BIGINT DEFAULT 0;

-- Schedules table
ALTER TABLE schedules ADD COLUMN version BIGINT DEFAULT 0;

