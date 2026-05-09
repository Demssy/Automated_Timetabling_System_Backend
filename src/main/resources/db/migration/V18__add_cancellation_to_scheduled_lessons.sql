-- V18: Add cancellation support to scheduled_lessons.
-- A separate boolean flag is used instead of extending the solver-managed `status` enum,
-- because `status` represents the solver's assignment result and must not be overwritten
-- by manual user actions (the solver would reset it on the next solve run).
ALTER TABLE scheduled_lessons
    ADD COLUMN is_cancelled  BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN cancelled_by  BIGINT      NULL,
    ADD COLUMN cancelled_at  DATETIME(6) NULL,
    ADD COLUMN cancel_reason VARCHAR(255) NULL,
    ADD CONSTRAINT fk_scheduled_lessons_cancelled_by
        FOREIGN KEY (cancelled_by) REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX idx_scheduled_lessons_cancelled ON scheduled_lessons (is_cancelled);

