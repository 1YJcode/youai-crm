package com.youai.crm.order;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record RefundRequest(
        @NotBlank(message = "订单编号不能为空")
        @Size(max = 32, message = "订单编号不能超过 32 个字符")
        String orderNo,

        @NotNull(message = "退款金额不能为空")
        @Positive(message = "退款金额必须大于 0")
        BigDecimal amount,

        @NotBlank(message = "退款原因不能为空")
        @Size(max = 160, message = "退款原因不能超过 160 个字符")
        String reason) {
}
