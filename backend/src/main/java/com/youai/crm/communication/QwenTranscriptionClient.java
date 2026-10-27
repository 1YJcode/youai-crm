package com.youai.crm.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Audio is sent only to the server-configured provider. No client URLs are accepted. */
@Component
public class QwenTranscriptionClient {
    private final ObjectMapper json;
    private final String key, model, endpoint, ffmpeg;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public QwenTranscriptionClient(ObjectMapper json,
            @Value("${recording.qwen.api-key:}") String key,
            @Value("${recording.qwen.model:qwen3-asr-flash}") String model,
            @Value("${recording.qwen.endpoint:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}") String endpoint,
            @Value("${recording.qwen.ffmpeg:ffmpeg}") String ffmpeg) {
        this.json = json; this.key = key; this.model = model; this.endpoint = endpoint; this.ffmpeg = ffmpeg;
    }

    public boolean configured() { return !key.isBlank() && !model.isBlank() && !endpoint.isBlank(); }

    public String transcribe(byte[] audio, String mediaType) throws Exception {
        if (!configured()) throw new IllegalStateException("管理员尚未配置千问转写服务");
        int duration = RecordingDuration.seconds(audio, mediaType);
        if (duration > 7200) throw new IllegalArgumentException("录音不能超过两小时");
        String format = switch (mediaType) {
            case "audio/mp4" -> "mov"; case "audio/wav" -> "wav"; case "audio/mpeg" -> "mp3";
            case "audio/flac" -> "flac"; case "audio/ogg" -> "ogg";
            default -> throw new IllegalArgumentException("不支持的录音格式");
        };
        Path directory = Files.createTempDirectory("youai-qwen-");
        try {
            Path source = directory.resolve("source." + format);
            Files.write(source, audio);
            StringBuilder transcript = new StringBuilder();
            for (int start = 0; start < duration; start += 240) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                Path segment = directory.resolve("segment.wav");
                Process process = new ProcessBuilder(ffmpeg, "-nostdin", "-v", "error", "-y",
                        "-protocol_whitelist", "file,pipe", "-f", format, "-ss", String.valueOf(start),
                        "-i", source.toString(), "-t", "240", "-vn", "-ac", "1", "-ar", "16000",
                        "-c:a", "pcm_s16le", segment.toString())
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
                try {
                    if (!process.waitFor(60, TimeUnit.SECONDS) || process.exitValue() != 0)
                        throw new IllegalStateException("录音分段失败");
                } finally { process.destroyForcibly(); }
                String text = recognize(Files.readAllBytes(segment));
                transcript.append(String.format("[录音分段 %02d:%02d–%02d:%02d，非逐句时间戳]%n",
                        start / 60, start % 60, Math.min(start + 240, duration) / 60, Math.min(start + 240, duration) % 60))
                        .append(text).append("\n\n");
                if (transcript.length() > 200000) throw new IllegalStateException("转写结果过长");
            }
            return transcript.toString().trim();
        } finally {
            try (var paths = Files.list(directory)) {
                for (Path path : paths.toList()) Files.deleteIfExists(path);
            }
            Files.deleteIfExists(directory);
        }
    }

    String recognize(byte[] wav) throws Exception {
        if (!configured()) throw new IllegalStateException("管理员尚未配置千问转写服务");
        byte[] body = json.writeValueAsBytes(Map.of("model", model, "stream", false,
                "messages", List.of(Map.of("role", "user", "content", List.of(Map.of("type", "input_audio",
                        "input_audio", Map.of("data", "data:audio/wav;base64," + Base64.getEncoder().encodeToString(wav)))))),
                "asr_options", Map.of("enable_itn", false)));
        var request = HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (var stream = response.body()) {
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("千问转写请求失败（HTTP " + response.statusCode() + "）");
            byte[] bytes = stream.readNBytes(1024 * 1024 + 1);
            if (bytes.length > 1024 * 1024) throw new IllegalStateException("千问响应过长");
            var choice = json.readTree(bytes).path("choices").path(0);
            String text = choice.path("message").path("content").asText("").trim();
            if (!"stop".equals(choice.path("finish_reason").asText()) || text.isBlank())
                throw new IllegalStateException("千问未返回完整转写，请重试");
            return text;
        }
    }
}
