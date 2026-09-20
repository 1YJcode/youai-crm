package com.youai.crm.customer;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Compatibility facade for customer use cases.
 *
 * <p>The HTTP layer and a few integrations historically depended on this
 * class. The actual responsibilities now live in focused services so a
 * change to search, commands, assignment, inheritance, or audit events is
 * isolated to its own component.</p>
 */
@Service
public class CustomerService {

    private final CustomerQueryService queryService;
    private final CustomerCommandService commandService;
    private final CustomerAssignmentService assignmentService;
    private final CustomerInheritanceService inheritanceService;
    private final CustomerEventService eventService;

    public CustomerService(CustomerQueryService queryService,
            CustomerCommandService commandService,
            CustomerAssignmentService assignmentService,
            CustomerInheritanceService inheritanceService,
            CustomerEventService eventService) {
        this.queryService = queryService;
        this.commandService = commandService;
        this.assignmentService = assignmentService;
        this.inheritanceService = inheritanceService;
        this.eventService = eventService;
    }

    public Page<CustomerResponse> search(String keyword, String stage, String level, String owner, String tag,
            Boolean inPool, Authentication authentication) {
        return queryService.search(keyword, stage, level, owner, tag, inPool, authentication);
    }

    public Page<CustomerResponse> search(String keyword, String stage, String level, String owner, String tag,
            Boolean inPool, Map<String, String> advanced, Pageable pageable, Authentication authentication) {
        return queryService.search(keyword, stage, level, owner, tag, inPool, advanced, pageable, authentication);
    }

    public Page<CustomerResponse> pool(Pageable pageable, String deepTalkDuration, Authentication authentication) {
        return queryService.pool(pageable, deepTalkDuration, authentication);
    }

    public Map<String, Long> tags(Authentication authentication) {
        return queryService.tags(authentication);
    }

    public CustomerResponse find(String customerNo, Authentication authentication) {
        return queryService.find(customerNo, authentication);
    }

    public CustomerResponse create(CustomerRequest request) {
        return commandService.create(request);
    }

    public CustomerResponse create(CustomerRequest request, Authentication authentication) {
        return commandService.create(request, authentication);
    }

    public CustomerResponse create(CustomerRequest request, Authentication authentication, String registrationSource) {
        return commandService.create(request, authentication, registrationSource);
    }

    public List<CustomerRegistrationEventResponse> registrationEvents(String customerNo,
            Authentication authentication) {
        return eventService.registrationEvents(customerNo, authentication);
    }

    public List<CustomerAssignmentEventResponse> assignmentEvents(String customerNo,
            Authentication authentication) {
        return eventService.assignmentEvents(customerNo, authentication);
    }

    public CustomerResponse update(String customerNo, CustomerRequest request, Authentication authentication) {
        return commandService.update(customerNo, request, authentication);
    }

    public CustomerResponse updateStage(String customerNo, String stage, Authentication authentication) {
        return commandService.updateStage(customerNo, stage, authentication);
    }

    public CustomerResponse updatePool(String customerNo, boolean inPool, Authentication authentication) {
        return assignmentService.updatePool(customerNo, inPool, authentication);
    }

    public CustomerResponse assign(String customerNo, CustomerAssignmentRequest request,
            Authentication authentication) {
        return assignmentService.assign(customerNo, request, authentication);
    }

    public CustomerInheritanceResponse inherit(CustomerInheritanceRequest request, Authentication authentication) {
        return inheritanceService.inherit(request, authentication);
    }
}
