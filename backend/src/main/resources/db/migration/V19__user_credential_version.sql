ALTER TABLE crm_user
    ADD COLUMN credential_version BIGINT NOT NULL DEFAULT 0 AFTER enabled;
