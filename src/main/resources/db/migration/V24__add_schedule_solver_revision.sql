ALTER TABLE schedules
    ADD COLUMN solver_revision BIGINT NOT NULL DEFAULT 0;
