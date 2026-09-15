package com.youai.crm.communication;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        String sender,
        String direction,
        String content,
        LocalDateTime sentAt,
        boolean read) {

    public static MessageResponse from(CrmMessage message) {
        return new MessageResponse(message.getId(), message.getConversationId(), message.getSender(),
                message.getDirection(), message.getContent(), message.getSentAt(), message.isRead());
    }
}
