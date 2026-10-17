package com.youai.crm.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class RecordingAiClientTest {
    @Test
    void sendsAudioAndSummarizesTranscriptWithoutStoringProviderResponse() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var audioRequest = new AtomicReference<String>();
        var summaryRequest = new AtomicReference<String>();
        server.createContext("/audio/transcriptions", exchange -> {
            audioRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] result = "{\"text\":\"客户说周五回访\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, result.length); exchange.getResponseBody().write(result); exchange.close();
        });
        server.createContext("/responses", exchange -> {
            summaryRequest.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] result = "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"约定：周五回访\"}]}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, result.length); exchange.getResponseBody().write(result); exchange.close();
        });
        server.start();
        try {
            var json = new ObjectMapper();
            var client = new RecordingAiClient(json, "test-only", "transcription-test", "summary-test",
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"));
            var result = client.analyze("audio-fixture".getBytes(), "audio/wav");
            assertEquals("客户说周五回访", result.transcript());
            assertEquals("约定：周五回访", result.summary());
            assertTrue(audioRequest.get().contains("filename=\"recording.wav\""));
            assertTrue(audioRequest.get().contains("audio-fixture"));
            var request = json.readTree(summaryRequest.get());
            assertFalse(request.get("store").asBoolean());
            assertEquals(result.transcript(), request.get("input").asText());
        } finally { server.stop(0); }
    }

    @Test
    void refusesUnconfiguredServiceWithoutNetwork() {
        var client = new RecordingAiClient(new ObjectMapper(), "", "", "");
        assertFalse(client.configured());
        assertThrows(IllegalStateException.class, () -> client.analyze(new byte[0], "audio/wav"));
    }
}
