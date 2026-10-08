package com.youai.crm.customer;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.youai.crm.account.AccessPolicy;

@Component
public class CustomerResponseMapper {

    private final AccessPolicy accessPolicy;

    public CustomerResponseMapper(AccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public CustomerResponse toResponse(Customer customer, Authentication authentication) {
        return toResponse(customer, authentication, 0);
    }

    public CustomerResponse toResponse(Customer customer, Authentication authentication, int deepTalkDurationSeconds) {
        boolean contactVisible = accessPolicy.canAccessUserId(customer.getOwnerId(), authentication);
        return CustomerResponse.from(customer, contactVisible, deepTalkDurationSeconds);
    }
}
