package com.youai.crm.account;

import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Resolves API references once, when writing data. Never resolves names for authorization. */
@Component
public class UserIdentityResolver {
    private final CrmUserRepository users;

    public UserIdentityResolver(CrmUserRepository users) { this.users = users; }

    public CrmUser enabledUser(String reference) {
        if (!StringUtils.hasText(reference)) throw new IllegalArgumentException("请选择员工账号");
        String value = reference.trim();
        if (value.startsWith("user:")) {
            try {
                return users.findById(Long.valueOf(value.substring(5))).filter(CrmUser::isEnabled)
                        .orElseThrow(() -> new IllegalArgumentException("员工账号不存在或已停用"));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("员工 ID 格式不正确");
            }
        }
        List<CrmUser> matches = users.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
                .filter(user -> value.equalsIgnoreCase(user.getUsername()) || value.equalsIgnoreCase(user.getDisplayName()))
                .toList();
        if (matches.size() != 1) throw new IllegalArgumentException("员工信息不存在或存在重名，请使用员工 ID");
        return matches.getFirst();
    }

    public CrmUser enabledEmployee(String reference) {
        CrmUser user = enabledUser(reference);
        if (user.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getCode()))) {
            throw new IllegalArgumentException("负责人必须选择普通员工账号");
        }
        return user;
    }
}
