package com.youke.crm.communication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageTemplateRequest(
        @NotBlank(message = "模板名称不能为空") @Size(max = 64, message = "模板名称不能超过 64 个字符") String name,
        @NotBlank(message = "发送渠道不能为空") @Size(max = 32, message = "发送渠道不能超过 32 个字符") String channel,
        @NotBlank(message = "模板内容不能为空") @Size(max = 2000, message = "模板内容不能超过 2000 个字符") String content) {
}
