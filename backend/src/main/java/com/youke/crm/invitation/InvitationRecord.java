package com.youke.crm.invitation;

import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "crm_invitation_record")
public class InvitationRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "invitation_no", nullable = false, unique = true) private String invitationNo;
    @Column(name = "customer_no", nullable = false) private String customerNo;
    @Column(nullable = false) private String inviter;
    @Column(nullable = false) private String department;
    @Column(name = "invitation_method", nullable = false) private String invitationMethod;
    @Column(name = "store_name", nullable = false) private String storeName;
    @Column(name = "scheduled_at", nullable = false) private LocalDateTime scheduledAt;
    @Column(name = "arrival_at") private LocalDateTime arrivalAt;
    @Column(name = "arrival_status", nullable = false) private String arrivalStatus;
    @Column(name = "marital_status", nullable = false) private String maritalStatus;
    @Column(name = "annual_income", nullable = false) private String annualIncome;
    @Column(nullable = false) private String source;
    private String referrer;
    @Column(name = "related_order_no") private String relatedOrderNo;
    private String remark;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    @PrePersist void create() { var now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void update() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getInvitationNo() { return invitationNo; } public void setInvitationNo(String v) { invitationNo = v; }
    public String getCustomerNo() { return customerNo; } public void setCustomerNo(String v) { customerNo = v; }
    public String getInviter() { return inviter; } public void setInviter(String v) { inviter = v; }
    public String getDepartment() { return department; } public void setDepartment(String v) { department = v; }
    public String getInvitationMethod() { return invitationMethod; } public void setInvitationMethod(String v) { invitationMethod = v; }
    public String getStoreName() { return storeName; } public void setStoreName(String v) { storeName = v; }
    public LocalDateTime getScheduledAt() { return scheduledAt; } public void setScheduledAt(LocalDateTime v) { scheduledAt = v; }
    public LocalDateTime getArrivalAt() { return arrivalAt; } public void setArrivalAt(LocalDateTime v) { arrivalAt = v; }
    public String getArrivalStatus() { return arrivalStatus; } public void setArrivalStatus(String v) { arrivalStatus = v; }
    public String getMaritalStatus() { return maritalStatus; } public void setMaritalStatus(String v) { maritalStatus = v; }
    public String getAnnualIncome() { return annualIncome; } public void setAnnualIncome(String v) { annualIncome = v; }
    public String getSource() { return source; } public void setSource(String v) { source = v; }
    public String getReferrer() { return referrer; } public void setReferrer(String v) { referrer = v; }
    public String getRelatedOrderNo() { return relatedOrderNo; } public void setRelatedOrderNo(String v) { relatedOrderNo = v; }
    public String getRemark() { return remark; } public void setRemark(String v) { remark = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
