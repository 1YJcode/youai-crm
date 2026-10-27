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
    static final int MAX_BYTES = 200 * 1024 * 1024;
    private final CallRecordRepository calls;
    private final CallRecordingRepository recordings;
    private final CustomerAccessPolicy access;
    private final RecordingStorage storage;
    private final RecordingHistory history;
    private final RecordingArchiveRepository archives;
    private final RecordingAutomation automation;
    @org.springframework.beans.factory.annotation.Autowired private CallFollowupWorkService followupWork;
    private final java.util.concurrent.Semaphore uploads = new java.util.concurrent.Semaphore(1);

    public CallRecordingController(CallRecordRepository calls, CallRecordingRepository recordings,
            CustomerAccessPolicy access, RecordingStorage storage, RecordingHistory history, RecordingArchiveRepository archives,RecordingAutomation automation) {
        this.calls = calls;
        this.recordings = recordings;
        this.access = access;
        this.storage = storage;
        this.history=history; this.archives=archives;
        this.automation=automation;
    }

    private void authorize(Long callId, Authentication authentication) {
        var call = calls.findById(callId).orElseThrow(() -> new NotFoundException("未找到通话记录"));
        access.requireAccess(call.getCustomerNo(), authentication);
    }

    public record RecordingInfo(Long callId, String mediaType, int size, LocalDateTime uploadedAt) {}

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public RecordingInfo uploadRequest(@PathVariable Long callId,@RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue="false") boolean autoProcess,@RequestParam(defaultValue="false") boolean processingConsent,Authentication authentication)throws IOException {
        authorize(callId,authentication);automation.validate(autoProcess,processingConsent);
        var result=upload(callId,file,authentication);automation.enqueue(callId,autoProcess,processingConsent,authentication);return result;
    }
    @org.springframework.transaction.annotation.Transactional(rollbackFor = Exception.class)
    public RecordingInfo upload(@PathVariable Long callId, @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        authorize(callId, authentication);
        if (file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "录音不能为空");
        if (file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "录音不能超过200MB");
        if(!uploads.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"正在校验其他录音，请稍后重试");
        try {
        var temporary=java.nio.file.Files.createTempFile("youai-upload-",".audio");
        try {
        long size=0;
        try(var stream=file.getInputStream(); var output=java.nio.file.Files.newOutputStream(temporary)) {
            byte[] buffer=new byte[65536]; int count;
            while((count=stream.read(buffer))!=-1) {
                size+=count;
                if(size>MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"录音不能超过200MB");
                output.write(buffer,0,count);
            }
        }
        String mediaType = detectMediaType(temporary);
        int duration = RecordingDuration.seconds(temporary, mediaType);
        if (duration > 7200) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "录音不能超过两小时");
        var call=calls.lockById(callId).orElseThrow(() -> new NotFoundException("未找到通话记录"));
        access.requireAccess(call.getCustomerNo(),authentication);
        var recording = new CallRecording(callId, mediaType, null);
        String key="recordings/"+callId+"/"+recording.getRevision();
        storage.put(key,temporary,mediaType);
        RecordingHistory.cleanupOnRollback(storage,key);
        history.archive(recordings.findById(callId).orElse(null));
        recording.externalize(storage.backend(),key,size,RecordingStorage.sha256(temporary));
        recording = recordings.save(recording);
        call.setRecordingSource("UPLOAD");
        call.setDurationSeconds(duration);
        call.setUploadedAt(recording.getUploadedAt());
        calls.save(call);
        if(followupWork!=null)followupWork.register(callId);
        return new RecordingInfo(callId, mediaType, (int)size, recording.getUploadedAt());
        } finally { java.nio.file.Files.deleteIfExists(temporary); }
        } finally { uploads.release(); }
    }

    @GetMapping
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long callId, Authentication authentication, @RequestParam(required=false) String revision) throws IOException {
        authorize(callId, authentication);
        var recording = recordings.findById(callId).orElseThrow(() -> new NotFoundException("该通话暂无录音"));
        if(revision!=null && !revision.equals(recording.getRevision())) {
            var entry=archives.findById(revision).filter(a -> callId.equals(a.callId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,"来源录音已替换，未找到旧版本"));
            recording=entry.recording();
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(recording.getMediaType())).contentLength(recording.getAudioSize())
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=recording")
                .body(storage.resource(recording));
    }

    public record Playback(String url) {}
    public record HistoryView(String revision,LocalDateTime uploadedAt,LocalDateTime archivedAt,String snapshot) {}
    @GetMapping("/history")
    public org.springframework.data.domain.Page<HistoryView> history(@PathVariable Long callId,Authentication authentication,
            @RequestParam(defaultValue="0") int page,jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store"); authorize(callId,authentication);
        return archives.findByCallIdOrderByArchivedAtDesc(callId,org.springframework.data.domain.PageRequest.of(Math.max(0,page),20))
                .map(a -> new HistoryView(a.revision,a.uploadedAt,a.archivedAt,a.snapshot));
    }
    @GetMapping("/playback")
    public Playback playback(@PathVariable Long callId, Authentication authentication,@RequestParam(required=false) String revision,
            jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        authorize(callId,authentication);
        var recording=recordings.findById(callId).orElseThrow(() -> new NotFoundException("该通话暂无录音"));
        if(revision!=null && !revision.equals(recording.getRevision())) recording=archives.findById(revision)
                .filter(a -> callId.equals(a.callId)).orElseThrow(() -> new NotFoundException("未找到录音历史版本")).recording();
        return new Playback(storage.playbackUrl(recording));
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
            if (head.substring(4, 8).equals("ftyp")) return "audio/mp4";
            if (head.startsWith("fLaC")) return "audio/flac";
            if (head.startsWith("OggS")) return "audio/ogg";
        }
        throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "请选择WAV、MP3、M4A、FLAC或OGG录音");
    }

    static String detectMediaType(java.nio.file.Path path) throws IOException {
        byte[] header;
        try(var stream=java.nio.file.Files.newInputStream(path)) { header=stream.readNBytes(12); }
        if(header.length==12 && new String(header,0,4,StandardCharsets.ISO_8859_1).equals("RIFF")
                && new String(header,8,4,StandardCharsets.ISO_8859_1).equals("WAVE")) {
            try(var stream=javax.sound.sampled.AudioSystem.getAudioInputStream(path.toFile())) {
                long expected=stream.getFrameLength()*stream.getFormat().getFrameSize(), read=0;
                if(expected<=0 || expected>MAX_BYTES) throw new IOException("Invalid WAV length");
                byte[] buffer=new byte[65536]; int count;
                while((count=stream.read(buffer))!=-1) read+=count;
                if(read!=expected) throw new IOException("Truncated WAV");
                return "audio/wav";
            } catch(Exception e) { throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"WAV录音无效或已损坏，请重新导出"); }
        }
        return detectMediaType(header);
    }
}
