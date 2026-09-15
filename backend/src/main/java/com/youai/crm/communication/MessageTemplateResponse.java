package com.youai.crm.communication;

import java.time.LocalDateTime;

public record MessageTemplateResponse(
        String id, String name, String channel, String content, String ownerUsername, LocalDateTime updatedAt) {
    public static MessageTemplateResponse from(MessageTemplate template) {
        return new MessageTemplateResponse(template.getId(), template.getName(), template.getChannel(),
                template.getContent(), template.getOwnerUsername(), template.getUpdatedAt());
    }
}
