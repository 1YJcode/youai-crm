package com.youai.crm.system;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/business-settings")
public class BusinessSettingsController {
    private final CustomerPoolRuleService service;

    public BusinessSettingsController(CustomerPoolRuleService service) {
        this.service = service;
    }

    @GetMapping("/customer-pool")
    public CustomerPoolRule customerPoolRule() {
        return service.getCustomerPoolRule();
    }
}
