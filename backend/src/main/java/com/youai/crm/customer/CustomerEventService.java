package com.youai.crm.customer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.common.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Owns customer registration, assignment, and operation audit records. */
@Service
public class CustomerEventService {

    private static final String PUBLIC_POOL = "\u516c\u6d77";
    private static final String WHITEBOARD = "\u767d\u677f";
    private static final String SYSTEM_OPERATOR = "\u7cfb\u7edf";

    private final CustomerRegistrationEventRepository registrationEvents;
    private final CustomerAssignmentEventRepository assignmentEvents;
    private final CustomerOperationEventRepository operationEvents;
    private final CustomerRepository customerRepository;
    private final AccessPolicy accessPolicy;

    public CustomerEventService(CustomerRegistrationEventRepository registrationEvents,
            CustomerAssignmentEventRepository assignmentEvents,
            CustomerOperationEventRepository operationEvents,
            CustomerRepository customerRepository, AccessPolicy accessPolicy) {
        this.registrationEvents = registrationEvents;
        this.assignmentEvents = assignmentEvents;
        this.operationEvents = operationEvents;
        this.customerRepository = customerRepository;
        this.accessPolicy = accessPolicy;
    }

    public List<CustomerRegistrationEventResponse> registrationEvents(String customerNo,
            Authentication authentication) {
        Customer customer = authorizedCustomer(customerNo, authentication);
        return registrationEvents.findAllByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .filter(event -> event.getRegistrationNumber() >= 2)
                .map(CustomerRegistrationEventResponse::from).toList();
    }

    public List<CustomerAssignmentEventResponse> assignmentEvents(String customerNo,
            Authentication authentication) {
        Customer customer = authorizedCustomer(customerNo, authentication);
        return assignmentEvents.findAllByCustomerIdOrderByAssignedAtDesc(customer.getId()).stream()
                .map(CustomerAssignmentEventResponse::from).toList();
    }

    public void saveRegistrationEvent(Customer customer, int registrationNumber, String source,
            Authentication authentication) {
        CustomerRegistrationEvent event = new CustomerRegistrationEvent();
        event.setCustomerId(customer.getId());
        event.setRegistrationNumber(registrationNumber);
        event.setSource(defaultText(source, "\u624b\u52a8\u5f55\u5165"));
        event.setOperator(operator(authentication));
        registrationEvents.save(event);
    }

    public void recordAssignmentIfNeeded(Customer customer, String previousOwner, String owner,
            Authentication authentication) {
        recordAssignmentIfNeeded(customer, previousOwner, owner,
                new CustomerAssignmentRequest(owner, null, null, null), authentication);
    }

    public void recordAssignmentIfNeeded(Customer customer, String previousOwner, String owner,
            CustomerAssignmentRequest request, Authentication authentication) {
        CustomerAssignmentRequest details = request == null
                ? new CustomerAssignmentRequest(owner, null, null, null) : request;
        boolean hasDetails = StringUtils.hasText(details.type()) || StringUtils.hasText(details.maturity())
                || StringUtils.hasText(details.reason());
        if (!StringUtils.hasText(owner) || PUBLIC_POOL.equals(owner) || WHITEBOARD.equals(owner)
                || (Objects.equals(previousOwner, owner) && !hasDetails)) return;
        LocalDateTime now = LocalDateTime.now();
        if (customer.getFirstAllocationAt() == null) customer.setFirstAllocationAt(now);
        customer.setLastAllocationAt(now);
        CustomerAssignmentEvent event = new CustomerAssignmentEvent();
        event.setCustomerId(customer.getId());
        event.setPreviousOwner(StringUtils.hasText(previousOwner) ? previousOwner : "\u672a\u5206\u914d");
        event.setOwner(owner);
        event.setType(trimToNull(details.type()));
        event.setMaturity(trimToNull(details.maturity()));
        event.setReason(trimToNull(details.reason()));
        event.setOperator(operator(authentication));
        event.setAssignedAt(now);
        assignmentEvents.save(event);
    }

    public void recordOperation(Customer customer, String operationType, String detail,
            Authentication authentication) {
        CustomerOperationEvent event = new CustomerOperationEvent();
        event.setCustomerId(customer.getId());
        event.setOperationType(operationType);
        event.setDetail(detail);
        event.setOperator(operator(authentication));
        operationEvents.save(event);
    }

    private Customer authorizedCustomer(String customerNo, Authentication authentication) {
        Customer customer = customerRepository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new NotFoundException("\u672a\u627e\u5230\u5ba2\u6237\uff1a" + customerNo));
        accessPolicy.requireOwner(customer.getOwner(), authentication);
        return customer;
    }

    private String operator(Authentication authentication) {
        return authentication == null ? SYSTEM_OPERATOR : accessPolicy.currentDisplayName(authentication);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
