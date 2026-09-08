CREATE TABLE users (
 id BIGSERIAL PRIMARY KEY, name VARCHAR(150) NOT NULL, email VARCHAR(320) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL,
 role VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE, must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, last_login_at TIMESTAMPTZ
);
CREATE INDEX idx_users_email ON users(email);
CREATE TABLE project_settings (
 id BIGSERIAL PRIMARY KEY, project_name VARCHAR(255) NOT NULL, total_acres DECIMAL(12,4) NOT NULL,
 start_date DATE NOT NULL, deadline TIMESTAMPTZ NOT NULL, timezone VARCHAR(80) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL, version BIGINT
);
CREATE TABLE sales (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), acres DECIMAL(12,4) NOT NULL,
 sale_date DATE NOT NULL, buyer_name VARCHAR(255), plot_reference VARCHAR(255), notes TEXT,
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 created_by BIGINT REFERENCES users(id), updated_by BIGINT REFERENCES users(id)
);
CREATE INDEX idx_sales_user ON sales(user_id); CREATE INDEX idx_sales_date ON sales(sale_date);
CREATE TABLE refresh_tokens (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), token_hash VARCHAR(255) NOT NULL UNIQUE,
 expires_at TIMESTAMPTZ NOT NULL, created_at TIMESTAMPTZ NOT NULL, revoked_at TIMESTAMPTZ,
 replaced_by_token_id BIGINT REFERENCES refresh_tokens(id), ip_address VARCHAR(64), user_agent TEXT
);
CREATE INDEX idx_refresh_hash ON refresh_tokens(token_hash); CREATE INDEX idx_refresh_user ON refresh_tokens(user_id);
CREATE TABLE audit_logs (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT REFERENCES users(id), action VARCHAR(60) NOT NULL, entity_type VARCHAR(60) NOT NULL,
 entity_id VARCHAR(100), old_value TEXT, new_value TEXT, created_at TIMESTAMPTZ NOT NULL, ip_address VARCHAR(64)
);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
