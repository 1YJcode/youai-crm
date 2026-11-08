package com.youai.crm.communication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TelephonyGatewayClient {
    private final boolean enabled;
    private final String endpoint;
    private final String token;
    private final String callerId;
    private final ObjectMapper json;
    private final GatewayHttpTransport transport;

    public TelephonyGatewayClient(@Value("${telephony.gateway.enabled:false}") boolean enabled,
            @Value("${telephony.gateway.endpoint:}") String endpoint,
            @Value("${telephony.gateway.token:}") String token,
            @Value("${telephony.gateway.caller-id:}") String callerId, ObjectMapper json) {
        this.enabled = enabled;
        this.endpoint = endpoint;
        this.token = token;
        this.callerId = callerId;
        this.json = json;
        this.transport = new GatewayHttpTransport(json);
        if (enabled) requireConfigured();
    }

    public boolean enabled() { return enabled; }

    public void requireConfigured() {
        if (!enabled || endpoint.isBlank() || token.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "电话网关尚未配置，请先接入支持 WebRTC 的电话通道");
        URI uri = URI.create(endpoint);
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme())) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("电话网关地址无效");
    }

    public JsonNode browserConfig(Long userId) throws Exception {
        requireConfigured();
        JsonNode result = transport.exchange(request(endpoint + "?action=browser-config&agentUserId=" + userId).GET().build());
        if (!"WEBRTC".equals(result.path("mode").asText()) || !result.path("iceServers").isArray()
                || result.path("iceServers").size() > 10)
            throw new java.io.IOException("电话网关缺少 WebRTC 配置");
        for (JsonNode server : result.path("iceServers")) {
            JsonNode urls = server.path("urls");
            if (urls.isTextual()) validateIceUrl(urls.asText());
            else if (urls.isArray() && !urls.isEmpty() && urls.size() <= 10) {
                for (JsonNode url : urls) validateIceUrl(url.asText());
            } else throw new java.io.IOException("电话网关 ICE 地址无效");
        }
        // Only these fields are allowed to reach the browser. The gateway bearer token stays server-side.
        var safe = json.createObjectNode();
        safe.put("configured", true);
        safe.put("mode", "WEBRTC");
        var servers = safe.putArray("iceServers");
        for (JsonNode server : result.path("iceServers")) {
            var target = servers.addObject();
            target.set("urls", server.path("urls"));
            if (server.has("username")) target.put("username", server.path("username").asText());
            if (server.has("credential")) target.put("credential", server.path("credential").asText());
        }
        return safe;
    }

    private void validateIceUrl(String url) throws java.io.IOException {
        if (url.length() > 2048 || !(url.startsWith("stun:") || url.startsWith("turn:") || url.startsWith("turns:")))
            throw new java.io.IOException("电话网关 ICE 地址无效");
    }

    public Receipt submit(CallRecord call, String phone) throws Exception {
        return command(call, "呼入".equals(call.getDirection()) ? "ANSWER" : "DIAL", phone);
    }

    public Receipt hangup(CallRecord call) throws Exception { return command(call, "HANGUP", ""); }

    private Receipt command(CallRecord call, String action, String phone) throws Exception {
        String key = ("HANGUP".equals(action) ? "hangup-" : "call-") + call.getId();
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("action", action);
        body.put("requestId", "call-" + call.getId());
        body.put("callId", call.getGatewayCallId());
        body.put("agentUserId", call.getAgentUserId());
        body.put("phone", phone);
        body.put("callerId", callerId);
        if (!"HANGUP".equals(action)) body.put("offerSdp", call.getBrowserOffer());
        return receipt(transport.exchange(request(endpoint).header("Idempotency-Key", key)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build()));
    }

    public Receipt query(CallRecord call) throws Exception {
        String url = endpoint + "?requestId=call-" + call.getId();
        if (call.getGatewayCallId() != null) url += "&callId=" + java.net.URLEncoder.encode(call.getGatewayCallId(), java.nio.charset.StandardCharsets.UTF_8);
        return receipt(transport.exchange(request(url).GET().build()));
    }

    private HttpRequest.Builder request(String url) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token).header("Accept", "application/json");
    }

    private Receipt receipt(JsonNode result) throws java.io.IOException {
        String state = result.path("state").asText();
        String callId = result.path("callId").asText();
        long sequence = result.path("sequence").asLong(-1);
        String answer = result.path("answerSdp").asText(null);
        int duration = result.path("durationSeconds").asInt(0);
        if (!Set.of("ACCEPTED", "RINGING", "CONNECTED", "ENDED", "FAILED", "UNKNOWN").contains(state)
                || sequence < 0 || callId.isBlank() || callId.length() > 128 || duration < 0
                || (answer != null && (answer.length() > 32000 || !answer.startsWith("v=0"))))
            throw new java.io.IOException("电话网关响应无效");
        return new Receipt(state, callId, sequence, duration, answer);
    }

    public record Receipt(String state, String callId, long sequence, int durationSeconds, String answerSdp) { }
}
