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

@org.springframework.context.annotation.Profile("dev & !prod")
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
            Department adminDepartment = departmentRepository.findByCode("ADMIN").orElseThrow();
            Department salesDepartment = departmentRepository.findByCode("SALES").orElseThrow();
            Role adminRole = roleRepository.findByCode("ADMIN").orElseThrow();
            Role salesRole = roleRepository.findByCode("SALES").orElseThrow();
            ensureUser(userRepository, "admin", "Admin@123", "赵娣", "13800000001", adminDepartment, Set.of(adminRole), passwordEncoder);
            ensureUser(userRepository, "admin2", "Admin2@123", "系统管理员2", "13800000004", adminDepartment, Set.of(adminRole), passwordEncoder);
            ensureUser(userRepository, "linxi", "Linxi@123", "林夕", "13800000002", salesDepartment, Set.of(salesRole), passwordEncoder);
            ensureUser(userRepository, "chenchen", "Sales@123", "陈晨", "13800000005", salesDepartment, Set.of(salesRole), passwordEncoder);
            ensureUser(userRepository, "zhouqian", "Sales@123", "周倩", "13800000006", salesDepartment, Set.of(salesRole), passwordEncoder);
            ensureUser(userRepository, "zhaolei", "Sales@123", "赵磊", "13800000007", salesDepartment, Set.of(salesRole), passwordEncoder);
            ensureUser(userRepository, "yuanjiang", "Sales@123", "元江", "13800000008", salesDepartment, Set.of(salesRole), passwordEncoder);
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
