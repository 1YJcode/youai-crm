package com.youai.crm.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload used by administrators to reset an employee login password. */
public record ResetPasswordRequest(
        @NotBlank(message = "登录密码不能为空")
        @Size(min = 8, max = 72, message = "登录密码长度为 8-72 个字符")
        String password) {
}
