package com.youai.crm.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "crm_sales_order")
public class SalesOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    private String orderNo;

    @Column(name = "customer_no", length = 32)
    private String customerNo;

    @Column(name = "customer_name", nullable = false, length = 64)
    private String customerName;

    @Column(nullable = false, length = 128)
    private String product;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "payment_status", nullable = false, length = 24)
    private String paymentStatus;

    @Column(name = "service_status", nullable = false, length = 24)
    private String serviceStatus;

    @Column(name = "performance_confirmed", nullable = false)
    private boolean performanceConfirmed;

    @Column(name = "confirmation_sales_id")
    private Long confirmationSalesId;
    @Column(name = "confirmation_sales_name")
    private String confirmationSalesName;
    @Column(name = "confirmation_admin_id")
    private Long confirmationAdminId;
    @Column(name = "confirmation_admin_name")
    private String confirmationAdminName;
    @Column(name = "sales_confirmed_at")
    private LocalDateTime salesConfirmedAt;
    @Column(name = "admin_confirmed_at")
    private LocalDateTime adminConfirmedAt;
    @Column(name = "legacy_performance_confirmed", nullable = false)
    private boolean legacyPerformanceConfirmed;

    @Column(nullable = false, length = 32)
    private String owner;

    @Column(name = "owner_id")
    private Long ownerId;
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { ownerId = value; }

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getConfirmationSalesId() { return confirmationSalesId; }
    public void setConfirmationSalesId(Long value) { confirmationSalesId = value; }
    public String getConfirmationSalesName() { return confirmationSalesName; }
    public void setConfirmationSalesName(String value) { confirmationSalesName = value; }
    public Long getConfirmationAdminId() { return confirmationAdminId; }
    public void setConfirmationAdminId(Long value) { confirmationAdminId = value; }
    public String getConfirmationAdminName() { return confirmationAdminName; }
    public void setConfirmationAdminName(String value) { confirmationAdminName = value; }
    public LocalDateTime getSalesConfirmedAt() { return salesConfirmedAt; }
    public void setSalesConfirmedAt(LocalDateTime value) { salesConfirmedAt = value; }
    public LocalDateTime getAdminConfirmedAt() { return adminConfirmedAt; }
    public void setAdminConfirmedAt(LocalDateTime value) { adminConfirmedAt = value; }
    public void resetConfirmations() {
        salesConfirmedAt = null;
        adminConfirmedAt = null;
        performanceConfirmed = false;
    }
    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getCustomerNo() { return customerNo; }
    public void setCustomerNo(String value) { customerNo = value; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getServiceStatus() { return serviceStatus; }
    public void setServiceStatus(String serviceStatus) { this.serviceStatus = serviceStatus; }
    public boolean isPerformanceConfirmed() { return performanceConfirmed; }
    public void setPerformanceConfirmed(boolean performanceConfirmed) { this.performanceConfirmed = performanceConfirmed; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
