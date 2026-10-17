package com.youai.crm.communication;

import com.youai.crm.common.NotFoundException;
import com.youai.crm.customer.CustomerAccessPolicy;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/calls/{callId}/recording/analysis")
public class RecordingAnalysisController {
    private final CallRecordRepository calls;
    private final CallRecordingRepository recordings;
    private final CustomerAccessPolicy access;
    private final RecordingAiClient ai;
    private final ExecutorService workers = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.SECONDS,
            new SynchronousQueue<>(), runnable -> { var thread = new Thread(runnable, "recording-analysis"); thread.setDaemon(true); return thread; },
            new ThreadPoolExecutor.AbortPolicy());

    public RecordingAnalysisController(CallRecordRepository calls, CallRecordingRepository recordings,
            CustomerAccessPolicy access, RecordingAiClient ai) {
        this.calls = calls; this.recordings = recordings; this.access = access; this.ai = ai;
    }
    @jakarta.annotation.PreDestroy
    void shutdown() { workers.shutdownNow(); }

    private String authorize(Long id, Authentication authentication) {
        var call = calls.findById(id).orElseThrow(() -> new NotFoundException("未找到通话记录"));
        access.requireAccess(call.getCustomerNo(), authentication);
        return call.getCustomerNo();
    }
    public record AnalysisView(boolean configured, String status, String transcript, String summary, String error) {}
    @GetMapping
    public AnalysisView get(@PathVariable Long callId, Authentication authentication, jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        authorize(callId, authentication);
        var recording = recordings.findById(callId).orElse(null);
        if (recording == null) return new AnalysisView(ai.configured(), "NO_RECORDING", null, null, null);
        boolean expired = "PROCESSING".equals(recording.getAnalysisStatus()) && recording.getAnalysisStartedAt() != null
                && recording.getAnalysisStartedAt().isBefore(LocalDateTime.now().minusMinutes(10));
        return new AnalysisView(ai.configured(), expired ? "FAILED" : recording.getAnalysisStatus(),
                recording.getTranscript(), recording.getSummary(), expired ? "上次任务中断，请重新分析" : recording.getAnalysisError());
    }
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AnalysisView start(@PathVariable Long callId, Authentication authentication) {
        String customerNo = authorize(callId, authentication);
        if (!ai.configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "管理员尚未配置录音AI服务");
        var recording = recordings.findById(callId).orElseThrow(() -> new NotFoundException("请先上传录音"));
        LocalDateTime started = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        if (recordings.claim(callId, recording.getRevision(), started, started.minusMinutes(10)) == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "录音已变更或正在分析，请刷新");
        try {
            workers.execute(() -> {
                try {
                    access.requireAccess(customerNo, authentication);
                    var result = ai.analyze(recording.getAudio(), recording.getMediaType());
                    recordings.finish(callId, recording.getRevision(), started, "COMPLETED", result.transcript(), result.summary(), null);
                } catch (Exception error) {
                    if (error instanceof InterruptedException) Thread.currentThread().interrupt();
                    recordings.finish(callId, recording.getRevision(), started, "FAILED", null, null,
                            "录音分析失败，请检查服务配置和音频，稍后重试");
                }
            });
        } catch (RejectedExecutionException error) {
            recordings.finish(callId, recording.getRevision(), started, "FAILED", null, null, "分析服务繁忙，请稍后重试");
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "分析服务繁忙，请稍后重试");
        }
        return new AnalysisView(true, "PROCESSING", null, null, null);
    }
}
