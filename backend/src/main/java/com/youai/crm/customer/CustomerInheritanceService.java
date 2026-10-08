package com.youai.crm.customer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.account.CrmUser;
import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.common.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Transfers an employee's customers before the employee account is removed. */
@Service
@Transactional
public class CustomerInheritanceService {

    private static final String ADMIN = "ADMIN";
    private static final String TRANSFER_TYPE = "离职继承";
    private static final String TRANSFER_REASON = "员工离职客户继承";

    private final CustomerRepository customerRepository;
    private final CrmUserRepository userRepository;
    private final AccessPolicy accessPolicy;
    private final CustomerEventService eventService;

    public CustomerInheritanceService(CustomerRepository customerRepository,
            CrmUserRepository userRepository, AccessPolicy accessPolicy,
            CustomerEventService eventService) {
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.accessPolicy = accessPolicy;
        this.eventService = eventService;
    }

    public CustomerInheritanceResponse inherit(CustomerInheritanceRequest request,
            Authentication authentication) {
        if (!accessPolicy.isAdmin(authentication)) {
            throw new AccessDeniedException("仅管理员可以办理离职继承");
        }
        if (request == null || !StringUtils.hasText(request.sourceOwner())
                || !StringUtils.hasText(request.targetOwner())) {
            throw new IllegalArgumentException("源员工和接收员工不能为空");
        }

        CrmUser source = resolveSource(request);
        CrmUser target = resolveTarget(request.targetOwner());
        if (source.getId().equals(target.getId())) {
            throw new IllegalArgumentException("接收员工不能与离职员工相同");
        }

        Map<Long, Customer> ownedCustomers = new LinkedHashMap<>();
        for (Customer customer : customerRepository.findAllByOwnerId(source.getId())) {
            ownedCustomers.put(customer.getId(), customer);
        }

        CustomerAssignmentRequest details = new CustomerAssignmentRequest(
                target.getDisplayName(), TRANSFER_TYPE, null, TRANSFER_REASON);
        for (Customer customer : ownedCustomers.values()) {
            String previousOwner = customer.getOwner();
            customer.setPreviousOwner(previousOwner);
            customer.setOwner(target.getDisplayName());
            customer.setOwnerId(target.getId());
            Customer saved = customerRepository.save(customer);
            eventService.recordAssignmentIfNeeded(saved, previousOwner,
                    target.getDisplayName(), details, authentication);
            eventService.recordOperation(saved, TRANSFER_TYPE,
                    "客户由“" + ownerLabel(previousOwner) + "”继承给“"
                            + target.getDisplayName() + "”",
                    authentication);
        }
        return new CustomerInheritanceResponse(source.getDisplayName(),
                target.getDisplayName(), ownedCustomers.size());
    }

    private CrmUser resolveSource(CustomerInheritanceRequest request) {
        if (request.sourceUserId() != null) {
            CrmUser source = userRepository.findById(request.sourceUserId())
                    .orElseThrow(() -> new NotFoundException("离职员工不存在"));
            ensureEmployee(source, "离职员工必须是普通员工账号");
            if (!matchesOwner(request.sourceOwner(), source)) {
                throw new IllegalArgumentException("离职员工信息与账号不匹配");
            }
            return source;
        }
        String candidate = request.sourceOwner().trim();
        List<CrmUser> matches = userRepository.findAllByOrderByDisplayNameAsc().stream()
                .filter(user -> matchesOwner(candidate, user))
                .filter(user -> !hasRole(user, ADMIN))
                .toList();
        if (matches.isEmpty()) {
            throw new NotFoundException("离职员工不存在");
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("离职员工信息匹配到多个账号，请使用员工账号或员工 ID");
        }
        return matches.get(0);
    }

    private CrmUser resolveTarget(String requestedOwner) {
        String candidate = requestedOwner.trim();
        List<CrmUser> matches = userRepository.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
                .filter(user -> matchesOwner(candidate, user))
                .filter(user -> !hasRole(user, ADMIN))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("接收员工账号不存在或已停用");
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("接收员工信息匹配到多个账号，请使用员工账号");
        }
        return matches.get(0);
    }

    private boolean matchesOwner(String candidate, CrmUser user) {
        String value = candidate == null ? "" : candidate.trim();
        return value.equals("user:" + user.getId()) || value.equalsIgnoreCase(user.getDisplayName())
                || value.equalsIgnoreCase(user.getUsername());
    }

    private boolean hasRole(CrmUser user, String roleCode) {
        return user.getRoles().stream()
                .anyMatch(role -> roleCode.equalsIgnoreCase(role.getCode()));
    }

    private void ensureEmployee(CrmUser user, String message) {
        if (hasRole(user, ADMIN)) throw new IllegalArgumentException(message);
    }

    private String ownerLabel(String owner) {
        return StringUtils.hasText(owner) ? owner : "未分配";
    }
}
