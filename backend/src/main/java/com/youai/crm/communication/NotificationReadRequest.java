package com.youai.crm.communication;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record NotificationReadRequest(
        @NotEmpty(message = "通知编号不能为空") @Size(max = 500, message = "一次最多处理 500 条通知") List<@Size(max = 160, message = "通知编号过长") String> notificationIds,
        boolean read) {
}
