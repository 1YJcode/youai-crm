package com.youai.crm.system;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerPoolRuleService {
    private static final String ENABLED_KEY = "customer.pool.auto_release.enabled";
    private static final String DAYS_KEY = "customer.pool.auto_release.days";
    private static final CustomerPoolRule DEFAULT_RULE = new CustomerPoolRule(true, 7);

    private final SystemSettingRepository settings;

    public CustomerPoolRuleService(SystemSettingRepository settings) {
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public CustomerPoolRule getCustomerPoolRule() {
        boolean enabled = settings.findById(ENABLED_KEY)
                .map(setting -> Boolean.parseBoolean(setting.getValue()))
                .orElse(DEFAULT_RULE.enabled());
        int days = settings.findById(DAYS_KEY)
                .map(setting -> parseDays(setting.getValue()))
                .orElse(DEFAULT_RULE.days());
        return new CustomerPoolRule(enabled, days);
    }

    @Transactional
    public CustomerPoolRule updateCustomerPoolRule(UpdateCustomerPoolRuleRequest request) {
        settings.save(setting(ENABLED_KEY, request.enabled().toString()));
        settings.save(setting(DAYS_KEY, request.days().toString()));
        return new CustomerPoolRule(request.enabled(), request.days());
    }

    private SystemSetting setting(String key, String value) {
        return settings.findById(key).map(existing -> {
            existing.setValue(value);
            return existing;
        }).orElseGet(() -> new SystemSetting(key, value));
    }

    private int parseDays(String value) {
        try {
            int days = Integer.parseInt(value);
            return days >= 1 && days <= 3650 ? days : DEFAULT_RULE.days();
        } catch (NumberFormatException ignored) {
            return DEFAULT_RULE.days();
        }
    }
}
