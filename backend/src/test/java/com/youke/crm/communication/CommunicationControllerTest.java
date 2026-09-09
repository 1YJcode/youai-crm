package com.youke.crm.communication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.youke.crm.customer.Customer;
import com.youke.crm.customer.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class CommunicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void listsConversationSendsMessageAndMarksRead() throws Exception {
        Conversation conversation = conversationRepository.findAll().getFirst();

        mockMvc.perform(get("/api/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").isNotEmpty());

        mockMvc.perform(post("/api/conversations/{id}/messages", conversation.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"新的报价方案已发送\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("新的报价方案已发送"))
                .andExpect(jsonPath("$.direction").value("OUTBOUND"));

        mockMvc.perform(patch("/api/conversations/read"))
                .andExpect(status().isNoContent());
    }

    @Test
    void createsAndFiltersCallRecord() throws Exception {
        Customer customer = customerRepository.findAll().getFirst();

        mockMvc.perform(post("/api/calls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerId":"%s","direction":"呼出","status":"待回拨",
                                 "durationSeconds":30,"note":"客户要求下午回拨"}
                                """.formatted(customer.getCustomerNo())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customer.getCustomerNo()))
                .andExpect(jsonPath("$.status").value("待回拨"));

        mockMvc.perform(get("/api/calls").param("status", "待回拨"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("待回拨"));
    }

    @Test
    @WithMockUser(username = "linxi", roles = "SALES")
    void salesCannotReadAnotherOwnersConversation() throws Exception {
        Conversation conversation = conversationRepository.findAll().stream()
                .filter(item -> !"林夕".equals(item.getOwner()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/conversations/{id}/messages", conversation.getId()))
                .andExpect(status().isForbidden());
    }
}
