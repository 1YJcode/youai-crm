package com.youai.crm.communication;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        String customerId,
        String name,
        String company,
        String owner,
        String preview,
        LocalDateTime lastMessageAt,
        long unread) {
}
