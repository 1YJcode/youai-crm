package com.youai.crm.communication;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "crm_call_record", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(columnNames = {"agent_user_id", "gateway_request_key"}),
        @jakarta.persistence.UniqueConstraint(columnNames = "gateway_call_id")})
public class CallRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_no", nullable = false, length = 32)
    private String customerNo;

    @Column(name = "customer_name", nullable = false, length = 64)
    private String customerName;

    @Column(nullable = false, length = 24)
    private String phone;

    @Column(nullable = false, length = 32)
    private String owner;

    @Column(nullable = false, length = 64)
    private String agent;

    @Column(nullable = false, length = 16)
    private String direction;

    @Column(name = "call_status", nullable = false, length = 24)
    private String status;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(length = 1000)
    private String note;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "recording_source", nullable = false, length = 16)
    private String recordingSource = "SYSTEM";

    @Column(name = "agent_user_id")
    private Long agentUserId;

    @Column(name = "uploaded_by_user_id")
    private Long uploadedByUserId;

    public String getRecordingSource() { return recordingSource; }
    public void setRecordingSource(String value) { recordingSource = value; }
    public Long getAgentUserId() { return agentUserId; }
    public void setAgentUserId(Long value) { agentUserId = value; }
    public Long getUploadedByUserId() { return uploadedByUserId; }
    public void setUploadedByUserId(Long value) { uploadedByUserId = value; }

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime value) { uploadedAt = value; }

    @Column(name = "gateway_state", length = 24, nullable = false)
    private String gatewayState = "LOCAL";

    @Column(name = "gateway_request_key", length = 64)
    private String gatewayRequestKey;

    @Column(name = "gateway_call_id", length = 128)
    private String gatewayCallId;

    @Column(name = "gateway_sequence", nullable = false)
    private long gatewaySequence = -1;

    @Column(name = "gateway_updated_at")
    private LocalDateTime gatewayUpdatedAt;

    @Column(name = "connected_at")
    private LocalDateTime connectedAt;

    @Column(name = "browser_offer", columnDefinition = "TEXT")
    private String browserOffer;

    @Column(name = "browser_answer", columnDefinition = "TEXT")
    private String browserAnswer;

    @Column(name = "hangup_requested", nullable = false)
    private boolean hangupRequested;

    @Column(name = "browser_last_seen_at")
    private LocalDateTime browserLastSeenAt;

    public LocalDateTime getBrowserLastSeenAt() { return browserLastSeenAt; }
    public void setBrowserLastSeenAt(LocalDateTime value) { browserLastSeenAt = value; }

    @PrePersist
    void onCreate() {
        if (startedAt == null) startedAt = LocalDateTime.now();
    }

    public String getGatewayState() { return gatewayState; }
    public void setGatewayState(String value) { gatewayState = value; }
    public String getGatewayRequestKey() { return gatewayRequestKey; }
    public void setGatewayRequestKey(String value) { gatewayRequestKey = value; }
    public String getGatewayCallId() { return gatewayCallId; }
    public void setGatewayCallId(String value) { gatewayCallId = value; }
    public long getGatewaySequence() { return gatewaySequence; }
    public void setGatewaySequence(long value) { gatewaySequence = value; }
    public LocalDateTime getGatewayUpdatedAt() { return gatewayUpdatedAt; }
    public void setGatewayUpdatedAt(LocalDateTime value) { gatewayUpdatedAt = value; }
    public LocalDateTime getConnectedAt() { return connectedAt; }
    public void setConnectedAt(LocalDateTime value) { connectedAt = value; }
    public String getBrowserOffer() { return browserOffer; }
    public void setBrowserOffer(String value) { browserOffer = value; }
    public String getBrowserAnswer() { return browserAnswer; }
    public void setBrowserAnswer(String value) { browserAnswer = value; }
    public boolean getHangupRequested() { return hangupRequested; }
    public void setHangupRequested(boolean value) { hangupRequested = value; }

    public Long getId() { return id; }
    public String getCustomerNo() { return customerNo; }
    public void setCustomerNo(String customerNo) { this.customerNo = customerNo; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getAgent() { return agent; }
    public void setAgent(String agent) { this.agent = agent; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
}
