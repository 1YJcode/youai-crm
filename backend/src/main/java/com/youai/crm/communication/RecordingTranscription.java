package com.youai.crm.communication;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="crm_recording_transcription")
public class RecordingTranscription {
 @Id String id;
 @Column(nullable=false) String revision;
 @Column(nullable=false) String status;
 LocalDateTime started;
 @Column(columnDefinition="LONGTEXT") String transcript;
 String error;
 @Column(length=128) String requestedBy;
 @Column(length=36) String activeJobId;
 protected RecordingTranscription() {}
 RecordingTranscription(String id, String revision) { this.id=id; this.revision=revision; this.status="PENDING"; }
}
