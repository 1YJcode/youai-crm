package com.youai.crm.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class BusinessSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void administratorCanSaveCustomerPoolRule() throws Exception {
        mockMvc.perform(put("/api/business-settings/customer-pool")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":false,\"days\":12}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.days").value(12));

        mockMvc.perform(get("/api/business-settings/customer-pool"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.days").value(12));
    }

    @Test
    @WithMockUser(username = "sales", roles = "SALES")
    void nonAdministratorCannotChangeCustomerPoolRule() throws Exception {
        mockMvc.perform(put("/api/business-settings/customer-pool")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"days\":7}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void customerPoolDaysMustBePositiveAndBounded() throws Exception {
        mockMvc.perform(put("/api/business-settings/customer-pool")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"days\":0}"))
                .andExpect(status().isBadRequest());
    }
}
