package com.youai.crm.communication;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name="crm_recording_job")
public class RecordingJob {
    @Id @Column(length=36) String id;
    @Column(nullable=false) Long callId;
    @Column(nullable=false,length=36) String revision;
    @Column(nullable=false,length=16) String provider;
    @Column(nullable=false,length=128) String actor;
    @Column(nullable=false) Long actorUserId;
    @Column(nullable=false,length=16) String state;
    @Column(nullable=false) int attempts;
    boolean analysisConsent;
    @Column(nullable=false) LocalDateTime availableAt;
    LocalDateTime leaseUntil;
    @Column(length=36) String token;
    @Column(nullable=false) LocalDateTime createdAt;
    protected RecordingJob() {}
    RecordingJob(Long callId,String revision,String provider,String actor,Long actorUserId) {
        id=java.util.UUID.randomUUID().toString(); this.callId=callId; this.revision=revision;
        this.provider=provider; this.actor=actor; state="QUEUED";
        this.actorUserId=actorUserId;
        availableAt=LocalDateTime.now(); createdAt=availableAt;
    }
}
