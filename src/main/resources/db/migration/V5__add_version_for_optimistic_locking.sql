-- V5: Add version column for optimistic locking
-- This migration exists in database but was missing from source code
-- Adding it to maintain consistency with flyway_schema_history

-- Add version column to lessons table for optimistic locking
ALTER TABLE lessons
ADD COLUMN version INT DEFAULT 0 NOT NULL;

-- Add version column to dance_groups table
ALTER TABLE dance_groups
ADD COLUMN version INT DEFAULT 0 NOT NULL;

-- Add version column to timeslots table
ALTER TABLE timeslots
ADD COLUMN version INT DEFAULT 0 NOT NULL;

-- Add version column to resource_unavailability table
ALTER TABLE resource_unavailability
ADD COLUMN version INT DEFAULT 0 NOT NULL;

