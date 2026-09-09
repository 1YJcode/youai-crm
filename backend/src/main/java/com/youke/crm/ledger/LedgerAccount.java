package com.youke.crm.ledger;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;

@Entity
@Table(name = "crm_ledger_account")
public class LedgerAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="ledger_no", nullable=false, unique=true) private String ledgerNo;
    @Column(name="store_name", nullable=false) private String storeName;
    @Column(name="source_merchant", nullable=false) private String sourceMerchant;
    @Column(name="flow_no", nullable=false) private String flowNo;
    @Column(name="order_no", nullable=false, unique=true) private String orderNo;
    @Column(name="order_amount", nullable=false) private BigDecimal orderAmount;
    @Column(name="fee_amount", nullable=false) private BigDecimal feeAmount;
    @Column(name="ledger_amount", nullable=false) private BigDecimal ledgerAmount;
    @Column(nullable=false) private String beneficiary;
    @Column(name="execution_status", nullable=false) private String executionStatus;
    @Column(name="allocated_at") private LocalDateTime allocatedAt;
    @Column(name="paid_out_at") private LocalDateTime paidOutAt;
    @Column(name="transaction_order_no") private String transactionOrderNo;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at", nullable=false) private LocalDateTime updatedAt;
    @PrePersist void create(){ var now=LocalDateTime.now(); createdAt=now; updatedAt=now; }
    @PreUpdate void update(){ updatedAt=LocalDateTime.now(); }
    public Long getId(){return id;} public String getLedgerNo(){return ledgerNo;} public void setLedgerNo(String v){ledgerNo=v;}
    public String getStoreName(){return storeName;} public void setStoreName(String v){storeName=v;} public String getSourceMerchant(){return sourceMerchant;} public void setSourceMerchant(String v){sourceMerchant=v;}
    public String getFlowNo(){return flowNo;} public void setFlowNo(String v){flowNo=v;} public String getOrderNo(){return orderNo;} public void setOrderNo(String v){orderNo=v;}
    public BigDecimal getOrderAmount(){return orderAmount;} public void setOrderAmount(BigDecimal v){orderAmount=v;} public BigDecimal getFeeAmount(){return feeAmount;} public void setFeeAmount(BigDecimal v){feeAmount=v;}
    public BigDecimal getLedgerAmount(){return ledgerAmount;} public void setLedgerAmount(BigDecimal v){ledgerAmount=v;} public String getBeneficiary(){return beneficiary;} public void setBeneficiary(String v){beneficiary=v;}
    public String getExecutionStatus(){return executionStatus;} public void setExecutionStatus(String v){executionStatus=v;} public LocalDateTime getAllocatedAt(){return allocatedAt;} public void setAllocatedAt(LocalDateTime v){allocatedAt=v;}
    public LocalDateTime getPaidOutAt(){return paidOutAt;} public void setPaidOutAt(LocalDateTime v){paidOutAt=v;} public String getTransactionOrderNo(){return transactionOrderNo;} public void setTransactionOrderNo(String v){transactionOrderNo=v;}
    public LocalDateTime getCreatedAt(){return createdAt;}
}
