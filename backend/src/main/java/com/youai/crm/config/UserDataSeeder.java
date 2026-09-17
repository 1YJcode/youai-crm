package com.youai.crm.config;

import java.util.Set;

import com.youai.crm.account.CrmUser;
import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.account.Department;
import com.youai.crm.account.DepartmentRepository;
import com.youai.crm.account.Role;
import com.youai.crm.account.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class UserDataSeeder {

    @Bean
    @Order(1)
    CommandLineRunner seedAccounts(
            DepartmentRepository departmentRepository,
            RoleRepository roleRepository,
            CrmUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            Department adminDepartment = departmentRepository.findByCode("ADMIN")
                    .orElseGet(() -> departmentRepository.save(department("ADMIN", "管理中心")));
            Department salesDepartment = departmentRepository.findByCode("SALES")
                    .orElseGet(() -> departmentRepository.save(department("SALES", "销售一部")));
            Role adminRole = roleRepository.findByCode("ADMIN")
                    .orElseGet(() -> roleRepository.save(role("ADMIN", "系统管理员", "管理账号、组织和全部业务数据")));
            Role salesRole = roleRepository.findByCode("SALES")
                    .orElseGet(() -> roleRepository.save(role("SALES", "销售顾问", "查看和跟进本人负责的客户")));
            seedRole(roleRepository, "OPERATIONS", "运营", "运营管理");
            seedRole(roleRepository, "RND", "研发", "研发管理");
            seedRole(roleRepository, "FINANCE", "财务", "财务管理");
            seedRole(roleRepository, "SALES_MANAGER", "销售经理", "销售团队管理");
            seedRole(roleRepository, "STORE_MANAGER", "店长", "门店管理");
            seedRole(roleRepository, "SERVICE_TEACHER", "服务老师", "客户服务");
            seedRole(roleRepository, "SERVICE_MANAGER", "服务经理", "服务团队管理");

            CrmUser adminUser = userRepository.findByUsernameIgnoreCase("admin").orElse(null);
            if (adminUser == null) {
                adminUser = user("admin", "Admin@123", "赵娣", "13800000001", adminDepartment, Set.of(adminRole), passwordEncoder);
                userRepository.save(adminUser);
            } else if (!"赵娣".equals(adminUser.getDisplayName())) {
                adminUser.setDisplayName("赵娣");
                userRepository.save(adminUser);
            }
            ensureUser(userRepository, "admin2", "Admin2@123", "系统管理员2", "13800000004", adminDepartment, Set.of(adminRole), passwordEncoder);
            ensureUser(userRepository, "linxi", "Linxi@123", "林夕", "13800000002", salesDepartment, Set.of(salesRole), passwordEncoder);
            ensureUser(userRepository, "chenchen", "Sales@123", "陈晨", "13800000005", salesDepartment, Set.of(salesRole), passwordEncoder);
            ensureUser(userRepository, "zhouqian", "Sales@123", "周倩", "13800000006", salesDepartment, Set.of(salesRole), passwordEncoder);
            ensureUser(userRepository, "zhaolei", "Sales@123", "赵磊", "13800000007", salesDepartment, Set.of(salesRole), passwordEncoder);
            disableDuplicateAccount(userRepository, "yuanjiang", "元江");
            ensureUserWithDisplayName(userRepository, "yuanjiang", "Sales@123", "元江", "13800000008", salesDepartment, Set.of(salesRole), passwordEncoder);
        };
    }

    private void ensureUser(
            CrmUserRepository repository,
            String username,
            String password,
            String displayName,
            String phone,
            Department department,
            Set<Role> roles,
            PasswordEncoder passwordEncoder) {
        if (repository.findByUsernameIgnoreCase(username).isEmpty()) {
            repository.save(user(username, password, displayName, phone, department, roles, passwordEncoder));
        }
    }

    private void ensureUserWithDisplayName(
            CrmUserRepository repository,
            String username,
            String password,
            String displayName,
            String phone,
            Department department,
            Set<Role> roles,
            PasswordEncoder passwordEncoder) {
        // A legacy or intentionally frozen account may already exist under the
        // requested username. Do not try to insert it again (the username is
        // unique), and never re-enable it as a side effect of startup seeding.
        CrmUser existingByUsername = repository.findByUsernameIgnoreCase(username).orElse(null);
        if (existingByUsername != null) {
            if (!displayName.equals(existingByUsername.getDisplayName())) {
                existingByUsername.setDisplayName(displayName);
                repository.save(existingByUsername);
            }
            return;
        }
        boolean exists = repository.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
                .anyMatch(existing -> displayName.equals(existing.getDisplayName()));
        if (!exists) {
            repository.save(user(username, password, displayName, phone, department, roles, passwordEncoder));
        }
    }

    private void disableDuplicateAccount(CrmUserRepository repository, String username, String displayName) {
        repository.findByUsernameIgnoreCase(username).ifPresent(candidate -> {
            boolean anotherAccountExists = repository.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
                    .anyMatch(existing -> !existing.getUsername().equalsIgnoreCase(username)
                            && displayName.equals(existing.getDisplayName()));
            if (anotherAccountExists && candidate.isEnabled()) {
                candidate.setEnabled(false);
                repository.save(candidate);
            }
        });
    }

    private Department department(String code, String name) {
        Department department = new Department();
        department.setCode(code);
        department.setName(name);
        return department;
    }

    private Role role(String code, String name, String description) {
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        return role;
    }

    private Role seedRole(RoleRepository repository, String code, String name, String description) {
        return repository.findByCode(code).orElseGet(() -> repository.save(role(code, name, description)));
    }

    private CrmUser user(
            String username, String password, String displayName, String phone,
            Department department, Set<Role> roles, PasswordEncoder encoder) {
        CrmUser user = new CrmUser();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(password));
        user.setDisplayName(displayName);
        user.setPhone(phone);
        user.setDepartment(department);
        user.setRoles(roles);
        user.setEnabled(true);
        return user;
    }
}
