ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('ADMIN','USER'));
ALTER TABLE sales ADD CONSTRAINT chk_sales_acres_positive CHECK (acres > 0);
ALTER TABLE project_settings ADD CONSTRAINT chk_project_total_positive CHECK (total_acres > 0);
