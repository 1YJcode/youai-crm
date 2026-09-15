package com.youai.crm.order;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record OrderRequest(
        @Size(max = 32, message = "订单编号不能超过 32 个字符")
        String orderNo,

        @NotBlank(message = "客户名称不能为空")
        @Size(max = 64, message = "客户名称不能超过 64 个字符")
        String customer,

        @NotBlank(message = "商品或套餐不能为空")
        @Size(max = 128, message = "商品或套餐不能超过 128 个字符")
        String product,

        @NotNull(message = "订单金额不能为空")
        @PositiveOrZero(message = "订单金额不能小于 0")
        BigDecimal amount,

        @PositiveOrZero(message = "已付金额不能小于 0")
        BigDecimal paid,

        @Size(max = 24, message = "支付状态不能超过 24 个字符")
        String status,

        @Size(max = 24, message = "服务状态不能超过 24 个字符")
        String service,

        @NotBlank(message = "销售负责人不能为空")
        @Size(max = 32, message = "销售负责人不能超过 32 个字符")
        String owner) {
}
