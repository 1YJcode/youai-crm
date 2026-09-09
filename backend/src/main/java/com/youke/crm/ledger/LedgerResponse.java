package com.youke.crm.ledger;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record LedgerResponse(String id,String storeName,String sourceMerchant,String flowNo,String orderNo,BigDecimal orderAmount,BigDecimal feeAmount,BigDecimal ledgerAmount,String beneficiary,String executionStatus,LocalDateTime allocatedAt,LocalDateTime paidOutAt,String transactionOrderNo) {
    static LedgerResponse from(LedgerAccount r){ return new LedgerResponse(r.getLedgerNo(),r.getStoreName(),r.getSourceMerchant(),r.getFlowNo(),r.getOrderNo(),r.getOrderAmount(),r.getFeeAmount(),r.getLedgerAmount(),r.getBeneficiary(),r.getExecutionStatus(),r.getAllocatedAt(),r.getPaidOutAt(),r.getTransactionOrderNo()); }
}
