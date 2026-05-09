-- V16: Add score_explanation column to store per-constraint breakdown as JSON
ALTER TABLE schedules
    ADD COLUMN score_explanation TEXT NULL;

