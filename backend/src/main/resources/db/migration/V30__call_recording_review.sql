ALTER TABLE crm_call_recording ADD COLUMN reviewed_summary LONGTEXT NULL;
ALTER TABLE crm_call_recording ADD COLUMN reviewed_by VARCHAR(128) NULL;
ALTER TABLE crm_call_recording ADD COLUMN reviewed_at DATETIME(6) NULL;
ALTER TABLE crm_call_recording ADD COLUMN review_version BIGINT NOT NULL DEFAULT 0;
