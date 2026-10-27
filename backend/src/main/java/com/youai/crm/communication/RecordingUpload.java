package com.youai.crm.communication;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="crm_recording_upload")
public class RecordingUpload {
    @Id @Column(length=36) String id;
    @Column(nullable=false,length=128) String actor;
    @Column(nullable=false) Long actorUserId;
    @Column(nullable=false,length=32) String customerId;
    Long callId;
    @Column(nullable=false) long size;
    @Column(nullable=false,length=1000) String note;
    @Column(nullable=false) LocalDateTime expiresAt;
    @Column(nullable=false) boolean completed;
    boolean autoProcess;
    boolean processingConsent;
    protected RecordingUpload() {}
}
