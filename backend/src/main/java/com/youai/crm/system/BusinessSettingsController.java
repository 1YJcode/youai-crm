package com.youai.crm.system;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

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

    @PutMapping("/customer-pool")
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerPoolRule updateCustomerPoolRule(@Valid @RequestBody UpdateCustomerPoolRuleRequest request) {
        return service.updateCustomerPoolRule(request);
    }
}
