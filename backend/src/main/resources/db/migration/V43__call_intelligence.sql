CREATE TABLE crm_call_intelligence (
 segment_id BIGINT NOT NULL PRIMARY KEY,
 customer_no VARCHAR(32) NOT NULL,
 state VARCHAR(24) NOT NULL,
 active_job_id VARCHAR(36) NULL,
 requested_by VARCHAR(128) NULL,
 requested_user_id BIGINT NULL,
 processing_consent BIT NOT NULL,
 preferred_template VARCHAR(64) NULL,
 model VARCHAR(128) NULL,
 prompt_version VARCHAR(64) NULL,
 analysis_json LONGTEXT NULL,
 error VARCHAR(200) NULL,
 requested_at DATETIME(6) NULL,
 completed_at DATETIME(6) NULL,
 confirmed_template VARCHAR(64) NULL,
 confirmed_content LONGTEXT NULL,
 confirmed_by BIGINT NULL,
 confirmed_by_name VARCHAR(128) NULL,
 confirmed_at DATETIME(6) NULL,
 follow_up_id BIGINT NULL,
 version BIGINT NOT NULL,
 INDEX idx_call_intelligence_customer (customer_no,segment_id),
 INDEX idx_call_intelligence_waiting (state,requested_at),
 INDEX idx_call_intelligence_job (active_job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE crm_recording_job ADD COLUMN analysis_consent BIT NOT NULL DEFAULT 0;
ALTER TABLE crm_recording_upload ADD COLUMN auto_process BIT NOT NULL DEFAULT 0;
ALTER TABLE crm_recording_upload ADD COLUMN processing_consent BIT NOT NULL DEFAULT 0;
