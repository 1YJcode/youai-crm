package com.youai.crm.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Payload used by administrators to add an employee from user management. */
public record CreateEmployeeRequest(
        @NotBlank(message = "用户账号不能为空")
        @Size(min = 3, max = 32, message = "用户账号长度为 3-32 个字符")
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_.-]*", message = "用户账号需以字母开头，只能包含字母、数字、下划线、点或连字符")
        String username,

        @NotBlank(message = "登录密码不能为空")
        @Size(min = 8, max = 72, message = "登录密码长度为 8-72 个字符")
        String password,

        @NotBlank(message = "用户姓名不能为空")
        @Size(min = 2, max = 64, message = "用户姓名长度为 2-64 个字符")
        String displayName,

        @Pattern(regexp = "^$|1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,

        List<String> roles) {
}
