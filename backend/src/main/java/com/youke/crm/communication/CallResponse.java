package com.youke.crm.communication;

import java.time.LocalDateTime;

public record CallResponse(
        Long id,
        String customerId,
        String customer,
        String phone,
        String owner,
        String agent,
        String direction,
        String status,
        int durationSeconds,
        String note,
        LocalDateTime startedAt) {

    public static CallResponse from(CallRecord call) {
        return new CallResponse(call.getId(), call.getCustomerNo(), call.getCustomerName(), call.getPhone(),
                call.getOwner(), call.getAgent(), call.getDirection(), call.getStatus(), call.getDurationSeconds(),
                call.getNote(), call.getStartedAt());
    }
}
