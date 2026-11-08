package com.youai.crm.communication;

import com.youai.crm.customer.Customer;
import com.youai.crm.customer.CustomerRepository;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class GatewayPhoneMatcher {
    private final SmsGatewayClient phones;
    private final CustomerRepository customers;

    public GatewayPhoneMatcher(SmsGatewayClient phones, CustomerRepository customers) {
        this.phones = phones;
        this.customers = customers;
    }

    public Optional<Customer> uniqueCustomer(String phone) {
        String normalized = phones.normalizePhone(phone);
        var candidates = new ArrayList<String>();
        candidates.add(normalized);
        candidates.add(normalized.substring(1));
        if (normalized.startsWith("+86") && normalized.length() == 14) candidates.add(normalized.substring(3));
        var matches = customers.findGatewayPhoneMatches(candidates);
        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }
}
