CREATE TABLE crm_call_recording (
    call_id BIGINT NOT NULL PRIMARY KEY,
    media_type VARCHAR(64) NOT NULL,
    audio LONGBLOB NOT NULL,
    uploaded_at DATETIME NOT NULL,
    CONSTRAINT fk_recording_call FOREIGN KEY (call_id) REFERENCES crm_call_record (id) ON DELETE CASCADE
);
