package com.youai.crm.communication;

import com.youai.crm.common.NotFoundException;
import com.youai.crm.customer.CustomerAccessPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/calls/{callId}/recording/review")
public class RecordingReviewController {
    private final CallRecordRepository calls;
    private final CallRecordingRepository recordings;
    private final CustomerAccessPolicy access;
    public RecordingReviewController(CallRecordRepository calls, CallRecordingRepository recordings, CustomerAccessPolicy access) {
        this.calls = calls; this.recordings = recordings; this.access = access;
    }
    private void authorize(Long id, Authentication authentication) {
        var call = calls.findById(id).orElseThrow(() -> new NotFoundException("未找到通话记录"));
        access.requireAccess(call.getCustomerNo(), authentication);
    }
    public record ReviewView(String revision, LocalDateTime started, long version, String text, String reviewer, LocalDateTime reviewedAt) {}
    public record ReviewRequest(@NotBlank @Size(max=36) String revision, @NotNull LocalDateTime started,
            @Min(0) long version, @NotBlank @Size(max=50000) String text) {}

    @GetMapping
    public ReviewView get(@PathVariable Long callId, Authentication authentication, jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        authorize(callId, authentication);
        var recording = recordings.findById(callId).orElseThrow(() -> new NotFoundException("请先上传录音"));
        if (!"COMPLETED".equals(recording.getAnalysisStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "请等待分析完成");
        return new ReviewView(recording.getRevision(), recording.getAnalysisStartedAt(), recording.getReviewVersion(),
                recording.getReviewedSummary() == null ? recording.getSummary() : recording.getReviewedSummary(),
                recording.getReviewedBy(), recording.getReviewedAt());
    }
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@PathVariable Long callId, @Valid @RequestBody ReviewRequest request, Authentication authentication) {
        authorize(callId, authentication);
        if (recordings.review(callId, request.revision(), request.started(), request.version(), request.text().trim(),
                authentication.getName(), LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS)) == 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "录音、分析或审核结果已更新，请重新打开后核对");
    }
}
