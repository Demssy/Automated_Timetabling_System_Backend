-- Add performance indices for availability tables created in V11.
-- These are required for efficient findByUserId / deleteByUserId queries.

CREATE INDEX idx_resource_unavailability_user_id
    ON resource_unavailability(user_id);

CREATE INDEX idx_weekly_availability_user_id
    ON weekly_availability(user_id);

