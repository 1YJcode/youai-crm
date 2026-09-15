package com.youai.crm.customer;

import java.time.LocalDateTime;

public record FollowUpRecordResponse(
        String id,
        String customerId,
        String customer,
        String type,
        String channel,
        String title,
        String content,
        String customerStatus,
        String owner,
        LocalDateTime occurredAt,
        boolean completed,
        String category) {
}
