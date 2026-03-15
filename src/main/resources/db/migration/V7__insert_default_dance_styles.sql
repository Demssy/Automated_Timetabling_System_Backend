-- Insert default dance styles for initial dictionary setup
-- Using MERGE-like behavior for idempotency

INSERT INTO dance_styles (name)
SELECT 'Salsa' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dance_styles WHERE name = 'Salsa');

INSERT INTO dance_styles (name)
SELECT 'Bachata' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dance_styles WHERE name = 'Bachata');

INSERT INTO dance_styles (name)
SELECT 'Kizomba' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dance_styles WHERE name = 'Kizomba');

INSERT INTO dance_styles (name)
SELECT 'Hip Hop' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dance_styles WHERE name = 'Hip Hop');

INSERT INTO dance_styles (name)
SELECT 'Contemporary' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dance_styles WHERE name = 'Contemporary');

INSERT INTO dance_styles (name)
SELECT 'Jazz Funk' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dance_styles WHERE name = 'Jazz Funk');

INSERT INTO dance_styles (name)
SELECT 'Ballroom' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dance_styles WHERE name = 'Ballroom');

INSERT INTO dance_styles (name)
SELECT 'Latin' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dance_styles WHERE name = 'Latin');

