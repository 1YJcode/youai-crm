package com.youai.crm.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
        String id,
        String customer,
        String product,
        BigDecimal amount,
        BigDecimal paid,
        String status,
        String service,
        boolean performanceConfirmed,
        String owner,
        LocalDateTime createdAt) {

    public static OrderResponse from(SalesOrder order) {
        return new OrderResponse(
                order.getOrderNo(), order.getCustomerName(), order.getProduct(), order.getAmount(),
                order.getPaidAmount(), order.getPaymentStatus(), order.getServiceStatus(),
                order.isPerformanceConfirmed(), order.getOwner(), order.getCreatedAt());
    }
}
