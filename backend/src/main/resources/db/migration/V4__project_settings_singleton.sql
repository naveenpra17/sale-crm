-- Enforce singleton project settings with stable ID = 1
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM project_settings WHERE id = 1) THEN
    IF EXISTS (SELECT 1 FROM project_settings LIMIT 1) THEN
      UPDATE project_settings SET id = 1 WHERE id = (SELECT MIN(id) FROM project_settings);
    END IF;
  END IF;
END $$;

ALTER TABLE project_settings DROP CONSTRAINT IF EXISTS project_settings_pkey;
ALTER TABLE project_settings ADD CONSTRAINT project_settings_pkey PRIMARY KEY (id);
ALTER TABLE project_settings DROP CONSTRAINT IF EXISTS chk_project_singleton;
ALTER TABLE project_settings ADD CONSTRAINT chk_project_singleton CHECK (id = 1);
