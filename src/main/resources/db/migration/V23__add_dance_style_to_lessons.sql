-- V23: Private lesson templates carry a dance style
ALTER TABLE lessons ADD COLUMN dance_style_id BIGINT NULL;
ALTER TABLE lessons ADD CONSTRAINT fk_lesson_dance_style FOREIGN KEY (dance_style_id) REFERENCES dance_styles(id) ON DELETE SET NULL;