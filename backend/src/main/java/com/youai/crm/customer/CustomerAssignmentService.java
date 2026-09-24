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
    private final CustomerQueryService queryService;
    private final CustomerResponseMapper responseMapper;
    private final CustomerEventService eventService;

    public CustomerAssignmentService(CustomerRepository repository, AccessPolicy accessPolicy,
            CrmUserRepository userRepository, SystemNotificationRepository notificationRepository,
            CustomerQueryService queryService, CustomerResponseMapper responseMapper,
            CustomerEventService eventService) {
        this.repository = repository;
        this.accessPolicy = accessPolicy;
        this.userRepository = userRepository;
        this.notificationRepository = notificationRepository;
        this.queryService = queryService;
        this.responseMapper = responseMapper;
        this.eventService = eventService;
    }

    public CustomerResponse updatePool(String customerNo, boolean inPool, Authentication authentication) {
        Customer customer = repository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new NotFoundException("\u672a\u627e\u5230\u5ba2\u6237\uff1a" + customerNo));
        String previousOwner = customer.getOwner();
        if (inPool) {
            accessPolicy.requireOwner(customer.getOwner(), authentication);
            if (!PUBLIC_POOL.equals(customer.getOwner())) customer.setPreviousOwner(customer.getOwner());
            customer.setOwner(PUBLIC_POOL);
            customer.setPoolEntryType("主动放弃");
        } else {
            if (!PUBLIC_POOL.equals(customer.getOwner()) && !accessPolicy.isAdmin(authentication)) {
                throw new AccessDeniedException("\u53ea\u80fd\u9886\u53d6\u516c\u6d77\u5ba2\u6237");
            }
            customer.setOwner(accessPolicy.isAdmin(authentication)
                    ? accessPolicy.currentDisplayName(authentication) : accessPolicy.currentOwner(authentication));
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
        String owner = resolveEmployeeOwner(request.owner());
        String previousOwner = customer.getOwner();
        customer.setOwner(owner);
        customer.setPoolEntryType(null);
        Customer saved = repository.save(customer);
        eventService.recordAssignmentIfNeeded(saved, previousOwner, owner, request, authentication);
        notifyAssignedEmployee(saved, owner, authentication, request);
        return responseMapper.toResponse(saved, authentication);
    }

    private void notifyAssignedEmployee(Customer customer, String owner, Authentication authentication,
            CustomerAssignmentRequest request) {
        CrmUser employee = userRepository.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
                .filter(user -> user.getRoles().stream().noneMatch(role -> "ADMIN".equals(role.getCode())))
                .filter(user -> owner.equalsIgnoreCase(user.getDisplayName()) || owner.equalsIgnoreCase(user.getUsername()))
                .findFirst().orElse(null);
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

    private String resolveEmployeeOwner(String requestedOwner) {
        if (!StringUtils.hasText(requestedOwner) || PUBLIC_POOL.equals(requestedOwner) || "\u767d\u677f".equals(requestedOwner)) {
            throw new IllegalArgumentException("\u8d1f\u8d23\u4eba\u5fc5\u987b\u9009\u62e9\u771f\u5b9e\u5458\u5de5\u8d26\u53f7");
        }
        return userRepository.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
                .filter(user -> requestedOwner.trim().equalsIgnoreCase(user.getDisplayName()))
                .filter(user -> user.getRoles().stream().noneMatch(role -> "ADMIN".equals(role.getCode())))
                .map(CrmUser::getDisplayName).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("\u8d1f\u8d23\u4eba\u8d26\u53f7\u4e0d\u5b58\u5728\uff0c\u8bf7\u9009\u62e9\u771f\u5b9e\u5458\u5de5\u8d26\u53f7"));
    }

    private String ownerLabel(String owner) {
        return StringUtils.hasText(owner) ? owner : "\u672a\u5206\u914d";
    }
}
