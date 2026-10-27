package com.youai.crm.communication;

import java.time.LocalDateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Staging PUT credentials expire before cleanup. Saved recording keys are never touched. */
@Component
public class RecordingUploadCleanup {
    private final RecordingUploadRepository uploads;
    private final RecordingStorage storage;
    public RecordingUploadCleanup(RecordingUploadRepository uploads,RecordingStorage storage) { this.uploads=uploads; this.storage=storage; }
    @Scheduled(fixedDelayString="${recording.storage.cleanup-ms:3600000}",initialDelay=60000)
    public void cleanup() {
        if(!"s3".equals(storage.backend())) return;
        for(var upload:uploads.findByExpiresAtBefore(LocalDateTime.now().minusDays(1),PageRequest.of(0,100))) {
            try { storage.delete("uploads/"+upload.id); uploads.deleteById(upload.id); }
            catch(Exception e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("Staging recording cleanup failed ({})",e.getClass().getSimpleName()); }
        }
    }
}
