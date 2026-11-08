package com.youai.crm.communication;

import jakarta.validation.constraints.Pattern;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageRequest(
        @NotBlank(message = "消息内容不能为空")
        @Size(max = 2000, message = "消息内容不能超过 2000 个字符")
        String content,
        @Pattern(regexp = "SMS|INTERNAL", message = "不支持的消息渠道")
        String channel,
        @Pattern(regexp = "[a-zA-Z0-9-]{16,64}", message = "发送请求编号格式错误")
        String requestId) {
}
