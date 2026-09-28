package com.youai.crm.customer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.youai.crm.account.CrmUser;
import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.communication.SystemNotification;
import com.youai.crm.communication.SystemNotificationRepository;
import com.youai.crm.system.CustomerPoolRule;
import com.youai.crm.system.CustomerPoolRuleService;

@Service
public class CustomerPoolScheduler {
    private static final String POOL = "公海";
    private final CustomerRepository customers;
    private final CustomerAssignmentEventRepository assignments;
    private final CustomerOperationEventRepository operations;
    private final SystemNotificationRepository notifications;
    private final CrmUserRepository users;
    private final CustomerPoolRuleService poolRuleService;
    public CustomerPoolScheduler(CustomerRepository customers, CustomerAssignmentEventRepository assignments,
            CustomerOperationEventRepository operations, SystemNotificationRepository notifications, CrmUserRepository users,
            CustomerPoolRuleService poolRuleService) {
        this.customers = customers; this.assignments = assignments; this.operations = operations; this.notifications = notifications;
        this.users = users;
        this.poolRuleService = poolRuleService;
    }
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void releaseStaleCustomers() {
        CustomerPoolRule rule = poolRuleService.getCustomerPoolRule();
        if (rule.enabled()) releaseStaleCustomers(LocalDateTime.now(), rule.days());
    }
    @Transactional
    int releaseStaleCustomers(LocalDateTime now, int days) {
        LocalDateTime cutoffExclusive = now.toLocalDate().minusDays(days).plusDays(1).atStartOfDay();
        List<Customer> stale = new ArrayList<>(customers.findAllByOwnerNotAndLastContactAtLessThan(POOL, cutoffExclusive));
        stale.addAll(customers.findAllByOwnerNotAndLastContactAtIsNullAndCreatedAtLessThan(POOL, cutoffExclusive));
        int released = 0;
        for (Customer customer : stale) {
            String previousOwner = customer.getOwner();
            if (POOL.equals(previousOwner) || "白板".equals(previousOwner)) continue;
            // Only assigned sales-library resources participate. Whiteboard,
            // public-pool, administrator-owned, and historical/non-user owner
            // values remain untouched.
            CrmUser salesOwner = employeeOwner(previousOwner);
            if (salesOwner == null || isAdministrator(previousOwner)) continue;
            customer.setPreviousOwner(previousOwner); customer.setOwner(POOL); customer.setLastAllocationAt(now);
            customer.setPoolEntryType("未及时跟进，系统推进");
            customers.save(customer);
            CustomerAssignmentEvent assignment = new CustomerAssignmentEvent();
            assignment.setCustomerId(customer.getId()); assignment.setPreviousOwner(previousOwner); assignment.setOwner(POOL);
            assignment.setType("未及时跟进，系统推进"); assignment.setReason("员工用户超过" + days + "个自然日未跟进"); assignment.setOperator("system"); assignment.setAssignedAt(now);
            assignments.save(assignment);
            CustomerOperationEvent operation = new CustomerOperationEvent();
            operation.setCustomerId(customer.getId()); operation.setOperationType("系统自动归海");
            operation.setDetail("员工用户" + previousOwner + "超过" + days + "个自然日未跟进，客户流入公海"); operation.setOperator("system"); operations.save(operation);
            String key = "customer-pool-release:" + customer.getId() + ":" + now.toLocalDate();
            if (notifications.findByNotificationKey(key).isEmpty()) {
                SystemNotification notification = new SystemNotification(); notification.setUsername(salesOwner.getUsername()); notification.setNotificationKey(key);
                notification.setTitle("客户已流入公海"); notification.setContent("客户「" + customer.getName() + "」超过" + days + "个自然日未跟进，已自动流入公海列表。");
                notification.setCustomerNo(customer.getCustomerNo()); notifications.save(notification);
            }
            released++;
        }
        return released;
    }

    private CrmUser employeeOwner(String owner) {
        if (users == null || owner == null) return null;
        return users.findAllByOrderByDisplayNameAsc().stream()
                .filter(user -> user.getRoles().stream().noneMatch(role -> "ADMIN".equals(role.getCode())))
                .filter(user -> owner.equalsIgnoreCase(user.getDisplayName()) || owner.equalsIgnoreCase(user.getUsername()))
                .findFirst().orElse(null);
    }

    private boolean isAdministrator(String owner) {
        if (users == null || owner == null) return false;
        return users.findAllByOrderByDisplayNameAsc().stream()
                .filter(user -> owner.equalsIgnoreCase(user.getDisplayName())
                        || owner.equalsIgnoreCase(user.getUsername()))
                .anyMatch(user -> user.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getCode())));
    }
}
