package com.youke.crm.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefundReviewRequest(
        @NotBlank(message = "审核结果不能为空")
        @Size(max = 24, message = "审核结果不能超过 24 个字符")
        String status) {
}
