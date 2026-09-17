package com.youai.crm.customer;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.youai.crm.account.AccessPolicy;

@Component
public class CustomerResponseMapper {

    private final AccessPolicy accessPolicy;

    public CustomerResponseMapper(AccessPolicy accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public CustomerResponse toResponse(Customer customer, Authentication authentication) {
        boolean contactVisible = accessPolicy.isAdmin(authentication)
                || (StringUtils.hasText(customer.getOwner())
                    && accessPolicy.canAccessOwner(customer.getOwner(), authentication));
        return CustomerResponse.from(customer, contactVisible);
    }
}
