ALTER TABLE crm_call_recording MODIFY COLUMN audio LONGBLOB NULL;
ALTER TABLE crm_call_recording ADD COLUMN storage_backend VARCHAR(16) NULL;
ALTER TABLE crm_call_recording ADD COLUMN storage_key VARCHAR(512) NULL;
ALTER TABLE crm_call_recording ADD COLUMN audio_size BIGINT NULL;
ALTER TABLE crm_call_recording ADD COLUMN audio_sha256 VARCHAR(64) NULL;

CREATE TABLE crm_recording_job (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 call_id BIGINT NOT NULL,
 revision VARCHAR(36) NOT NULL,
 provider VARCHAR(16) NOT NULL,
 actor VARCHAR(128) NOT NULL,
 actor_user_id BIGINT NOT NULL,
 state VARCHAR(16) NOT NULL,
 attempts INT NOT NULL,
 available_at DATETIME(6) NOT NULL,
 lease_until DATETIME(6) NULL,
 token VARCHAR(36) NULL,
 created_at DATETIME(6) NOT NULL,
 INDEX idx_recording_job_dispatch (state, available_at),
 INDEX idx_recording_job_call (call_id, provider, state)
);
ALTER TABLE crm_recording_transcription ADD COLUMN requested_by VARCHAR(128) NULL;
ALTER TABLE crm_recording_transcription ADD COLUMN active_job_id VARCHAR(36) NULL;
