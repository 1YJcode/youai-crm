package com.youai.crm.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        long totalCustomers,
        long newCustomers,
        long followUpsToday,
        long pendingTasks,
        long closedCustomers,
        BigDecimal pipelineAmount,
        BigDecimal salesAmount,
        BigDecimal paidAmount,
        long callDurationSeconds,
        long deepCalls,
        long arrivedCustomers,
        long pendingServiceCustomers,
        long activeServiceCustomers,
        long expiringServiceCustomers,
        BigDecimal refundAmount,
        List<VisitRank> visitRanking,
        List<SalesRank> salesRanking) {

    public record VisitRank(int rank, String department, String employee,
            long arrivedCustomers, long closedCustomers, BigDecimal conversionRate) {}

    public record SalesRank(int rank, String employee, String department,
            BigDecimal salesAmount, BigDecimal paidAmount, BigDecimal completionRate) {}
}
