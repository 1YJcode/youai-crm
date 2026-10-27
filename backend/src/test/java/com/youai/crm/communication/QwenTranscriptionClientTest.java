package com.youai.crm.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class QwenTranscriptionClientTest {
    @Test
    void sendsAudioOnlyAndRejectsIncompleteResults() throws Exception {
        var json = new ObjectMapper();
        var request = new AtomicReference<String>();
        var authorization = new AtomicReference<String>();
        var response = new AtomicReference<>("{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"客户希望周五回访\"}}]}");
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] bytes = response.get().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            var client = new QwenTranscriptionClient(json, "test-only", "qwen3-asr-flash",
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/", "ffmpeg");
            assertEquals("客户希望周五回访", client.recognize(new byte[]{1, 2, 3}));
            var body = json.readTree(request.get());
            assertEquals("Bearer test-only", authorization.get());
            assertEquals("qwen3-asr-flash", body.path("model").asText());
            assertEquals("data:audio/wav;base64,AQID", body.path("messages").path(0).path("content").path(0).path("input_audio").path("data").asText());
            assertFalse(body.path("asr_options").path("enable_itn").asBoolean());
            var wav = new java.io.ByteArrayOutputStream();
            try (var audio = new javax.sound.sampled.AudioInputStream(
                    new java.io.ByteArrayInputStream(new byte[32000]),
                    new javax.sound.sampled.AudioFormat(16000, 16, 1, true, false), 16000)) {
                javax.sound.sampled.AudioSystem.write(audio, javax.sound.sampled.AudioFileFormat.Type.WAVE, wav);
            }
            String transcript = client.transcribe(wav.toByteArray(), "audio/wav");
            assertTrue(transcript.contains("00:00–00:01"));
            assertTrue(transcript.contains("客户希望周五回访"));
            response.set("{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"截断\"}}]}");
            assertThrows(IllegalStateException.class, () -> client.recognize(new byte[]{1}));
        } finally { server.stop(0); }
    }

    @Test
    void noCredentialsNeverSendsAudio() {
        var client = new QwenTranscriptionClient(new ObjectMapper(), "", "qwen3-asr-flash", "invalid", "ffmpeg");
        assertFalse(client.configured());
        assertThrows(IllegalStateException.class, () -> client.transcribe(new byte[]{1}, "audio/wav"));
    }
}
