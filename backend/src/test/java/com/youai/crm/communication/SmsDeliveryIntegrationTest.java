package com.youai.crm.communication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = "sms.gateway.initial-ms=3600000")
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class SmsDeliveryIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ConversationRepository conversations;
    @Autowired MessageRepository messages;
    @Autowired SmsDeliveryWorker worker;
    @MockitoBean SmsGatewayClient gateway;
    Long conversationId;

    @BeforeEach
    void setup() {
        conversationId = conversations.findAll().getFirst().getId();
        when(gateway.normalizePhone(anyString())).thenReturn("+8613800138000");
    }

    private String request(String key, String content) throws Exception {
        return json.writeValueAsString(java.util.Map.of("content", content, "channel", "SMS", "requestId", key));
    }

    private Long queue(String key) throws Exception {
        String body = mvc.perform(post("/api/conversations/{id}/messages", conversationId)
                .contentType(MediaType.APPLICATION_JSON).content(request(key, "短信测试")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.deliveryStatus").value("QUEUED"))
                .andExpect(jsonPath("$.recipientPhone").doesNotExist()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("id").asLong();
    }

    @Test
    void rejectsUnconfiguredGatewayWithoutCreatingMessage() throws Exception {
        long before = messages.count();
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "短信网关尚未配置")).when(gateway).requireConfigured();
        mvc.perform(post("/api/conversations/{id}/messages", conversationId).contentType(MediaType.APPLICATION_JSON)
                .content(request(UUID.randomUUID().toString(), "test"))).andExpect(status().isServiceUnavailable());
        assertEquals(before, messages.count());
    }

    @Test
    void idempotentRequestsAndConflictingContent() throws Exception {
        String key = UUID.randomUUID().toString();
        Long id = queue(key);
        assertEquals(id, queue(key));
        mvc.perform(post("/api/conversations/{id}/messages", conversationId).contentType(MediaType.APPLICATION_JSON)
                .content(request(key, "different"))).andExpect(status().isConflict());
        verify(gateway, never()).send(any());
    }

    @Test
    void concurrentRequestsCreateOneMessage() throws Exception {
        String payload = request(UUID.randomUUID().toString(), "concurrent");
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Long> call = () -> {
                String body = mvc.perform(post("/api/conversations/{id}/messages", conversationId)
                        .with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(payload))
                        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
                return json.readTree(body).path("id").asLong();
            };
            var first = executor.submit(call);
            var second = executor.submit(call);
            assertEquals(first.get(), second.get());
        }
    }

    @Test
    void persistsAcceptedThenDeliveredAndDoesNotResend() throws Exception {
        Long id = queue(UUID.randomUUID().toString());
        when(gateway.enabled()).thenReturn(true);
        when(gateway.send(any())).thenReturn(new SmsGatewayClient.Receipt("ACCEPTED", "provider-1", null));
        worker.dispatch();
        assertEquals("ACCEPTED", messages.findById(id).orElseThrow().getDeliveryStatus());
        CrmMessage message = messages.findById(id).orElseThrow();
        message.setDeliveryUpdatedAt(LocalDateTime.now().minusMinutes(1));
        messages.saveAndFlush(message);
        when(gateway.query(any())).thenReturn(new SmsGatewayClient.Receipt("DELIVERED", "provider-1", null));
        worker.dispatch();
        assertEquals("DELIVERED", messages.findById(id).orElseThrow().getDeliveryStatus());
        worker.dispatch();
        verify(gateway, atLeastOnce()).send(any());
        verify(gateway, times(1)).send(argThat(item -> id.equals(item.getId())));
    }

    @Test
    void uncertainDeliveryAndRestartRecoveryNeverAutomaticallyResend() throws Exception {
        Long id = queue(UUID.randomUUID().toString());
        when(gateway.enabled()).thenReturn(true);
        when(gateway.send(any())).thenThrow(new java.net.http.HttpTimeoutException("timeout"));
        worker.dispatch();
        assertEquals("UNKNOWN", messages.findById(id).orElseThrow().getDeliveryStatus());
        worker.dispatch();
        verify(gateway, times(1)).send(argThat(item -> id.equals(item.getId())));
        CrmMessage message = messages.findById(id).orElseThrow();
        message.setDeliveryStatus("SENDING");
        message.setDeliveryUpdatedAt(LocalDateTime.now().minusMinutes(3));
        messages.saveAndFlush(message);
        worker.dispatch();
        assertEquals("UNKNOWN", messages.findById(id).orElseThrow().getDeliveryStatus());
        verify(gateway, times(1)).send(argThat(item -> id.equals(item.getId())));
    }

    @Test
    @WithMockUser(username = "linxi", roles = "SALES")
    void rejectsAnotherOwnersMessages() throws Exception {
        Conversation other = conversations.findAll().stream().filter(item -> !"林夕".equals(item.getOwner())).findFirst().orElseThrow();
        mvc.perform(post("/api/conversations/{id}/messages", other.getId()).contentType(MediaType.APPLICATION_JSON)
                .content(request(UUID.randomUUID().toString(), "test"))).andExpect(status().isForbidden());
    }
}
