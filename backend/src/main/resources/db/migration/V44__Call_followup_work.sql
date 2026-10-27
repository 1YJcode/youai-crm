CREATE TABLE crm_call_followup_work (
 call_id BIGINT NOT NULL PRIMARY KEY,
 customer_no VARCHAR(32) NOT NULL,
 started_at DATETIME(6) NOT NULL,
 remind_at DATETIME(6) NOT NULL,
 escalate_at DATETIME(6) NOT NULL,
 follow_up_id BIGINT NULL,
 completed_at DATETIME(6) NULL,
 source_segment_id BIGINT NULL,
 analysis_version BIGINT NULL,
 template VARCHAR(64) NULL,
 confirmed_content TEXT NULL,
 confirmed_by BIGINT NULL,
 INDEX idx_call_work_pending (follow_up_id,remind_at),
 INDEX idx_call_work_customer (customer_no,started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE crm_system_notification ADD COLUMN call_id BIGINT NULL;
ALTER TABLE crm_system_notification ADD COLUMN notification_kind VARCHAR(32) NULL;
