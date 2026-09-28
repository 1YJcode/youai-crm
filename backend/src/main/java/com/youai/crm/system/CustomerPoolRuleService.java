package com.youai.crm.system;

import org.springframework.stereotype.Service;
@Service
public class CustomerPoolRuleService {
    private static final CustomerPoolRule FIXED_RULE = new CustomerPoolRule(true, 7);

    public CustomerPoolRule getCustomerPoolRule() {
        return FIXED_RULE;
    }
}
