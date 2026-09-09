package com.youke.crm.order;

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
                                {"orderNo":"SO-TEST-001","customer":"接口客户","product":"专业版",
                                 "amount":10000,"paid":2000,"owner":"林夕"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("SO-TEST-001"))
                .andExpect(jsonPath("$.status").value("部分支付"));

        mockMvc.perform(get("/api/orders").param("keyword", "SO-TEST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("SO-TEST-001"));

        mockMvc.perform(patch("/api/orders/SO-TEST-001/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paid\":10000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("已支付"));
    }

    @Test
    void rejectsPaidAmountAboveOrderAmount() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderNo":"SO-INVALID-001","customer":"接口客户","product":"专业版",
                                 "amount":10000,"paid":10001,"owner":"林夕"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void confirmsOrderPerformance() throws Exception {
        mockMvc.perform(patch("/api/orders/SO20260817008/confirmation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.performanceConfirmed").value(true));
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "linxi", roles = "SALES")
    void salesCannotReadAnotherOwnersOrder() throws Exception {
        mockMvc.perform(get("/api/orders/SO20260816023"))
                .andExpect(status().isForbidden());
    }
}
