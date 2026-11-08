package com.youai.crm.communication;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TelephonyGatewayClientTest {
    @Test
    void usesServerSideCredentialsAndFiltersBrowserConfiguration() throws Exception {
        ObjectMapper json = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var body = new AtomicReference<String>();
        var key = new AtomicReference<String>();
        var auth = new AtomicReference<String>();
        server.createContext("/calls", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            key.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            String result = "GET".equals(exchange.getRequestMethod())
                    ? "{\"mode\":\"WEBRTC\",\"iceServers\":[{\"urls\":\"turn:localhost:3478\",\"username\":\"temporary\",\"credential\":\"temporary-password\"}],\"secret\":\"do-not-expose\"}"
                    : "{\"state\":\"ACCEPTED\",\"callId\":\"provider-1\",\"sequence\":1,\"answerSdp\":\"v=0 answer\"}";
            byte[] response = result.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            var gateway = new TelephonyGatewayClient(true, "http://127.0.0.1:" + server.getAddress().getPort() + "/calls", "private-token", "caller-id", json);
            var config = gateway.browserConfig(7L);
            assertEquals("WEBRTC", config.path("mode").asText());
            assertFalse(config.has("secret"));
            assertEquals("Bearer private-token", auth.get());
            var call = new CallRecord();
            var id = CallRecord.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(call, 99L);
            call.setAgentUserId(7L);
            call.setDirection("呼出");
            call.setBrowserOffer("v=0 offer");
            assertEquals("v=0 answer", gateway.submit(call, "+8613800138000").answerSdp());
            assertEquals("call-99", key.get());
            assertEquals("DIAL", json.readTree(body.get()).path("action").asText());
            assertEquals("+8613800138000", json.readTree(body.get()).path("phone").asText());
            assertEquals("caller-id", json.readTree(body.get()).path("callerId").asText());
            call.setDirection("呼入");
            gateway.submit(call, "+8613800138000");
            assertEquals("ANSWER", json.readTree(body.get()).path("action").asText());
            gateway.hangup(call);
            assertEquals("hangup-99", key.get());
            assertEquals("HANGUP", json.readTree(body.get()).path("action").asText());
        } finally { server.stop(0); }
    }

    @Test
    void rejectsInvalidAndOversizedGatewayResponses() throws Exception {
        ObjectMapper json = new ObjectMapper();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<String> result = new AtomicReference<>("{\"state\":\"OK\"}");
        server.createContext("/calls", exchange -> {
            byte[] response = result.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try { exchange.getResponseBody().write(response); } finally { exchange.close(); }
        });
        server.start();
        try {
            var gateway = new TelephonyGatewayClient(true, "http://127.0.0.1:" + server.getAddress().getPort() + "/calls", "token", "", json);
            assertThrows(java.io.IOException.class, () -> gateway.query(new CallRecord()));
            result.set("x".repeat(70000));
            assertThrows(java.io.IOException.class, () -> gateway.query(new CallRecord()));
        } finally { server.stop(0); }
    }
}
