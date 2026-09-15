package com.youai.crm.customer;

import java.time.LocalDateTime;

public record CustomerAssignmentEventResponse(
        Long id,
        String previousOwner,
        String owner,
        String type,
        String maturity,
        String reason,
        String operator,
        LocalDateTime assignedAt) {

    static CustomerAssignmentEventResponse from(CustomerAssignmentEvent event) {
        return new CustomerAssignmentEventResponse(event.getId(), event.getPreviousOwner(), event.getOwner(),
                event.getType(), event.getMaturity(), event.getReason(), event.getOperator(), event.getAssignedAt());
    }
}
