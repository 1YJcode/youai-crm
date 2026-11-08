package com.youai.crm.communication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.customer.Customer;
import com.youai.crm.customer.CustomerRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(properties = {"telephony.gateway.initial-ms=3600000", "sms.gateway.initial-ms=3600000", "communication.webhook-token=test-webhook-secret"})
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class TelephonyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CallRecordRepository calls;
    @Autowired CustomerRepository customers;
    @Autowired CrmUserRepository users;
    @Autowired ConversationRepository conversations;
    @Autowired MessageRepository messages;
    @Autowired TelephonyWorker worker;
    @MockitoBean TelephonyGatewayClient gateway;
    @MockitoBean SmsGatewayClient sms;
    Customer customer;
    Long agentId;

    @BeforeEach
    void setup() {
        for (var call : calls.findAll()) {
            if (!"LOCAL".equals(call.getGatewayState())) {
                call.setGatewayState("ENDED");
                call.setHangupRequested(false);
                calls.save(call);
            }
        }
        customer = customers.findAll().getFirst();
        agentId = users.findByUsernameIgnoreCase("admin").orElseThrow().getId();
        customer.setOwnerId(agentId);
        customer.setPhone("13800138000");
        customers.saveAndFlush(customer);
        var normalizer = new SmsGatewayClient(false, "", "", "", json);
        when(sms.normalizePhone(any())).thenAnswer(invocation -> normalizer.normalizePhone(invocation.getArgument(0)));
    }

    private String payload(String key) throws Exception {
        return json.writeValueAsString(Map.of("customerId", customer.getCustomerNo(), "requestId", key, "offerSdp", "v=0\r\no=test"));
    }

    private Long dial(String key) throws Exception {
        String body = mvc.perform(post("/api/telephony/calls").contentType(MediaType.APPLICATION_JSON).content(payload(key)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.call.gatewayState").value("QUEUED"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).path("call").path("id").asLong();
    }

    private String event(Long id, String callId, String state, long seq, String direction, Long agent) throws Exception {
        var body = new java.util.LinkedHashMap<String,Object>();
        if (id != null) body.put("requestId", "call-" + id);
        body.put("callId", callId);
        body.put("direction", direction);
        body.put("state", state);
        body.put("sequence", seq);
        body.put("from", "13800138000");
        body.put("agentUserId", agent);
        body.put("durationSeconds", "ENDED".equals(state) ? 45 : 0);
        return json.writeValueAsString(body);
    }

    private void callback(String body) throws Exception {
        mvc.perform(post("/api/communications/events/calls").header("X-Communication-Token", "test-webhook-secret")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
    }

    @Test
    void disabledGatewayRejectsDialAndInvalidSdpIsRejected() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "电话网关尚未配置")).when(gateway).requireConfigured();
        mvc.perform(get("/api/telephony/browser-config")).andExpect(status().isOk()).andExpect(jsonPath("$.configured").value(false));
        long before = calls.count();
        mvc.perform(post("/api/telephony/calls").contentType(MediaType.APPLICATION_JSON).content(payload(UUID.randomUUID().toString())))
                .andExpect(status().isServiceUnavailable());
        assertEquals(before, calls.count());
        mvc.perform(post("/api/telephony/calls").contentType(MediaType.APPLICATION_JSON).content("{\"customerId\":\"x\",\"requestId\":\"valid-request-123456\",\"offerSdp\":\"invalid\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void repeatedAndConcurrentDialCreatesOneCallAndNewCallWhileBusyIsRejected() throws Exception {
        String key = UUID.randomUUID().toString();
        LocalDateTime contact = customers.findById(customer.getId()).orElseThrow().getLastContactAt();
        String payload = payload(key);
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Long> create = () -> {
                String response = mvc.perform(post("/api/telephony/calls").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(payload)).andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString();
                return json.readTree(response).path("call").path("id").asLong();
            };
            var a = executor.submit(create);
            var b = executor.submit(create);
            assertEquals(a.get(), b.get());
        }
        assertEquals(contact, customers.findById(customer.getId()).orElseThrow().getLastContactAt());
        mvc.perform(post("/api/telephony/calls").contentType(MediaType.APPLICATION_JSON).content(payload(UUID.randomUUID().toString())))
                .andExpect(status().isConflict());
    }

    @Test
    void queuedCallCanBeCancelledWithoutCallingGateway() throws Exception {
        Long id = dial(UUID.randomUUID().toString());
        mvc.perform(post("/api/telephony/calls/{id}/hangup", id)).andExpect(status().isOk()).andExpect(jsonPath("$.call.gatewayState").value("ENDED"));
        when(gateway.enabled()).thenReturn(true);
        worker.dispatch();
        verify(gateway, never()).submit(any(), anyString());
    }

    @Test
    void dispatchNegotiatesAudioAndCallbacksStayOrderedAndAuthenticated() throws Exception {
        Long id = dial(UUID.randomUUID().toString());
        when(gateway.enabled()).thenReturn(true);
        when(gateway.submit(any(), anyString())).thenReturn(new TelephonyGatewayClient.Receipt("ACCEPTED", "provider-" + id, 1, 0, "v=0\r\nanswer"));
        worker.dispatch();
        mvc.perform(get("/api/telephony/calls/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.answerSdp").value("v=0\r\nanswer"));
        String connected = event(id, "provider-" + id, "CONNECTED", 3, "OUTBOUND", agentId);
        mvc.perform(post("/api/communications/events/calls").header("X-Communication-Token", "wrong")
                .contentType(MediaType.APPLICATION_JSON).content(connected)).andExpect(status().isUnauthorized());
        callback(connected);
        callback(event(id, "provider-" + id, "RINGING", 2, "OUTBOUND", agentId));
        assertEquals("CONNECTED", calls.findById(id).orElseThrow().getGatewayState());
        callback(event(id, "provider-" + id, "ENDED", 4, "OUTBOUND", agentId));
        callback(connected);
        var ended = calls.findById(id).orElseThrow();
        assertEquals("ENDED", ended.getGatewayState());
        assertEquals(45, ended.getDurationSeconds());
        assertNull(ended.getBrowserOffer());
        assertNull(ended.getBrowserAnswer());
    }

    @Test
    void uncertainRequestsAndRestartAreNotAutomaticallyRedialed() throws Exception {
        Long id = dial(UUID.randomUUID().toString());
        when(gateway.enabled()).thenReturn(true);
        when(gateway.submit(any(), anyString())).thenThrow(new java.net.http.HttpTimeoutException("timeout"));
        worker.dispatch();
        worker.dispatch();
        verify(gateway, times(1)).submit(argThat(call -> id.equals(call.getId())), anyString());
        CallRecord call = calls.findById(id).orElseThrow();
        assertEquals("UNKNOWN", call.getGatewayState());
        call.setGatewayState("SUBMITTING");
        call.setGatewayUpdatedAt(LocalDateTime.now().minusMinutes(3));
        calls.saveAndFlush(call);
        worker.dispatch();
        assertEquals("UNKNOWN", calls.findById(id).orElseThrow().getGatewayState());
        verify(gateway, times(1)).submit(argThat(item -> id.equals(item.getId())), anyString());
    }

    @Test
    void incomingCallAssociatesCustomerAndCanBeAnsweredAndHungUp() throws Exception {
        when(gateway.enabled()).thenReturn(true);
        String externalId = "incoming-" + UUID.randomUUID();
        callback(event(null, externalId, "RINGING", 1, "INBOUND", agentId));
        callback(event(null, externalId, "RINGING", 1, "INBOUND", agentId));
        var inbound = calls.findAll().stream().filter(call -> externalId.equals(call.getGatewayCallId())).toList();
        assertEquals(1, inbound.size());
        Long id = inbound.getFirst().getId();
        assertEquals(customer.getCustomerNo(), inbound.getFirst().getCustomerNo());
        mvc.perform(get("/api/telephony/incoming")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id));
        String offer = "{\"offerSdp\":\"v=0 answer-offer\"}";
        mvc.perform(post("/api/telephony/calls/{id}/answer", id).contentType(MediaType.APPLICATION_JSON).content(offer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.call.gatewayState").value("QUEUED"));
        mvc.perform(post("/api/telephony/calls/{id}/answer", id).contentType(MediaType.APPLICATION_JSON).content(offer)).andExpect(status().isOk());
        mvc.perform(post("/api/telephony/calls/{id}/hangup", id)).andExpect(status().isOk()).andExpect(jsonPath("$.hangupRequested").value(true));
        when(gateway.enabled()).thenReturn(true);
        when(gateway.hangup(any())).thenReturn(new TelephonyGatewayClient.Receipt("ENDED", externalId, 2, 0, null));
        worker.dispatch();
        assertEquals("ENDED", calls.findById(id).orElseThrow().getGatewayState());
        verify(gateway, never()).submit(any(), anyString());
    }

    @Test
    void disconnectedBrowserExpiresQueuedCallWithoutDialing() throws Exception {
        Long id = dial(UUID.randomUUID().toString());
        var call = calls.findById(id).orElseThrow();
        call.setBrowserLastSeenAt(LocalDateTime.now().minusMinutes(2));
        calls.saveAndFlush(call);
        when(gateway.enabled()).thenReturn(true);
        worker.dispatch();
        assertEquals("ENDED", calls.findById(id).orElseThrow().getGatewayState());
        verify(gateway, never()).submit(any(), anyString());
    }

    @Test
    void customerAndMediaPermissionsAreEnforced() throws Exception {
        when(gateway.enabled()).thenReturn(true);
        Long id = dial(UUID.randomUUID().toString());
        mvc.perform(get("/api/telephony/calls/{id}", id).with(user("linxi").roles("SALES"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/telephony/calls/{id}/hangup", id).with(user("linxi").roles("SALES"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/telephony/calls").with(user("linxi").roles("SALES")).contentType(MediaType.APPLICATION_JSON)
                .content(payload(UUID.randomUUID().toString()))).andExpect(status().isForbidden());
        Long other = users.findByUsernameIgnoreCase("linxi").orElseThrow().getId();
        String external = "foreign-" + UUID.randomUUID();
        callback(event(null, external, "RINGING", 1, "INBOUND", other));
        mvc.perform(get("/api/telephony/incoming").with(user("linxi").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].customerId").value(""))
                .andExpect(jsonPath("$[0].customer").value("未匹配客户"));
    }

    @Test
    void headerLookupResolvesCustomerWithinCurrentPermissions() throws Exception {
        mvc.perform(get("/api/telephony/customer").param("phone", "13800138000"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(customer.getCustomerNo()));
        mvc.perform(get("/api/telephony/customer").param("customerId", customer.getCustomerNo()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value(customer.getName()));
        mvc.perform(get("/api/telephony/customer").param("phone", "13800138000").with(user("linxi").roles("SALES")))
                .andExpect(status().isForbidden());
    }

    @Test
    void incomingSmsIsAuthenticatedIdempotentAndVisibleInConversation() throws Exception {
        String messageId = UUID.randomUUID().toString();
        String body = json.writeValueAsString(Map.of("messageId", messageId, "from", "13800138000", "content", "客户回复"));
        mvc.perform(post("/api/communications/events/sms").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        long before = messages.count();
        for (int i = 0; i < 2; i++) mvc.perform(post("/api/communications/events/sms").header("X-Communication-Token", "test-webhook-secret")
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent());
        assertEquals(before + 1, messages.count());
        assertTrue(messages.findAll().stream().anyMatch(message -> messageId.equals(message.getGatewayMessageId())
                && "INBOUND".equals(message.getDirection()) && !message.isRead()));
        String unknown = json.writeValueAsString(Map.of("messageId", "unknown", "from", "+14155552671", "content", "hi"));
        mvc.perform(post("/api/communications/events/sms").header("X-Communication-Token", "test-webhook-secret")
                .contentType(MediaType.APPLICATION_JSON).content(unknown)).andExpect(status().isConflict());
    }
}
