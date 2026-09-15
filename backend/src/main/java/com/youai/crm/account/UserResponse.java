package com.youai.crm.account;

import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        String phone,
        String departmentCode,
        String departmentName,
        List<String> roles,
        boolean enabled) {

    public static UserResponse from(CrmUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getPhone(),
                user.getDepartment().getCode(),
                user.getDepartment().getName(),
                user.getRoles().stream().map(Role::getCode).sorted().toList(),
                user.isEnabled());
    }
}

