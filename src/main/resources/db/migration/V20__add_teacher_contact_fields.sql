-- Add teacher contact fields for self-registration support
ALTER TABLE teachers ADD COLUMN phone VARCHAR(50) NULL;
ALTER TABLE teachers ADD COLUMN bio TEXT NULL;

