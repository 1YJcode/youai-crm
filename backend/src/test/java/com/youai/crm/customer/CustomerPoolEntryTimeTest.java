package com.youai.crm.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerPoolEntryTimeTest {
    @Autowired CustomerRepository customers;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void recordsTransitionsWithoutRefreshingRepeatedEntryOrProfileUpdates() {
        Customer customer = new Customer();
        customer.setOwner("公海");
        assertThat(customer.getPoolEnteredAt()).isNotNull();
        LocalDateTime earlier = LocalDateTime.of(2026, 1, 1, 0, 0);
        customer.setPoolEnteredAt(earlier);
        customer.setOwner("公海");
        customer.setName("更新资料");
        customer.setLastContactAt(LocalDateTime.now());
        assertThat(customer.getPoolEnteredAt()).isEqualTo(earlier);
        customer.setOwner("员工");
        customer.setOwner("公海");
        assertThat(customer.getPoolEnteredAt()).isAfter(earlier);
    }

    @Test
    void manualEntryIsReturnedAndRepeatedEntryPreservesTime() throws Exception {
        Customer customer = fixture("manual", null);
        customer.setOwner("白板");
        customers.save(customer);
        String path = "/api/customers/" + customer.getCustomerNo() + "/pool";
        String first = mvc.perform(patch(path).with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"inPool\":true}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String enteredAt = json.readTree(first).get("poolEnteredAt").asText();
        assertThat(enteredAt).isNotEqualTo("null");
        String again = mvc.perform(patch(path).with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"inPool\":true}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(again).get("poolEnteredAt").asText()).isEqualTo(enteredAt);
    }

    @Test
    void importedAndEditedPoolCustomersHaveEntryTime() throws Exception {
        String phone = "138" + String.format("%08d", Math.abs(System.nanoTime() % 100_000_000L));
        String body = "{\"name\":\"导入公海\",\"phone\":\"" + phone
                + "\",\"source\":\"测试\",\"owner\":\"公海\"}";
        String imported = mvc.perform(post("/api/customers/import").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var record = json.readTree(imported);
        assertThat(record.get("owner").asText()).isEqualTo("公海");
        assertThat(record.get("poolEnteredAt").isNull()).isFalse();

        Customer customer = fixture("edit", null);
        customer.setOwner("白板");
        customers.save(customer);
        String editBody = "{\"name\":\"编辑入海\",\"phone\":\"" + customer.getPhone()
                + "\",\"source\":\"测试\",\"owner\":\"公海\"}";
        String edited = mvc.perform(put("/api/customers/" + customer.getCustomerNo())
                .with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(editBody))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(edited).get("poolEnteredAt").isNull()).isFalse();
    }

    @Test
    void filtersEntryTimeWithInclusiveEndDateSingleBoundsAndPagination() throws Exception {
        String prefix = "range" + System.nanoTime();
        fixture(prefix + "a", LocalDateTime.of(2026, 3, 10, 0, 0));
        fixture(prefix + "b", LocalDateTime.of(2026, 3, 10, 23, 59, 59));
        fixture(prefix + "c", LocalDateTime.of(2026, 3, 11, 0, 0));
        fixture(prefix + "d", null);
        var bounded = query(prefix, "2026-03-10", "2026-03-10");
        assertThat(bounded.get("totalElements").asInt()).isEqualTo(2);
        assertThat(bounded.get("content").size()).isEqualTo(1);
        assertThat(query(prefix, "2026-03-11", "").get("totalElements").asInt()).isEqualTo(1);
        assertThat(query(prefix, "", "2026-03-10").get("totalElements").asInt()).isEqualTo(2);
        assertThat(query(prefix, "", "").get("totalElements").asInt()).isEqualTo(4);
    }

    private com.fasterxml.jackson.databind.JsonNode query(String prefix, String start, String end) throws Exception {
        String response = mvc.perform(get("/api/customers/pool").with(user("admin").roles("ADMIN"))
                .param("keyword", prefix).param("poolEntryStart", start).param("poolEntryEnd", end)
                .param("size", "1"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private Customer fixture(String name, LocalDateTime enteredAt) {
        Customer customer = new Customer();
        String unique = Long.toString(System.nanoTime());
        customer.setCustomerNo(unique);
        customer.setName(name);
        customer.setPhone("139" + String.format("%08d", Math.abs(System.nanoTime() % 100_000_000L)));
        customer.setCompany("测试");
        customer.setSource("测试");
        customer.setOwner("公海");
        customer.setPoolEnteredAt(enteredAt);
        customer.setStage("初步沟通");
        customer.setLevel("普通客户");
        return customers.save(customer);
    }
}
