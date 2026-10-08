package com.youai.crm.config;

import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.customer.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=${TEST_PROD_DB_URL:jdbc:h2:mem:production_profile_test;MODE=MySQL;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${TEST_DB_USERNAME:sa}", "spring.datasource.password=${TEST_DB_PASSWORD:}", "security.jwt.secret=test-production-secret-at-least-32-characters",
        "BOOTSTRAP_ADMIN_USERNAME=production_admin", "BOOTSTRAP_ADMIN_PASSWORD=Long-Production-Test-Password!"
})
@ActiveProfiles({"dev", "prod"})
@DirtiesContext
class ProductionProfileTest {
    @Autowired ApplicationContext context;
    @Autowired CrmUserRepository users;
    @Autowired CustomerRepository customers;

    @Test
    void productionSuppressesDemoSeedsEvenWhenDevIsAlsoActive() {
        assertTrue(context.getBeansOfType(UserDataSeeder.class).isEmpty());
        assertTrue(context.getBeansOfType(DemoDataSeeder.class).isEmpty());
        assertEquals(1, users.count());
        assertTrue(users.findByUsernameIgnoreCase("production_admin").isPresent());
        assertTrue(users.findByUsernameIgnoreCase("admin").isEmpty());
        assertTrue(users.findByUsernameIgnoreCase("admin2").isEmpty());
        assertEquals(0, customers.count());
    }
}
