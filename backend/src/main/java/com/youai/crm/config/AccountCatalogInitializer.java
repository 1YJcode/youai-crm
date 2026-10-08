package com.youai.crm.config;

import com.youai.crm.account.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Required reference data, independent of demo accounts or business records. */
@Component
@Order(0)
public class AccountCatalogInitializer implements CommandLineRunner {
    private final DepartmentRepository departments;
    private final RoleRepository roles;

    public AccountCatalogInitializer(DepartmentRepository departments, RoleRepository roles) {
        this.departments = departments;
        this.roles = roles;
    }

    @Override
    @Transactional
    public void run(String... args) {
        department("ADMIN", "管理中心");
        department("SALES", "销售一部");
        role("ADMIN", "系统管理员");
        role("SALES", "销售顾问");
        role("OPERATIONS", "运营");
        role("RND", "研发");
        role("FINANCE", "财务");
        role("SALES_MANAGER", "销售经理");
        role("STORE_MANAGER", "店长");
        role("SERVICE_TEACHER", "服务老师");
        role("SERVICE_MANAGER", "服务经理");
    }

    private void department(String code, String name) {
        if (departments.findByCode(code).isPresent()) return;
        Department department = new Department();
        department.setCode(code);
        department.setName(name);
        departments.save(department);
    }

    private void role(String code, String name) {
        if (roles.findByCode(code).isPresent()) return;
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(name);
        roles.save(role);
    }
}
