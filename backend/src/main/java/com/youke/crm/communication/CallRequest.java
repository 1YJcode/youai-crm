package com.youke.crm.communication;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CallRequest(
        @NotBlank(message = "关联客户不能为空")
        String customerId,

        @NotBlank(message = "呼叫方向不能为空")
        String direction,

        @NotBlank(message = "呼叫结果不能为空")
        String status,

        @Min(value = 0, message = "通话时长不能小于 0")
        Integer durationSeconds,

        @Size(max = 1000, message = "通话备注不能超过 1000 个字符")
        String note) {
}
