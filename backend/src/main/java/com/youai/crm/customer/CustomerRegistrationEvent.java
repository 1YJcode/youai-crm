package com.youai.crm.customer;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "crm_customer_registration_event")
public class CustomerRegistrationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "registration_number", nullable = false)
    private Integer registrationNumber;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(nullable = false, length = 64)
    private String operator;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long value) { customerId = value; }
    public Integer getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(Integer value) { registrationNumber = value; }
    public String getSource() { return source; }
    public void setSource(String value) { source = value; }
    public String getOperator() { return operator; }
    public void setOperator(String value) { operator = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
