CREATE TABLE crm_customer_transcript (
 customer_no VARCHAR(32) NOT NULL PRIMARY KEY,
 created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE crm_customer_transcript_segment (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 customer_no VARCHAR(32) NOT NULL,
 call_id BIGINT NOT NULL,
 recording_revision VARCHAR(36) NOT NULL,
 provider VARCHAR(16) NOT NULL,
 transcript LONGTEXT NOT NULL,
 customer_name VARCHAR(64) NOT NULL,
 sales_user_id BIGINT NULL,
 sales_name VARCHAR(64) NOT NULL,
 recording_source VARCHAR(16) NOT NULL,
 call_started_at DATETIME(6) NULL,
 recording_uploaded_at DATETIME(6) NULL,
 source_time DATETIME(6) NOT NULL,
 duration_seconds INT NULL,
 transcription_started_at DATETIME(6) NULL,
 transcribed_at DATETIME(6) NULL,
 added_at DATETIME(6) NOT NULL,
 source_article_id BIGINT NULL,
 UNIQUE KEY uk_customer_transcript_recording (call_id,recording_revision),
 INDEX idx_customer_transcript_time (customer_no,source_time,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE crm_knowledge_segment_source (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 article_id BIGINT NOT NULL,
 segment_id BIGINT NOT NULL,
 UNIQUE KEY uk_knowledge_segment_source (article_id,segment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
