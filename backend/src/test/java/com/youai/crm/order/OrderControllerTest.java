package com.youai.crm.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsFiltersAndUpdatesOrderPayment() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"610001","customer":"接口客户","product":"专业版",
                                 "amount":10000,"paid":2000,"owner":"林夕"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("610001"))
                .andExpect(jsonPath("$.status").value("部分支付"));

        mockMvc.perform(get("/api/orders").param("keyword", "610001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("610001"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(patch("/api/orders/610001/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paid\":10000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("已支付"));
    }

    @Autowired
    private SalesOrderRepository orderRepository;

    @Test
    void filtersByInclusiveSubmissionDates() throws Exception {
        SalesOrder order = new SalesOrder();
        order.setOrderNo("610006");
        order.setCustomerName("日期筛选客户");
        order.setProduct("VIP");
        order.setAmount(java.math.BigDecimal.TEN);
        order.setPaidAmount(java.math.BigDecimal.ZERO);
        order.setPaymentStatus("待支付");
        order.setServiceStatus("待开通");
        order.setOwner("admin");
        order.setCreatedAt(java.time.LocalDateTime.of(2026, 11, 6, 23, 59, 59));
        orderRepository.saveAndFlush(order);
        mockMvc.perform(get("/api/orders").param("keyword", "610006")
                        .param("createdFrom", "2026-11-06").param("createdTo", "2026-11-06"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/orders").param("keyword", "610006").param("createdTo", "2026-11-05"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/orders").param("keyword", "610006").param("createdFrom", "2026-11-07"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/orders").param("createdFrom", "2026-11-07").param("createdTo", "2026-11-06"))
                .andExpect(status().isBadRequest());
    }

    @Autowired
    private com.youai.crm.customer.CustomerRepository customers;

    @Test
    void linksOrderByCustomerIdAndFiltersByIt() throws Exception {
        var customer = customers.findAllByName("周雨桐").getFirst();
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"orderNo":"610002","customer":"旧姓名","customerId":"%s",
                             "product":"VIP","amount":100,"paid":0,"owner":"林夕"}
                            """.formatted(customer.getCustomerNo())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customer.getCustomerNo()))
                .andExpect(jsonPath("$.customer").value(customer.getName()));
        mockMvc.perform(get("/api/orders").param("customerId", customer.getCustomerNo()).param("keyword", "610002"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].customerId").value(customer.getCustomerNo()));
        mockMvc.perform(get("/api/orders").param("customerId", "not-a-customer"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void rejectsUnknownCustomerId() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"orderNo":"610003","customer":"客户","customerId":"not-a-customer",
                             "product":"VIP","amount":100,"paid":0,"owner":"林夕"}
                            """))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "linxi", roles = "SALES")
    void salesCannotLinkAnotherOwnersCustomer() throws Exception {
        var customer = customers.findAllByName("王泽").getFirst();
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"orderNo":"610004","customer":"王泽","customerId":"%s",
                             "product":"VIP","amount":100,"paid":0,"owner":"林夕"}
                            """.formatted(customer.getCustomerNo())))
                .andExpect(status().isForbidden());
    }

    @Test
    void generatesUniqueSixDigitNumbers() throws Exception {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var ids = new java.util.HashSet<String>();
        for (int index = 0; index < 12; index++) {
            var result = mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"customer":"随机编号客户","product":"VIP","amount":100,"paid":0,"owner":"林夕"}
                        """))
                    .andExpect(status().isCreated()).andReturn();
            String id = mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
            org.junit.jupiter.api.Assertions.assertTrue(id.matches("[1-9][0-9]{5}"));
            org.junit.jupiter.api.Assertions.assertTrue(ids.add(id));
            mockMvc.perform(get("/api/orders/" + id)).andExpect(status().isOk());
        }
    }

    @Test
    void rejectsInvalidAndDuplicateOrderNumbers() throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"orderNo":"SO-LONG-NUMBER","customer":"客户","product":"VIP","amount":100,"owner":"林夕"}
                    """))
                .andExpect(status().isBadRequest());
        String payload = """
                    {"orderNo":"654321","customer":"客户","product":"VIP","amount":100,"owner":"林夕"}
                    """;
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsPaidAmountAboveOrderAmount() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"610005","customer":"接口客户","product":"专业版",
                                 "amount":10000,"paid":10001,"owner":"林夕"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private void createSharedOrder(String id, int paid) throws Exception {
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"orderNo":"%s","customer":"双人确认客户","product":"VIP","amount":100,"paid":%d,"owner":"林夕"}
                    """.formatted(id, paid)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.confirmationSalesName").value("林夕"))
                .andExpect(jsonPath("$.confirmationAdminName").value("赵娣"));
    }

    private org.springframework.test.web.servlet.ResultActions confirmAs(String id, String username, String role) throws Exception {
        return mockMvc.perform(patch("/api/orders/" + id + "/confirmation")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(username).roles(role))
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirmed\":true}"));
    }

    @Test
    void confirmsOrderPerformanceOnlyAfterBothPeopleConfirm() throws Exception {
        createSharedOrder("620001", 100);
        var first = confirmAs("620001", "admin", "ADMIN").andExpect(status().isOk())
                .andExpect(jsonPath("$.performanceConfirmed").value(false))
                .andExpect(jsonPath("$.adminConfirmedAt").isNotEmpty())
                .andExpect(jsonPath("$.salesConfirmedAt").isEmpty())
                .andExpect(jsonPath("$.canConfirmPerformance").value(false)).andReturn();
        String timestamp = new com.fasterxml.jackson.databind.ObjectMapper().readTree(first.getResponse().getContentAsString()).get("adminConfirmedAt").asText();
        confirmAs("620001", "admin", "ADMIN").andExpect(status().isOk())
                .andExpect(jsonPath("$.adminConfirmedAt").value(timestamp)).andExpect(jsonPath("$.performanceConfirmed").value(false));
        confirmAs("620001", "chenchen", "SALES").andExpect(status().isForbidden());
        confirmAs("620001", "linxi", "SALES").andExpect(status().isOk())
                .andExpect(jsonPath("$.performanceConfirmed").value(true)).andExpect(jsonPath("$.salesConfirmedAt").isNotEmpty());
    }

    @Test
    void salesCanConfirmFirstAndFinancialChangesResetBothConfirmations() throws Exception {
        createSharedOrder("620002", 100);
        confirmAs("620002", "linxi", "SALES").andExpect(status().isOk()).andExpect(jsonPath("$.performanceConfirmed").value(false));
        confirmAs("620002", "admin", "ADMIN").andExpect(status().isOk()).andExpect(jsonPath("$.performanceConfirmed").value(true));
        mockMvc.perform(patch("/api/orders/620002/payment").contentType(MediaType.APPLICATION_JSON).content("{\"paid\":50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.performanceConfirmed").value(false))
                .andExpect(jsonPath("$.salesConfirmedAt").isEmpty()).andExpect(jsonPath("$.adminConfirmedAt").isEmpty());
    }

    @Test
    void usesTheOrdersSalespersonWhenCustomerOwnershipDiffers() throws Exception {
        var customer = customers.findAllByName("周雨桐").getFirst();
        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"orderNo":"620006","customer":"周雨桐","customerId":"%s","product":"VIP","amount":100,"paid":100,"owner":"陈晨"}
                    """.formatted(customer.getCustomerNo())))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.owner").value("陈晨"))
                .andExpect(jsonPath("$.confirmationSalesName").value("陈晨"));
        mockMvc.perform(get("/api/orders").param("keyword", "620006")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("chenchen").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].canConfirmPerformance").value(true));
        confirmAs("620006", "linxi", "SALES").andExpect(status().isForbidden());
        confirmAs("620006", "chenchen", "SALES").andExpect(status().isOk()).andExpect(jsonPath("$.performanceConfirmed").value(false));
        confirmAs("620006", "admin", "ADMIN").andExpect(status().isOk()).andExpect(jsonPath("$.performanceConfirmed").value(true));
    }

    @Test
    void partialPaymentCannotBeConfirmed() throws Exception {
        createSharedOrder("620007", 50);
        confirmAs("620007", "admin", "ADMIN").andExpect(status().isBadRequest());
        confirmAs("620007", "linxi", "SALES").andExpect(status().isBadRequest());
    }

    @Test
    void unpaidOrderCannotBeConfirmed() throws Exception {
        createSharedOrder("620003", 0);
        confirmAs("620003", "admin", "ADMIN").andExpect(status().isBadRequest());
        confirmAs("620003", "linxi", "SALES").andExpect(status().isBadRequest());
    }

    @Autowired
    private com.youai.crm.account.CrmUserRepository users;

    @Test
    void anotherAdministratorCannotConfirmForZhaoDi() throws Exception {
        var administrator = users.findByUsernameIgnoreCase("admin").orElseThrow();
        var other = new com.youai.crm.account.CrmUser();
        other.setUsername("other-confirmation-admin"); other.setDisplayName("其他管理员");
        other.setPasswordHash(administrator.getPasswordHash()); other.setDepartment(administrator.getDepartment());
        other.setRoles(new java.util.HashSet<>(administrator.getRoles())); users.saveAndFlush(other);
        createSharedOrder("620004", 100);
        confirmAs("620004", other.getUsername(), "ADMIN").andExpect(status().isForbidden());
    }

    @Test
    void concurrentConfirmationsDoNotOverwriteEachOther() throws Exception {
        createSharedOrder("620005", 100);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            var sales = executor.submit(() -> { start.await(); confirmAs("620005", "linxi", "SALES").andExpect(status().isOk()); return true; });
            var admin = executor.submit(() -> { start.await(); confirmAs("620005", "admin", "ADMIN").andExpect(status().isOk()); return true; });
            start.countDown();
            sales.get(15, java.util.concurrent.TimeUnit.SECONDS); admin.get(15, java.util.concurrent.TimeUnit.SECONDS);
            mockMvc.perform(get("/api/orders/620005")).andExpect(status().isOk()).andExpect(jsonPath("$.performanceConfirmed").value(true))
                    .andExpect(jsonPath("$.salesConfirmedAt").isNotEmpty()).andExpect(jsonPath("$.adminConfirmedAt").isNotEmpty());
        } finally { executor.shutdownNow(); }
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "linxi", roles = "SALES")
    void salesCannotReadAnotherOwnersOrder() throws Exception {
        mockMvc.perform(get("/api/orders/SO20260816023"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "linxi", roles = "SALES")
    void salesCannotUseFinanceEndpoints() throws Exception {
        mockMvc.perform(patch("/api/orders/SO20260817008/payment")
                .contentType(MediaType.APPLICATION_JSON).content("{\"paid\":100}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/orders/SO20260817008/confirmation")
                .contentType(MediaType.APPLICATION_JSON).content("{\"confirmed\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/ledger-accounts")).andExpect(status().isForbidden());
    }
}
