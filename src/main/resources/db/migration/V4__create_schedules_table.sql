-- Migration: V4__create_schedules_table.sql
-- Description: Creates schedules table for tracking schedule versions and history

CREATE TABLE schedules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT 'Human-readable schedule name (e.g., "Fall 2025", "Draft 1")',
    valid_from DATE NOT NULL COMMENT 'First date this schedule is valid/active',
    valid_to DATE NOT NULL COMMENT 'Last date this schedule is valid/active',
    created_at DATETIME NOT NULL COMMENT 'Timestamp when schedule was created',
    status ENUM('DRAFT', 'PUBLISHED', 'ARCHIVED') NOT NULL COMMENT 'Lifecycle status of the schedule',
    solver_score VARCHAR(50) COMMENT 'Timefold Solver score (e.g., "0hard/-200soft")',
    description VARCHAR(500) COMMENT 'Optional notes or description',

    CONSTRAINT chk_valid_dates CHECK (valid_to >= valid_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for querying by status (find active/published schedules)
CREATE INDEX idx_schedules_status ON schedules(status);

-- Index for querying by date range (find schedules valid on a specific date)
CREATE INDEX idx_schedules_dates ON schedules(valid_from, valid_to);

-- Index for ordering by creation date
CREATE INDEX idx_schedules_created_at ON schedules(created_at DESC);

-- Comment on table
ALTER TABLE schedules COMMENT 'Stores metadata for schedule versions, allowing version control and history tracking';

