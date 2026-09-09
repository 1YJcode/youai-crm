package com.youke.crm.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "账号不能为空")
        @Size(min = 3, max = 32, message = "账号长度为 3-32 个字符")
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_.-]*", message = "账号需以字母开头，只能包含字母、数字、下划线、点或连字符")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度为 8-72 个字符")
        String password,

        @NotBlank(message = "显示名称不能为空")
        @Size(min = 2, max = 64, message = "显示名称长度为 2-64 个字符")
        String displayName,

        @Pattern(regexp = "^$|1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone) {
}
