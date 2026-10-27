package com.youai.crm.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

@Component
public class LocalTranscriptionClient {
    private final boolean enabled;
    private final URI endpoint;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public LocalTranscriptionClient(ObjectMapper json,
            @Value("${recording.local.enabled:false}") boolean enabled,
            @Value("${recording.local.endpoint:http://127.0.0.1:8766/transcribe}") String endpoint) {
        this.json = json; this.enabled = enabled; this.endpoint = URI.create(endpoint);
    }
    public boolean configured() { return enabled; }
    public String transcribe(byte[] audio, String mediaType) throws Exception {
        if (!enabled) throw new IllegalStateException("本地转写未启用");
        var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofMinutes(110))
                .header("Content-Type", mediaType).POST(HttpRequest.BodyPublishers.ofByteArray(audio)).build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (var stream = response.body()) {
            if (response.statusCode() != 200) throw new IllegalStateException("本地转写失败");
            byte[] bytes = stream.readNBytes(4 * 1024 * 1024 + 1);
            if (bytes.length > 4 * 1024 * 1024) throw new IllegalStateException("转写结果过长");
            String text = json.readTree(bytes).path("text").asText("").trim();
            if (text.isEmpty() || text.length() > 200000) throw new IllegalStateException("转写原文为空或过长");
            return text;
        }
    }
}
