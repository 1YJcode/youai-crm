package com.youai.crm.communication;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Immutable completed revision; retries update only an unfinished revision. */
@Entity @Table(name="crm_customer_review",uniqueConstraints=@UniqueConstraint(columnNames={"customer_no","fingerprint"}))
public class CustomerReview {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(nullable=false,length=32) String customerNo;
    @Column(nullable=false,length=64) String fingerprint;
    @Column(nullable=false,length=24) String state;
    @Column(nullable=false,length=32) String customerStage;
    @Column(nullable=false,columnDefinition="LONGTEXT") String sourcesJson;
    @Column(columnDefinition="LONGTEXT") String resultJson;
    @Column(length=200) String error;
    @Column(length=36) String token;
    @Column(nullable=false) int attempts;
    @Column(nullable=false) LocalDateTime createdAt;
    LocalDateTime completedAt;
    LocalDateTime leaseUntil;
    Long publishedArticleId;
    @Version Long version;
}
