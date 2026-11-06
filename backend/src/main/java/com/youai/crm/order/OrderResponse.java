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
        LocalDateTime createdAt,
        String customerId,
        Long confirmationSalesId,
        String confirmationSalesName,
        Long confirmationAdminId,
        String confirmationAdminName,
        LocalDateTime salesConfirmedAt,
        LocalDateTime adminConfirmedAt,
        boolean canConfirmPerformance) {

    public static OrderResponse from(SalesOrder order, boolean canConfirmPerformance) {
        return new OrderResponse(
                order.getOrderNo(), order.getCustomerName(), order.getProduct(), order.getAmount(),
                order.getPaidAmount(), order.getPaymentStatus(), order.getServiceStatus(),
                order.isPerformanceConfirmed(), order.getOwner(), order.getCreatedAt(), order.getCustomerNo(),
                order.getConfirmationSalesId(), order.getConfirmationSalesName(), order.getConfirmationAdminId(), order.getConfirmationAdminName(),
                order.getSalesConfirmedAt(), order.getAdminConfirmedAt(), canConfirmPerformance);
    }
}
