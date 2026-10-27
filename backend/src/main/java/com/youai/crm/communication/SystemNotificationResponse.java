package com.youai.crm.communication;

import java.time.LocalDateTime;

public record SystemNotificationResponse(String id, String title, String content, String customerNo, LocalDateTime createdAt, boolean read,
        Long callId,Long workId,String kind,boolean completed) {}
