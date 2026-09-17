package com.youai.crm.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Editable employee fields exposed to administrators. */
public record UpdateEmployeeRequest(
        @NotBlank
        @Size(min = 3, max = 32, message = "用户账号长度为 3-32 个字符")
        @Pattern(regexp = "[A-Za-z][A-Za-z0-9_.-]*", message = "用户账号需以字母开头，只能包含字母、数字、下划线、点或连字符")
        String username,
        @NotBlank @Size(min = 2, max = 64) String displayName,
        @Pattern(regexp = "^$|1[3-9]\\d{9}$") String phone,
        List<String> roles) {
}
