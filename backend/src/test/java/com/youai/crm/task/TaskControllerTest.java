package com.youai.crm.task;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

import com.youai.crm.customer.CustomerRepository;
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
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void filtersTasksAndChangesCompletion() throws Exception {
        mockMvc.perform(get("/api/tasks").param("status", "today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("today"))
                .andExpect(jsonPath("$.totalElements").isNumber());

        mockMvc.perform(patch("/api/tasks/1/completion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completed\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.done").value(true))
                .andExpect(jsonPath("$.status").value("done"));
    }

    @Test
    void createsTaskAndRejectsInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"发送合同","customer":"接口客户","owner":"林夕",
                                 "dueAt":"2030-01-02T10:00:00","type":"合同","priority":"高"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("发送合同"))
                .andExpect(jsonPath("$.status").value("upcoming"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createsCustomerFollowUpWithDurableCustomerLinkAndStatus() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"已沟通预算和下一步安排","customer":"周雨桐","customerId":"1430038038",
                                 "customerStatus":"商务谈判","owner":"林夕",
                                 "dueAt":"2030-01-02T10:00:00","type":"电话跟进","priority":"高"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("1430038038"))
                .andExpect(jsonPath("$.customerStatus").value("商务谈判"))
                .andExpect(jsonPath("$.followedAt").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        var customer = customerRepository.findByCustomerNo("1430038038").orElseThrow();
        assertThat(customer.getStage()).isEqualTo("商务谈判");
        assertThat(customer.getLastContactAt()).isNotNull();
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "linxi", roles = "SALES")
    void salesCannotReadAnotherOwnersTask() throws Exception {
        mockMvc.perform(get("/api/tasks/2"))
                .andExpect(status().isForbidden());
    }

}
