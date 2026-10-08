package com.youai.crm.config;

import com.youai.crm.account.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates one explicitly configured administrator on an empty installation only. */
@Component
@Profile("!dev | prod")
@Order(1)
public class ProductionAccountInitializer implements CommandLineRunner {
    private final CrmUserRepository users;
    private final DepartmentRepository departments;
    private final RoleRepository roles;
    private final PasswordEncoder passwords;
    private final String username;
    private final String password;

    public ProductionAccountInitializer(CrmUserRepository users, DepartmentRepository departments,
            RoleRepository roles, PasswordEncoder passwords,
            @Value("${BOOTSTRAP_ADMIN_USERNAME:}") String username,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
        this.users = users;
        this.departments = departments;
        this.roles = roles;
        this.passwords = passwords;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<CrmUser> existing = users.findAllByOrderByDisplayNameAsc();
        for (CrmUser user : existing) {
            if (user.isEnabled() && List.of("Admin@123", "Admin2@123", "Linxi@123", "Sales@123")
                    .stream().anyMatch(value -> passwords.matches(value, user.getPasswordHash()))) {
                throw new IllegalStateException("启用账号仍使用演示密码，请先更换密码或停用演示账号后再启动生产环境");
            }
        }
        if (!existing.isEmpty()) {
            boolean hasAdministrator = existing.stream().anyMatch(user -> user.isEnabled()
                    && user.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getCode())));
            if (!hasAdministrator) throw new IllegalStateException("已有账号但没有启用的管理员，请恢复管理员账号");
            return;
        }
        if (!username.matches("[A-Za-z][A-Za-z0-9_.-]{2,31}") || password.isBlank()
                || password.length() < 16 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("首次启动须设置 BOOTSTRAP_ADMIN_USERNAME（3-32 位账号）和 BOOTSTRAP_ADMIN_PASSWORD（至少16字符、最多72字节）");
        }
        CrmUser administrator = new CrmUser();
        administrator.setUsername(username);
        administrator.setDisplayName("系统管理员");
        administrator.setPasswordHash(passwords.encode(password));
        administrator.setDepartment(departments.findByCode("ADMIN").orElseThrow());
        administrator.setRoles(Set.of(roles.findByCode("ADMIN").orElseThrow()));
        administrator.setEnabled(true);
        users.save(administrator);
    }
}
