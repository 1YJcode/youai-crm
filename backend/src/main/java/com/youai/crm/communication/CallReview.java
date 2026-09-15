package com.youai.crm.communication;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "crm_call_review")
public class CallReview {

    @Id
    @Column(name = "call_id")
    private Long callId;

    @Column(name = "reviewer_username", nullable = false, length = 64)
    private String reviewerUsername;

    @Column(nullable = false)
    private boolean reviewed;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getCallId() { return callId; }
    public void setCallId(Long callId) { this.callId = callId; }
    public String getReviewerUsername() { return reviewerUsername; }
    public void setReviewerUsername(String reviewerUsername) { this.reviewerUsername = reviewerUsername; }
    public boolean isReviewed() { return reviewed; }
    public void setReviewed(boolean reviewed) { this.reviewed = reviewed; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
