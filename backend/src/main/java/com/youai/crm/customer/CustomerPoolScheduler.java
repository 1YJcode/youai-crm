package com.youai.crm.customer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.youai.crm.account.CrmUser;
import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.account.LoginProtectionService;
import com.youai.crm.communication.SystemNotification;
import com.youai.crm.communication.SystemNotificationRepository;

@Service
public class CustomerPoolScheduler {
    private static final String POOL = "公海";
    private final CustomerRepository customers;
    private final CustomerAssignmentEventRepository assignments;
    private final CustomerOperationEventRepository operations;
    private final SystemNotificationRepository notifications;
    private final CrmUserRepository users;
    private final LoginProtectionService loginProtection;
    public CustomerPoolScheduler(CustomerRepository customers, CustomerAssignmentEventRepository assignments,
            CustomerOperationEventRepository operations, SystemNotificationRepository notifications, CrmUserRepository users,
            LoginProtectionService loginProtection) {
        this.customers = customers; this.assignments = assignments; this.operations = operations; this.notifications = notifications;
        this.users = users; this.loginProtection = loginProtection;
    }
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void releaseStaleCustomers() { releaseStaleCustomers(LocalDateTime.now()); }
    @Transactional
    int releaseStaleCustomers(LocalDateTime now) {
        LocalDateTime cutoff = now.minusDays(7);
        List<Customer> stale = new ArrayList<>(customers.findAllByOwnerNotAndLastContactAtLessThanEqual(POOL, cutoff));
        stale.addAll(customers.findAllByOwnerNotAndLastContactAtIsNullAndCreatedAtLessThanEqual(POOL, cutoff));
        int released = 0;
        for (Customer customer : stale) {
            String previousOwner = customer.getOwner();
            if (POOL.equals(previousOwner) || "白板".equals(previousOwner)) continue;
            // Historical owners may be frozen, temporarily locked, or already
            // deleted. Their stale customers must still be released; only
            // administrator-owned customers are excluded from this job.
            if (isAdministrator(previousOwner)) continue;
            customer.setPreviousOwner(previousOwner); customer.setOwner(POOL); customer.setLastAllocationAt(now);
            customers.save(customer);
            CustomerAssignmentEvent assignment = new CustomerAssignmentEvent();
            assignment.setCustomerId(customer.getId()); assignment.setPreviousOwner(previousOwner); assignment.setOwner(POOL);
            assignment.setType("系统自动归海"); assignment.setReason("员工用户超过7天未跟进"); assignment.setOperator("system"); assignment.setAssignedAt(now);
            assignments.save(assignment);
            CustomerOperationEvent operation = new CustomerOperationEvent();
            operation.setCustomerId(customer.getId()); operation.setOperationType("系统自动归海");
            operation.setDetail("员工用户" + previousOwner + "超过7天未跟进，客户流入公海"); operation.setOperator("system"); operations.save(operation);
            String key = "customer-pool-release:" + customer.getId() + ":" + now.toLocalDate();
            if (notifications.findByNotificationKey(key).isEmpty()) {
                SystemNotification notification = new SystemNotification(); notification.setUsername(notificationRecipient(previousOwner)); notification.setNotificationKey(key);
                notification.setTitle("客户已流入公海"); notification.setContent("客户「" + customer.getName() + "」超过7天未跟进，已自动流入公海列表。");
                notification.setCustomerNo(customer.getCustomerNo()); notifications.save(notification);
            }
            released++;
        }
        return released;
    }

    private String notificationRecipient(String owner) {
        CrmUser employee = employeeOwner(owner);
        return employee == null ? owner : employee.getUsername();
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
