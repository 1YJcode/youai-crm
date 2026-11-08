package com.youai.crm.communication;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SmsGatewayClientTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void validatesConfigurationAndNormalizesPhone() {
        var disabled = new SmsGatewayClient(false, "", "", "", json);
        assertThrows(ResponseStatusException.class, disabled::requireConfigured);
        assertEquals("+8613800138000", disabled.normalizePhone("138 0013-8000"));
        assertEquals("+14155552671", disabled.normalizePhone("+1 (415) 555-2671"));
        assertThrows(IllegalArgumentException.class, () -> disabled.normalizePhone("138****8000"));
        assertThrows(IllegalArgumentException.class, () -> disabled.normalizePhone(null));
        assertThrows(IllegalArgumentException.class, () -> new SmsGatewayClient(true, "file:///tmp/sms", "token", "", json));
    }

    @Test
    void sendsAuthenticatedIdempotentRequestAndQueriesReceipt() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> key = new AtomicReference<>();
        AtomicReference<String> auth = new AtomicReference<>();
        AtomicReference<String> query = new AtomicReference<>();
        server.createContext("/sms", exchange -> {
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            key.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            query.set(exchange.getRequestURI().getQuery());
            String status = "GET".equals(exchange.getRequestMethod()) ? "DELIVERED" : "ACCEPTED";
            byte[] response = ("{\"status\":\"" + status + "\",\"messageId\":\"provider-1\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var gateway = new SmsGatewayClient(true, "http://127.0.0.1:" + server.getAddress().getPort() + "/sms", "secret", "sender-number", json);
            CrmMessage message = new CrmMessage();
            var id = CrmMessage.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(message, 123L);
            message.setRecipientPhone("+8613800138000");
            message.setContent("测试短信");
            assertEquals("ACCEPTED", gateway.send(message).status());
            assertEquals("Bearer secret", auth.get());
            assertEquals("sms-123", key.get());
            assertEquals("+8613800138000", json.readTree(body.get()).path("phone").asText());
            assertEquals("测试短信", json.readTree(body.get()).path("content").asText());
            assertEquals("sender-number", json.readTree(body.get()).path("sender").asText());
            assertEquals("DELIVERED", gateway.query(message).status());
            assertEquals("requestId=sms-123", query.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void doesNotTreatHttpSuccessWithoutReceiptAsDelivery() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/sms", exchange -> {
            byte[] response = "{\"status\":\"OK\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var gateway = new SmsGatewayClient(true, "http://127.0.0.1:" + server.getAddress().getPort() + "/sms", "secret", "", json);
            assertThrows(java.io.IOException.class, () -> gateway.query(new CrmMessage()));
        } finally {
            server.stop(0);
        }
    }
}
