-- Idempotent seed for category icon paths used by the homepage card renderer.
-- Paths are web-relative to the app root (e.g., /hercey/assets/images/category/tea.png).

UPDATE category SET icon = 'assets/images/category/tea.png' WHERE LOWER(name) = 'tea';
UPDATE category SET icon = 'assets/images/category/herbal.png' WHERE LOWER(name) = 'herbal';
UPDATE category SET icon = 'assets/images/category/drugs.png' WHERE LOWER(name) = 'drugs';
UPDATE category SET icon = 'assets/images/category/perfume.png' WHERE LOWER(name) = 'perfume';
UPDATE category SET icon = 'assets/images/category/remedy.png' WHERE LOWER(name) = 'remedy';
UPDATE category SET icon = 'assets/images/category/cosmetics.png' WHERE LOWER(name) = 'beauty';

-- Optional fallback for any unmapped category rows.
UPDATE category
SET icon = 'assets/images/category/apple.png'
WHERE icon IS NULL OR TRIM(icon) = '';

