package com.youke.crm.customer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class CustomerFollowUpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aggregatesCustomerFollowUpsAndPersistsPersonalAnnotation() throws Exception {
        mockMvc.perform(get("/api/customers/1430038038/follow-ups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value("1430038038"));

        mockMvc.perform(patch("/api/customers/1430038038/follow-ups/annotations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recordId":"task:1","recordType":"task","favorite":true,"comment":"已确认下一步安排"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favorite").value(true))
                .andExpect(jsonPath("$.comment").value("已确认下一步安排"));

        mockMvc.perform(get("/api/customers/1430038038/follow-ups/annotations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recordId").value("task:1"));
    }
}
