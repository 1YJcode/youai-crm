package com.youke.crm.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.youke.crm.account.AccessPolicy;
import com.youke.crm.communication.CallRecordRepository;
import com.youke.crm.customer.CustomerRepository;
import com.youke.crm.invitation.InvitationRecordRepository;
import com.youke.crm.order.OrderRefundRepository;
import com.youke.crm.order.SalesOrderRepository;
import com.youke.crm.task.FollowUpTaskRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final CustomerRepository customerRepository;
    private final FollowUpTaskRepository taskRepository;
    private final SalesOrderRepository orderRepository;
    private final CallRecordRepository callRepository;
    private final InvitationRecordRepository invitationRepository;
    private final OrderRefundRepository refundRepository;
    private final AccessPolicy accessPolicy;

    public DashboardController(
            CustomerRepository customerRepository,
            FollowUpTaskRepository taskRepository,
            SalesOrderRepository orderRepository,
            CallRecordRepository callRepository,
            InvitationRecordRepository invitationRepository,
            OrderRefundRepository refundRepository,
            AccessPolicy accessPolicy) {
        this.customerRepository = customerRepository;
        this.taskRepository = taskRepository;
        this.orderRepository = orderRepository;
        this.callRepository = callRepository;
        this.invitationRepository = invitationRepository;
        this.refundRepository = refundRepository;
        this.accessPolicy = accessPolicy;
    }

    @GetMapping
    public DashboardResponse summary(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String store,
            Authentication authentication) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = from == null ? today.withDayOfMonth(1) : from;
        LocalDate endDate = to == null ? today : to;
        if (endDate.isBefore(startDate)) throw new IllegalArgumentException("结束日期不能早于开始日期");
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();
        accessPolicy.scopedOwner(authentication); // also rejects direct calls without an identity
        var visibleCustomers = customerRepository.findAll().stream()
                .filter(customer -> accessPolicy.canAccessOwner(customer.getOwner(), authentication))
                .toList();
        var visibleTasks = taskRepository.findAll().stream()
                .filter(task -> accessPolicy.canAccessOwner(task.getOwner(), authentication))
                .toList();
        var visibleOrders = orderRepository.findAll().stream()
                .filter(order -> accessPolicy.canAccessOwner(order.getOwner(), authentication))
                .filter(order -> inRange(order.getCreatedAt(), start, end))
                .toList();
        var visibleCalls = callRepository.findAll().stream()
                .filter(call -> accessPolicy.canAccessOwner(call.getOwner(), authentication))
                .filter(call -> inRange(call.getStartedAt(), start, end))
                .toList();
        var visibleInvitations = invitationRepository.findAll().stream()
                .filter(invitation -> accessPolicy.canAccessOwner(invitation.getInviter(), authentication))
                .filter(invitation -> store == null || store.isBlank() || store.equals(invitation.getStoreName()))
                .filter(invitation -> inRange(invitation.getScheduledAt(), start, end))
                .toList();
        var visibleRefunds = refundRepository.findAll().stream()
                .filter(refund -> accessPolicy.canAccessOwner(refund.getOwner(), authentication))
                .filter(refund -> inRange(refund.getCreatedAt(), start, end))
                .toList();
        long customers = visibleCustomers.size();
        long followUpsToday = visibleTasks.stream()
                .filter(task -> inRange(task.getDueAt(), start, end))
                .map(task -> task.getCustomerName()).distinct().count();
        long newCustomers = visibleCustomers.stream()
                .filter(customer -> inRange(customer.getCreatedAt(), start, end))
                .count();
        BigDecimal pipeline = visibleCustomers.stream()
                .map(customer -> customer.getExpectedAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long closedCustomers = visibleCustomers.stream()
                .filter(customer -> "已成交".equals(customer.getStage()))
                .count();
        BigDecimal salesAmount = visibleOrders.stream()
                .filter(order -> !"已取消".equals(order.getPaymentStatus()))
                .map(order -> order.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paidAmount = visibleOrders.stream()
                .map(order -> order.getPaidAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long arrivedCustomers = visibleInvitations.stream()
                .filter(invitation -> invitation.getArrivalAt() != null)
                .map(invitation -> invitation.getCustomerNo()).distinct().count();
        Map<String, String> departments = new HashMap<>();
        visibleInvitations.forEach(invitation -> departments.putIfAbsent(invitation.getInviter(), invitation.getDepartment()));
        var owners = new java.util.HashSet<String>();
        visibleInvitations.forEach(invitation -> owners.add(invitation.getInviter()));
        visibleOrders.forEach(order -> owners.add(order.getOwner()));
        var visitRanking = owners.stream().map(employee -> {
            long arrived = visibleInvitations.stream().filter(i -> employee.equals(i.getInviter()) && i.getArrivalAt() != null)
                    .map(i -> i.getCustomerNo()).distinct().count();
            long closed = visibleOrders.stream().filter(o -> employee.equals(o.getOwner()) && o.getPaidAmount().signum() > 0)
                    .map(o -> o.getCustomerName()).distinct().count();
            BigDecimal rate = arrived == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(closed * 100.0 / arrived).setScale(1, RoundingMode.HALF_UP);
            return new DashboardResponse.VisitRank(0, departments.getOrDefault(employee, "销售部"), employee, arrived, closed, rate);
        }).sorted(Comparator.comparingLong(DashboardResponse.VisitRank::arrivedCustomers).reversed()).toList();
        var rankedVisits = new java.util.ArrayList<DashboardResponse.VisitRank>();
        for (int i = 0; i < visitRanking.size(); i++) {
            var row = visitRanking.get(i);
            rankedVisits.add(new DashboardResponse.VisitRank(i + 1, row.department(), row.employee(), row.arrivedCustomers(), row.closedCustomers(), row.conversionRate()));
        }
        var salesRanking = owners.stream().map(employee -> {
            BigDecimal sales = visibleOrders.stream().filter(o -> employee.equals(o.getOwner()) && !"已取消".equals(o.getPaymentStatus()))
                    .map(o -> o.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal paid = visibleOrders.stream().filter(o -> employee.equals(o.getOwner()))
                    .map(o -> o.getPaidAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal rate = sales.signum() == 0 ? BigDecimal.ZERO : paid.multiply(BigDecimal.valueOf(100)).divide(sales, 1, RoundingMode.HALF_UP);
            return new DashboardResponse.SalesRank(0, employee, departments.getOrDefault(employee, "销售部"), sales, paid, rate);
        }).sorted(Comparator.comparing(DashboardResponse.SalesRank::salesAmount).reversed()).toList();
        var rankedSales = new java.util.ArrayList<DashboardResponse.SalesRank>();
        for (int i = 0; i < salesRanking.size(); i++) {
            var row = salesRanking.get(i);
            rankedSales.add(new DashboardResponse.SalesRank(i + 1, row.employee(), row.department(), row.salesAmount(), row.paidAmount(), row.completionRate()));
        }
        BigDecimal refundAmount = visibleRefunds.stream().filter(refund -> "已通过".equals(refund.getStatus()))
                .map(refund -> refund.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardResponse(
                customers,
                newCustomers,
                followUpsToday,
                visibleTasks.stream().filter(task -> !task.isCompleted()).count(),
                closedCustomers,
                pipeline,
                salesAmount,
                paidAmount,
                visibleCalls.stream().mapToLong(call -> call.getDurationSeconds()).sum(),
                visibleCalls.stream().filter(call -> call.getDurationSeconds() >= 300).count(),
                arrivedCustomers,
                visibleOrders.stream().filter(order -> "未开始".equals(order.getServiceStatus())).count(),
                visibleOrders.stream().filter(order -> "实施中".equals(order.getServiceStatus()) || "已开通".equals(order.getServiceStatus())).count(),
                visibleOrders.stream().filter(order -> "待开通".equals(order.getServiceStatus())).count(),
                refundAmount,
                rankedVisits,
                rankedSales);
    }

    private boolean inRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }
}
