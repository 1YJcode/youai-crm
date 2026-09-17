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
@Table(name = "crm_system_notification")
public class SystemNotification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 64) private String username;
    @Column(name = "notification_key", nullable = false, unique = true, length = 160) private String notificationKey;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 2000) private String content;
    @Column(name = "customer_no", length = 32) private String customerNo;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @PrePersist void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String value) { username = value; }
    public String getNotificationKey() { return notificationKey; }
    public void setNotificationKey(String value) { notificationKey = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getContent() { return content; }
    public void setContent(String value) { content = value; }
    public String getCustomerNo() { return customerNo; }
    public void setCustomerNo(String value) { customerNo = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
