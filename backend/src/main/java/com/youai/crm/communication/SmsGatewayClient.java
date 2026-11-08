package com.youai.crm.communication;

import java.io.IOException;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SmsGatewayClient {
    private final boolean enabled;
    private final String endpoint;
    private final String token;
    private final String sender;
    private final ObjectMapper json;
    private final GatewayHttpTransport transport;

    public SmsGatewayClient(@Value("${sms.gateway.enabled:false}") boolean enabled,
            @Value("${sms.gateway.endpoint:}") String endpoint,
            @Value("${sms.gateway.token:}") String token,
            @Value("${sms.gateway.sender:}") String sender, ObjectMapper json) {
        this.enabled = enabled;
        this.endpoint = endpoint;
        this.token = token;
        this.sender = sender;
        this.json = json;
        this.transport = new GatewayHttpTransport(json);
        if (enabled) requireConfigured();
    }

    public boolean enabled() { return enabled; }

    public void requireConfigured() {
        if (!enabled || endpoint.isBlank() || token.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "短信网关尚未配置，请先接入短信发送通道");
        URI uri = URI.create(endpoint);
        if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("短信网关地址应为不含查询参数的 HTTP/HTTPS 地址");
    }

    public String normalizePhone(String value) {
        String phone = value == null ? "" : value.replaceAll("[\\s()-]", "");
        if (phone.matches("1[3-9][0-9]{9}")) return "+86" + phone;
        if (phone.matches("\\+[1-9][0-9]{7,14}")) return phone;
        throw new IllegalArgumentException("客户手机号无效，请填写中国大陆手机号或带国家区号的国际号码");
    }

    public Receipt send(CrmMessage message) throws Exception {
        String requestId = "sms-" + message.getId();
        String body = json.writeValueAsString(Map.of("requestId", requestId,
                "phone", message.getRecipientPhone(), "content", message.getContent(), "sender", sender));
        HttpRequest request = request(endpoint).header("Idempotency-Key", requestId)
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return exchange(request);
    }

    public Receipt query(CrmMessage message) throws Exception {
        return exchange(request(endpoint + "?requestId=sms-" + message.getId()).GET().build());
    }

    private HttpRequest.Builder request(String url) {
        return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token).header("Accept", "application/json");
    }

    private Receipt exchange(HttpRequest request) throws Exception {
        var result = transport.exchange(request);
        String status = result.path("status").asText();
        if (!Set.of("ACCEPTED", "DELIVERED", "FAILED", "UNKNOWN").contains(status))
            throw new IOException("短信网关响应状态无效");
        String providerId = result.path("messageId").asText("");
        if (providerId.length() > 128) throw new IOException("短信网关消息编号过长");
        String error = switch (status) {
            case "FAILED" -> "短信网关报告发送失败";
            case "UNKNOWN" -> "发送结果待确认";
            default -> null;
        };
        return new Receipt(status, providerId, error);
    }

    public record Receipt(String status, String messageId, String error) { }
}
