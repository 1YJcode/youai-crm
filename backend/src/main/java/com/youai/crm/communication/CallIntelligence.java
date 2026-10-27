package com.youai.crm.communication;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="crm_call_intelligence")
public class CallIntelligence {
    @Id Long segmentId;
    @Column(nullable=false,length=32) String customerNo;
    @Column(nullable=false,length=24) String state;
    @Column(length=36) String activeJobId;
    @Column(length=128) String requestedBy;
    Long requestedUserId;
    boolean processingConsent;
    @Column(length=64) String preferredTemplate;
    @Column(length=128) String model;
    @Column(length=64) String promptVersion;
    @Column(columnDefinition="LONGTEXT") String analysisJson;
    @Column(length=200) String error;
    LocalDateTime requestedAt;
    LocalDateTime completedAt;
    @Column(length=64) String confirmedTemplate;
    @Column(columnDefinition="LONGTEXT") String confirmedContent;
    Long confirmedBy;
    @Column(length=128) String confirmedByName;
    LocalDateTime confirmedAt;
    Long followUpId;
    @Version Long version;
    protected CallIntelligence(){}
}
