CREATE TABLE crm_recording_transcription (
 id VARCHAR(255) NOT NULL PRIMARY KEY,
 revision VARCHAR(255) NOT NULL,
 status VARCHAR(255) NOT NULL,
 started DATETIME(6) NULL,
 transcript LONGTEXT NULL,
 error VARCHAR(255) NULL
);
