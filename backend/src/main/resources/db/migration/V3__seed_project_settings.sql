INSERT INTO project_settings (project_name, total_acres, start_date, deadline, timezone, created_at, updated_at, version)
SELECT '50 Acre Sales Challenge', 50.0000, CURRENT_DATE, TIMESTAMPTZ '2026-12-31 23:59:59+05:30', 'Asia/Kolkata', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM project_settings);
