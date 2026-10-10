package com.youai.crm.customer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.communication.CallRecordRepository;
import com.youai.crm.task.FollowUpTaskRepository;

@Component
public class CustomerResponseMapper {

    private final AccessPolicy accessPolicy;
    private final FollowUpTaskRepository taskRepository;
    private final CallRecordRepository callRepository;

    public CustomerResponseMapper(AccessPolicy accessPolicy, FollowUpTaskRepository taskRepository,
            CallRecordRepository callRepository) {
        this.accessPolicy = accessPolicy;
        this.taskRepository = taskRepository;
        this.callRepository = callRepository;
    }

    public CustomerResponse toResponse(Customer customer, Authentication authentication) {
        return toResponse(customer, authentication, 0);
    }

    public CustomerResponse toResponse(Customer customer, Authentication authentication, int deepTalkDurationSeconds) {
        long followUpCount = followUpCounts(List.of(customer)).getOrDefault(customer.getCustomerNo(), 0L);
        return toResponse(customer, authentication, deepTalkDurationSeconds, followUpCount);
    }

    public CustomerResponse toResponse(Customer customer, Authentication authentication, int deepTalkDurationSeconds,
            long followUpCount) {
        boolean contactVisible = accessPolicy.canAccessUserId(customer.getOwnerId(), authentication);
        return CustomerResponse.from(customer, contactVisible, deepTalkDurationSeconds, followUpCount);
    }

    public Map<String, Long> followUpCounts(Collection<Customer> customers) {
        List<String> customerNos = customers.stream().map(Customer::getCustomerNo).distinct().toList();
        if (customerNos.isEmpty()) return Map.of();
        Map<String, Long> counts = new HashMap<>();
        taskRepository.countByCustomerNos(customerNos)
                .forEach(count -> counts.merge(count.getCustomerNo(), count.getRecordCount(), Long::sum));
        callRepository.countByCustomerNos(customerNos)
                .forEach(count -> counts.merge(count.getCustomerNo(), count.getRecordCount(), Long::sum));
        return counts;
    }
}
