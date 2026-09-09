package com.youke.crm.task;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TaskRequest(
        @NotBlank(message = "任务标题不能为空")
        @Size(max = 2000, message = "跟进内容不能超过 2000 个字符")
        String title,

        @NotBlank(message = "关联客户不能为空")
        @Size(max = 64, message = "客户名称不能超过 64 个字符")
        String customer,

        @Size(max = 32, message = "客户编号不能超过 32 个字符")
        String customerId,

        @Size(max = 32, message = "客户状态不能超过 32 个字符")
        String customerStatus,

        @NotBlank(message = "负责人不能为空")
        @Size(max = 32, message = "负责人不能超过 32 个字符")
        String owner,

        @NotNull(message = "截止时间不能为空")
        LocalDateTime dueAt,

        @NotBlank(message = "任务类型不能为空")
        @Size(max = 32, message = "任务类型不能超过 32 个字符")
        String type,

        @Size(max = 24, message = "优先级不能超过 24 个字符")
        String priority,

        Boolean completed) {
}
