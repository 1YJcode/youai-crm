package com.youke.crm.customer;

import java.time.LocalDateTime;

public record CustomerRegistrationEventResponse(
        Long id,
        Integer registrationNumber,
        String source,
        String operator,
        LocalDateTime createdAt) {

    static CustomerRegistrationEventResponse from(CustomerRegistrationEvent event) {
        return new CustomerRegistrationEventResponse(event.getId(), event.getRegistrationNumber(),
                event.getSource(), event.getOperator(), event.getCreatedAt());
    }
}
