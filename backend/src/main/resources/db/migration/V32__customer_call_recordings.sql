ALTER TABLE crm_call_record ADD COLUMN recording_source VARCHAR(16) NOT NULL DEFAULT 'SYSTEM';
ALTER TABLE crm_call_record ADD COLUMN agent_user_id BIGINT NULL;
ALTER TABLE crm_call_record ADD COLUMN uploaded_by_user_id BIGINT NULL;
UPDATE crm_call_record SET recording_source = 'UPLOAD' WHERE id IN (SELECT call_id FROM crm_call_recording);
CREATE INDEX idx_call_customer_started ON crm_call_record(customer_no, started_at);
