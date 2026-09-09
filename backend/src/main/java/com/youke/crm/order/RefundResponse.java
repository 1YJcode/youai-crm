package com.youke.crm.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundResponse(
        String id,
        String orderId,
        String customer,
        BigDecimal amount,
        String reason,
        String applicant,
        String status,
        String reviewer,
        LocalDateTime createdAt,
        LocalDateTime reviewedAt) {

    public static RefundResponse from(OrderRefund refund) {
        return new RefundResponse(refund.getId(), refund.getOrderNo(), refund.getCustomerName(), refund.getAmount(),
                refund.getReason(), refund.getApplicant(), refund.getStatus(), refund.getReviewer(),
                refund.getCreatedAt(), refund.getReviewedAt());
    }
}
