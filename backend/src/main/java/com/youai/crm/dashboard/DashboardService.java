package com.youai.crm.dashboard;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.communication.CallRecordRepository;
import com.youai.crm.customer.CustomerRepository;
import com.youai.crm.invitation.InvitationRecordRepository;
import com.youai.crm.order.OrderRefundRepository;
import com.youai.crm.order.SalesOrderRepository;
import com.youai.crm.task.FollowUpTaskRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private static final String CANCELLED = "已取消";
    private static final String APPROVED = "已通过";
    private static final String CLOSED_STAGE = "已成交";
    private static final String DEFAULT_DEPARTMENT = "销售部";
    private static final int DEEP_CALL_SECONDS = 300;

    private final CustomerRepository customerRepository;
    private final FollowUpTaskRepository taskRepository;
    private final SalesOrderRepository orderRepository;
    private final CallRecordRepository callRepository;
    private final InvitationRecordRepository invitationRepository;
    private final OrderRefundRepository refundRepository;
    private final AccessPolicy accessPolicy;

    public DashboardService(CustomerRepository customerRepository, FollowUpTaskRepository taskRepository,
            SalesOrderRepository orderRepository, CallRecordRepository callRepository,
            InvitationRecordRepository invitationRepository, OrderRefundRepository refundRepository,
            AccessPolicy accessPolicy) {
        this.customerRepository = customerRepository;
        this.taskRepository = taskRepository;
        this.orderRepository = orderRepository;
        this.callRepository = callRepository;
        this.invitationRepository = invitationRepository;
        this.refundRepository = refundRepository;
        this.accessPolicy = accessPolicy;
    }

    public DashboardResponse summary(LocalDate startDate, LocalDate endDate, String store,
            Authentication authentication) {
        String owner = accessPolicy.scopedOwner(authentication);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        String normalizedStore = store == null || store.isBlank() ? null : store;

        long totalCustomers = customerRepository.countForDashboard(owner);
        long newCustomers = customerRepository.countNewForDashboard(owner, start, end);
        long closedCustomers = customerRepository.countByStageForDashboard(owner, CLOSED_STAGE);
        BigDecimal pipeline = amount(customerRepository.sumPipelineForDashboard(owner));

        long followUpsToday = taskRepository.countDueCustomersForDashboard(owner, start, end);
        long pendingTasks = taskRepository.countPendingForDashboard(owner);

        BigDecimal salesAmount = amount(orderRepository.sumActiveAmountForDashboard(owner, start, end, CANCELLED));
        BigDecimal paidAmount = amount(orderRepository.sumPaidForDashboard(owner, start, end));
        long pendingService = orderRepository.countServiceStatusForDashboard(owner, start, end, "未开始");
        long activeService = orderRepository.countServiceStatusesForDashboard(owner, start, end, List.of("实施中", "已开通"));
        long expiringService = orderRepository.countServiceStatusForDashboard(owner, start, end, "待开通");

        long arrivedCustomers = invitationRepository.countArrivedForDashboard(owner, normalizedStore, start, end);
        long callDuration = callRepository.sumDurationForDashboard(owner, start, end);
        long deepCalls = callRepository.countDeepForDashboard(owner, start, end, DEEP_CALL_SECONDS);
        BigDecimal refundAmount = amount(refundRepository.sumApprovedForDashboard(owner, start, end, APPROVED));

        Map<String, String> departments = loadDepartments(owner, normalizedStore, start, end);
        List<Object[]> arrivalRows = invitationRepository.aggregateArrivalsForDashboard(owner, normalizedStore, start, end);
        List<Object[]> salesRows = orderRepository.aggregateSalesRanking(owner, start, end, CANCELLED);
        List<DashboardResponse.VisitRank> visitRanking = buildVisitRanking(departments, arrivalRows, salesRows);
        List<DashboardResponse.SalesRank> salesRanking = buildSalesRanking(departments, salesRows);
        return new DashboardResponse(totalCustomers, newCustomers, followUpsToday, pendingTasks, closedCustomers,
                pipeline, salesAmount, paidAmount, callDuration, deepCalls, arrivedCustomers, pendingService,
                activeService, expiringService, refundAmount, visitRanking, salesRanking);
    }

    private Map<String, String> loadDepartments(String owner, String store, LocalDateTime start, LocalDateTime end) {
        Map<String, String> departments = new HashMap<>();
        for (Object[] row : invitationRepository.aggregateEmployeesForDashboard(owner, store, start, end)) {
            String employee = (String) row[0];
            String department = row[1] == null ? DEFAULT_DEPARTMENT : (String) row[1];
            departments.putIfAbsent(employee, department);
        }
        return departments;
    }

    private List<DashboardResponse.VisitRank> buildVisitRanking(Map<String, String> departments,
            List<Object[]> arrivalRows, List<Object[]> salesRows) {
        Map<String, VisitAggregate> aggregates = new HashMap<>();
        departments.forEach((employee, department) -> aggregates.put(employee, new VisitAggregate(department)));
        for (Object[] row : arrivalRows) {
            String employee = (String) row[0];
            VisitAggregate aggregate = aggregates.computeIfAbsent(employee,
                    key -> new VisitAggregate(departmentFor(employee, departments)));
            aggregate.arrived += number(row[2]);
        }
        for (Object[] row : salesRows) {
            String employee = (String) row[0];
            aggregates.computeIfAbsent(employee, key -> new VisitAggregate(departmentFor(employee, departments))).closed = number(row[3]);
        }
        return rankVisits(aggregates);
    }

    private List<DashboardResponse.SalesRank> buildSalesRanking(Map<String, String> departments, List<Object[]> salesRows) {
        List<SalesAggregate> aggregates = new ArrayList<>();
        for (String employee : departments.keySet()) {
            aggregates.add(new SalesAggregate(employee, BigDecimal.ZERO, BigDecimal.ZERO));
        }
        for (Object[] row : salesRows) {
            String employee = (String) row[0];
            aggregates.removeIf(aggregate -> aggregate.employee().equals(employee));
            aggregates.add(new SalesAggregate(employee, amount(row[1]), amount(row[2])));
        }
        aggregates.sort(Comparator.comparing(SalesAggregate::sales).reversed().thenComparing(SalesAggregate::employee));
        List<DashboardResponse.SalesRank> result = new ArrayList<>(aggregates.size());
        int rank = 1;
        for (SalesAggregate aggregate : aggregates) {
            BigDecimal completion = aggregate.sales.signum() == 0 ? BigDecimal.ZERO
                    : aggregate.paid.multiply(BigDecimal.valueOf(100)).divide(aggregate.sales, 1, RoundingMode.HALF_UP);
            result.add(new DashboardResponse.SalesRank(rank++, aggregate.employee, departments.getOrDefault(aggregate.employee, DEFAULT_DEPARTMENT),
                    aggregate.sales, aggregate.paid, completion));
        }
        return result;
    }

    private List<DashboardResponse.VisitRank> rankVisits(Map<String, VisitAggregate> aggregates) {
        List<Map.Entry<String, VisitAggregate>> rows = new ArrayList<>(aggregates.entrySet());
        rows.sort(Comparator.comparingLong((Map.Entry<String, VisitAggregate> row) -> row.getValue().arrived)
                .reversed().thenComparing(Map.Entry::getKey));
        List<DashboardResponse.VisitRank> result = new ArrayList<>(rows.size());
        int rank = 1;
        for (Map.Entry<String, VisitAggregate> row : rows) {
            VisitAggregate aggregate = row.getValue();
            BigDecimal conversion = aggregate.arrived == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(aggregate.closed).multiply(BigDecimal.valueOf(100))
                            .divide(BigDecimal.valueOf(aggregate.arrived), 1, RoundingMode.HALF_UP);
            result.add(new DashboardResponse.VisitRank(rank++, aggregate.department, row.getKey(), aggregate.arrived,
                    aggregate.closed, conversion));
        }
        return result;
    }

    private static long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static BigDecimal amount(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return new BigDecimal(number.toString());
        throw new IllegalArgumentException("聚合金额类型不受支持: " + value.getClass().getName());
    }

    private static String departmentFor(String employee, Map<String, String> departments) {
        return departments.getOrDefault(employee, DEFAULT_DEPARTMENT);
    }

    private static final class VisitAggregate {
        private final String department;
        private long arrived;
        private long closed;

        private VisitAggregate(String department) { this.department = department; }
    }

    private record SalesAggregate(String employee, BigDecimal sales, BigDecimal paid) {}
}
