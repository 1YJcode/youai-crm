package com.youai.crm.communication;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="crm_customer_transcript_segment",uniqueConstraints=@UniqueConstraint(columnNames={"call_id","recording_revision"}))
public class CustomerTranscriptSegment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false,length=32) String customerNo;
    @Column(nullable=false) Long callId;
    @Column(nullable=false,length=36) String recordingRevision;
    @Column(nullable=false,length=16) String provider;
    @Column(nullable=false,columnDefinition="LONGTEXT") String transcript;
    @Column(nullable=false,length=64) String customerName;
    Long salesUserId;
    @Column(nullable=false,length=64) String salesName;
    @Column(nullable=false,length=16) String recordingSource;
    LocalDateTime callStartedAt;
    LocalDateTime recordingUploadedAt;
    @Column(nullable=false) LocalDateTime sourceTime;
    Integer durationSeconds;
    LocalDateTime transcriptionStartedAt;
    LocalDateTime transcribedAt;
    @Column(nullable=false) LocalDateTime addedAt;
    Long sourceArticleId;
    protected CustomerTranscriptSegment() {}
}
