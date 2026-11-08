package com.youai.crm.communication;

import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "crm_message", uniqueConstraints = @UniqueConstraint(columnNames = {"conversation_id", "request_key"}))
public class CrmMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(nullable = false, length = 64)
    private String sender;

    @Column(nullable = false, length = 16)
    private String direction;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "read_flag", nullable = false)
    private boolean read;

    @Column(name = "delivery_status", length = 16, nullable = false)
    private String deliveryStatus = "LOCAL";

    @Column(name = "recipient_phone", length = 32)
    private String recipientPhone;

    @Column(name = "request_key", length = 64)
    private String requestKey;

    @Column(name = "gateway_message_id", length = 128)
    private String gatewayMessageId;

    @Column(name = "delivery_error", length = 200)
    private String deliveryError;

    @Column(name = "delivery_updated_at")
    private LocalDateTime deliveryUpdatedAt;

    @PrePersist
    void onCreate() {
        if (sentAt == null) sentAt = LocalDateTime.now();
    }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String value) { deliveryStatus = value; }
    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String value) { recipientPhone = value; }
    public String getRequestKey() { return requestKey; }
    public void setRequestKey(String value) { requestKey = value; }
    public String getGatewayMessageId() { return gatewayMessageId; }
    public void setGatewayMessageId(String value) { gatewayMessageId = value; }
    public String getDeliveryError() { return deliveryError; }
    public void setDeliveryError(String value) { deliveryError = value; }
    public LocalDateTime getDeliveryUpdatedAt() { return deliveryUpdatedAt; }
    public void setDeliveryUpdatedAt(LocalDateTime value) { deliveryUpdatedAt = value; }

    public Long getId() { return id; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
