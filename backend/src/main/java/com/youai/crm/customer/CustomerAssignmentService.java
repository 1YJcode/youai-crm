package com.youai.crm.customer;

import java.time.LocalDateTime;
import java.util.Objects;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.account.CrmUser;
import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.communication.SystemNotification;
import com.youai.crm.communication.SystemNotificationRepository;
import com.youai.crm.common.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Owns assignment, public-pool transitions, and assignment notifications. */
@Service
@Transactional
public class CustomerAssignmentService {

    private static final String PUBLIC_POOL = "\u516c\u6d77";
    private final CustomerRepository repository;
    private final AccessPolicy accessPolicy;
    private final CrmUserRepository userRepository;
    private final SystemNotificationRepository notificationRepository;
    private final CustomerOwnershipService ownership;
    private final com.youai.crm.account.UserIdentityResolver identities;
    private final CustomerResponseMapper responseMapper;
    private final CustomerEventService eventService;

    public CustomerAssignmentService(CustomerRepository repository, AccessPolicy accessPolicy,
            CrmUserRepository userRepository, SystemNotificationRepository notificationRepository,
            CustomerOwnershipService ownership, com.youai.crm.account.UserIdentityResolver identities, CustomerResponseMapper responseMapper,
            CustomerEventService eventService) {
        this.repository = repository;
        this.accessPolicy = accessPolicy;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.ownership = ownership;
        this.identities = identities;
        this.responseMapper = responseMapper;
        this.eventService = eventService;
    }

    public CustomerResponse updatePool(String customerNo, boolean inPool, Authentication authentication) {
        Customer customer = repository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new NotFoundException("\u672a\u627e\u5230\u5ba2\u6237\uff1a" + customerNo));
        String previousOwner = customer.getOwner();
        if (inPool) {
            accessPolicy.requireUserId(customer.getOwnerId(), authentication);
            if (!PUBLIC_POOL.equals(customer.getOwner())) customer.setPreviousOwner(customer.getOwner());
            ownership.assign(customer, PUBLIC_POOL);
            customer.setPoolEntryType("主动放弃");
        } else {
            if (!PUBLIC_POOL.equals(customer.getOwner()) && !accessPolicy.isAdmin(authentication)) {
                throw new AccessDeniedException("\u53ea\u80fd\u9886\u53d6\u516c\u6d77\u5ba2\u6237");
            }
            ownership.assignCurrent(customer, authentication);
            customer.setPoolEntryType(null);
            eventService.recordAssignmentIfNeeded(customer, previousOwner, customer.getOwner(), authentication);
        }
        customer.setLastContactAt(LocalDateTime.now());
        Customer saved = repository.save(customer);
        if (inPool && !Objects.equals(previousOwner, PUBLIC_POOL)) {
            eventService.recordOperation(saved, "\u79fb\u5165\u516c\u6d77",
                    "\u5ba2\u6237\u7531\u201c" + ownerLabel(previousOwner) + "\u201d\u79fb\u5165\u516c\u6d77", authentication);
        }
        return responseMapper.toResponse(saved, authentication);
    }

    public CustomerResponse assign(String customerNo, CustomerAssignmentRequest request,
            Authentication authentication) {
        if (!accessPolicy.isAdmin(authentication)) throw new AccessDeniedException("\u4ec5\u7ba1\u7406\u5458\u53ef\u4ee5\u5206\u914d\u5ba2\u6237");
        Customer customer = repository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new NotFoundException("\u672a\u627e\u5230\u5ba2\u6237\uff1a" + customerNo));
        CrmUser employee = identities.enabledEmployee(request.owner());
        String owner = employee.getDisplayName();
        String previousOwner = customer.getOwner();
        Long previousOwnerId = customer.getOwnerId();
        ownership.assign(customer, employee);
        customer.setPoolEntryType(null);
        Customer saved = repository.save(customer);
        eventService.recordAssignmentIfNeeded(saved, previousOwner, owner, request, authentication,
                !Objects.equals(previousOwnerId, saved.getOwnerId()));
        notifyAssignedEmployee(saved, owner, authentication, request);
        return responseMapper.toResponse(saved, authentication);
    }

    private void notifyAssignedEmployee(Customer customer, String owner, Authentication authentication,
            CustomerAssignmentRequest request) {
        CrmUser employee = userRepository.findById(customer.getOwnerId()).orElse(null);
        if (employee == null || !StringUtils.hasText(employee.getUsername())) return;
        String allocationTime = customer.getLastAllocationAt() == null
                ? Long.toString(System.currentTimeMillis()) : customer.getLastAllocationAt().toString();
        String key = "customer-assigned:" + customer.getId() + ":" + allocationTime;
        if (notificationRepository.findByNotificationKey(key).isPresent()) return;
        String operator = authentication == null ? "\u7ba1\u7406\u5458" : accessPolicy.currentDisplayName(authentication);
        String reason = StringUtils.hasText(request.reason()) ? " \u539f\u56e0\uff1a" + request.reason().trim() : "";
        SystemNotification notification = new SystemNotification();
        notification.setUsername(employee.getUsername());
        notification.setNotificationKey(key);
        notification.setTitle("\u6536\u5230\u65b0\u5ba2\u6237");
        notification.setContent(operator + "\u5df2\u5c06\u5ba2\u6237\u300a" + customer.getName() + "\u300b\u5206\u914d\u7ed9\u4f60\uff0c\u53ef\u5728\u5ba2\u6237\u5217\u8868\u67e5\u770b\u3002" + reason);
        notification.setCustomerNo(customer.getCustomerNo());
        notificationRepository.save(notification);
    }

    private String ownerLabel(String owner) {
        return StringUtils.hasText(owner) ? owner : "\u672a\u5206\u914d";
    }
}
