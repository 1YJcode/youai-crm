package com.youai.crm.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youai.crm.account.CrmUser;
import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.account.DepartmentRepository;
import com.youai.crm.account.RoleRepository;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerPoolSchedulerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CustomerRepository customers;
    @Autowired private CustomerPoolScheduler scheduler;
    @Autowired private CrmUserRepository users;
    @Autowired private DepartmentRepository departments;
    @Autowired private RoleRepository roles;

    @BeforeEach
    void ensureSalesUser() {
        if (users.findByUsernameIgnoreCase("linxi").isPresent()) return;
        CrmUser user = new CrmUser();
        user.setUsername("linxi");
        user.setPasswordHash("test-only-password-hash");
        user.setDisplayName("林夕");
        user.setPhone("13810000001");
        user.setEnabled(true);
        user.setDepartment(departments.findByCode("SALES").orElseThrow());
        user.setRoles(Set.of(roles.findByCode("SALES").orElseThrow()));
        users.save(user);
    }

    @Test
    void releasesOnlySalesCustomersAfterSevenNaturalDaysAndIsIdempotent() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 9, 28, 2, 0);
        String suffix = String.format("%08d", Math.abs(System.nanoTime() % 100_000_000L));
        String staleNo = createSalesCustomer("七天未跟进", "138" + suffix);
        String freshNo = createSalesCustomer("尚未到期", "139" + suffix);
        String whiteboardNo = createAdminCustomer("白板不归海", "137" + suffix);

        setLastContact(staleNo, now.minusDays(7));
        setLastContact(freshNo, now.minusDays(6));
        setLastContact(whiteboardNo, now.minusDays(30));

        assertThat(scheduler.releaseStaleCustomers(now, 7)).isEqualTo(1);
        assertThat(customers.findByCustomerNo(staleNo).orElseThrow().getOwner()).isEqualTo("公海");
        assertThat(customers.findByCustomerNo(staleNo).orElseThrow().getPoolEntryType())
                .isEqualTo("未及时跟进，系统推进");
        assertThat(customers.findByCustomerNo(staleNo).orElseThrow().getPreviousOwner()).isEqualTo("林夕");
        assertThat(customers.findByCustomerNo(freshNo).orElseThrow().getOwner()).isEqualTo("林夕");
        assertThat(customers.findByCustomerNo(whiteboardNo).orElseThrow().getOwner()).isEqualTo("白板");
        assertThat(scheduler.releaseStaleCustomers(now, 7)).isZero();
    }

    private String createSalesCustomer(String name, String phone) throws Exception {
        String response = mockMvc.perform(post("/api/customers").with(user("linxi").roles("SALES"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"phone\":\"" + phone
                                + "\",\"source\":\"测试\",\"owner\":\"陈晨\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createAdminCustomer(String name, String phone) throws Exception {
        String response = mockMvc.perform(post("/api/customers").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"phone\":\"" + phone
                                + "\",\"source\":\"测试\",\"owner\":\"林夕\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private void setLastContact(String customerNo, LocalDateTime value) {
        jdbc.update("update crm_customer set last_contact_at = ? where customer_no = ?", value, customerNo);
    }
}
