package com.youai.crm.communication;

import com.youai.crm.common.NotFoundException;
import com.youai.crm.customer.CustomerAccessPolicy;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/calls/{callId}/recording")
public class CallRecordingController {
    static final int MAX_BYTES = 20 * 1024 * 1024;
    private final CallRecordRepository calls;
    private final CallRecordingRepository recordings;
    private final CustomerAccessPolicy access;

    public CallRecordingController(CallRecordRepository calls, CallRecordingRepository recordings,
            CustomerAccessPolicy access) {
        this.calls = calls;
        this.recordings = recordings;
        this.access = access;
    }

    private void authorize(Long callId, Authentication authentication) {
        var call = calls.findById(callId).orElseThrow(() -> new NotFoundException("未找到通话记录"));
        access.requireAccess(call.getCustomerNo(), authentication);
    }

    public record RecordingInfo(Long callId, String mediaType, int size, LocalDateTime uploadedAt) {}

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecordingInfo upload(@PathVariable Long callId, @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        authorize(callId, authentication);
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "录音不能为空");
        if (file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "录音不能超过20MB");
        byte[] audio;
        try (var stream = file.getInputStream()) { audio = stream.readNBytes(MAX_BYTES + 1); }
        if (audio.length > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "录音不能超过20MB");
        String mediaType = detectMediaType(audio);
        var recording = recordings.save(new CallRecording(callId, mediaType, audio));
        return new RecordingInfo(callId, mediaType, audio.length, recording.getUploadedAt());
    }

    @GetMapping
    public ResponseEntity<byte[]> download(@PathVariable Long callId, Authentication authentication) {
        authorize(callId, authentication);
        var recording = recordings.findById(callId).orElseThrow(() -> new NotFoundException("该通话暂无录音"));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(recording.getMediaType()))
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=recording")
                .body(recording.getAudio());
    }

    static String detectMediaType(byte[] audio) {
        if (audio.length >= 12) {
            String head = new String(audio, 0, 12, StandardCharsets.ISO_8859_1);
            if (head.startsWith("RIFF") && head.endsWith("WAVE")) {
                try (var stream = javax.sound.sampled.AudioSystem.getAudioInputStream(new java.io.ByteArrayInputStream(audio))) {
                    long expected = stream.getFrameLength() * stream.getFormat().getFrameSize();
                    if (expected <= 0 || expected > MAX_BYTES) throw new IOException("Invalid WAV length");
                    long read = 0;
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = stream.read(buffer)) != -1) read += count;
                    if (read != expected) throw new IOException("Truncated WAV");
                    return "audio/wav";
                } catch (Exception error) {
                    throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "WAV录音无效或已损坏，请重新导出");
                }
            }
            if (head.startsWith("ID3") || ((audio[0] & 0xff) == 0xff && (audio[1] & 0xe6) == 0xe2)) return "audio/mpeg";
            if (head.startsWith("fLaC")) return "audio/flac";
            if (head.startsWith("OggS")) return "audio/ogg";
        }
        throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "请选择WAV、MP3、FLAC或OGG录音");
    }
}
