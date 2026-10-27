CREATE TABLE crm_recording_upload (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 actor VARCHAR(128) NOT NULL,
 actor_user_id BIGINT NOT NULL,
 customer_id VARCHAR(32) NOT NULL,
 call_id BIGINT NULL,
 size BIGINT NOT NULL,
 note VARCHAR(1000) NOT NULL,
 expires_at DATETIME(6) NOT NULL,
 completed BIT NOT NULL,
 INDEX idx_recording_upload_expiry (expires_at)
);
