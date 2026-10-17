ALTER TABLE crm_call_recording ADD COLUMN revision VARCHAR(36) NOT NULL DEFAULT '';
UPDATE crm_call_recording SET revision = UUID();
ALTER TABLE crm_call_recording ADD COLUMN analysis_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE crm_call_recording ADD COLUMN analysis_started_at DATETIME(6) NULL;
ALTER TABLE crm_call_recording ADD COLUMN transcript LONGTEXT NULL;
ALTER TABLE crm_call_recording ADD COLUMN summary LONGTEXT NULL;
ALTER TABLE crm_call_recording ADD COLUMN analysis_error VARCHAR(200) NULL;
