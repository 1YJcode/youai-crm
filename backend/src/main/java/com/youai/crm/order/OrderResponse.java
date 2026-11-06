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
        boolean canConfirmPerformance,
        Long ownerId,
        String storeName,
        String inviter,
        String statusLabel) {

    public static OrderResponse from(SalesOrder order, boolean canConfirmPerformance, String inviter) {
        return new OrderResponse(
                order.getOrderNo(), order.getCustomerName(), order.getProduct(), order.getAmount(),
                order.getPaidAmount(), order.getPaymentStatus(), order.getServiceStatus(),
                order.isPerformanceConfirmed(), order.getOwner(), order.getCreatedAt(), order.getCustomerNo(),
                order.getConfirmationSalesId(), order.getConfirmationSalesName(), order.getConfirmationAdminId(), order.getConfirmationAdminName(),
                order.getSalesConfirmedAt(), order.getAdminConfirmedAt(), canConfirmPerformance,
                order.getOwnerId(), order.getStoreName(), inviter, "已支付".equals(order.getPaymentStatus()) ? "支付成功" : order.getPaymentStatus());
    }
}
