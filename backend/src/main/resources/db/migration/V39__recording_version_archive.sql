CREATE TABLE crm_recording_archive (
 revision VARCHAR(36) NOT NULL PRIMARY KEY,
 call_id BIGINT NOT NULL,
 media_type VARCHAR(64) NOT NULL,
 storage_backend VARCHAR(16) NOT NULL,
 storage_key VARCHAR(512) NOT NULL,
 audio_size BIGINT NOT NULL,
 audio_sha256 VARCHAR(64) NOT NULL,
 uploaded_at DATETIME(6) NOT NULL,
 archived_at DATETIME(6) NOT NULL,
 snapshot LONGTEXT NOT NULL,
 INDEX idx_recording_archive_call (call_id, archived_at)
);
