package com.youai.crm.communication;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RecordingHistory {
    private final RecordingStorage storage;
    private final RecordingArchiveRepository archives;
    private final RecordingTranscriptionRepository transcriptions;
    private final ObjectMapper json;
    private final CallRecordRepository calls;
    public RecordingHistory(RecordingStorage storage,RecordingArchiveRepository archives,
            RecordingTranscriptionRepository transcriptions,ObjectMapper json,CallRecordRepository calls) {
        this.storage=storage; this.archives=archives; this.transcriptions=transcriptions; this.json=json;
        this.calls=calls;
    }
    /** Caller holds the call row lock. Never discard the previous recording or its text. */
    void archive(CallRecording old) throws IOException {
        if(old==null || archives.existsById(old.getRevision())) return;
        var entry=new RecordingArchive(); entry.revision=old.getRevision(); entry.callId=old.getCallId();
        entry.mediaType=old.getMediaType(); entry.uploadedAt=old.getUploadedAt(); entry.archivedAt=LocalDateTime.now();
        if(old.getStorageKey()==null) {
            Path path=Files.createTempFile("youai-archive-",".audio");
            String key="recordings/"+old.getCallId()+"/"+UUID.randomUUID();
            try { Files.write(path,old.getAudio()); storage.put(key,path,old.getMediaType()); }
            finally { Files.deleteIfExists(path); }
            entry.storageBackend=storage.backend(); entry.storageKey=key;
            entry.audioSize=old.getAudioSize(); entry.audioSha256=RecordingStorage.sha256(old.getAudio());
            cleanupOnRollback(storage,key);
        } else {
            entry.storageBackend=old.getStorageBackend(); entry.storageKey=old.getStorageKey();
            entry.audioSize=old.getAudioSize(); entry.audioSha256=old.getAudioSha256();
        }
        Map<String,Object> snapshot=new LinkedHashMap<>();
        calls.findById(old.getCallId()).ifPresent(call->{snapshot.put("durationSeconds",call.getDurationSeconds());snapshot.put("recordingSource",call.getRecordingSource());});
        snapshot.put("transcript",old.getTranscript()); snapshot.put("summary",old.getSummary());
        snapshot.put("reviewedSummary",old.getReviewedSummary()); snapshot.put("reviewedBy",old.getReviewedBy());
        snapshot.put("reviewedAt",old.getReviewedAt());
        for(String provider:List.of("local","cloud")) {
            var row=transcriptions.findById(old.getCallId()+":"+provider).orElse(null);
            if(row!=null && old.getRevision().equals(row.revision)) {
                Map<String,Object> result=new LinkedHashMap<>(); result.put("transcript",row.transcript);
                result.put("status",row.status); result.put("started",row.started); result.put("requestedBy",row.requestedBy);
                snapshot.put(provider,result);
            }
        }
        entry.snapshot=json.writeValueAsString(snapshot); archives.saveAndFlush(entry);
    }
    static void cleanupOnRollback(RecordingStorage storage,String key) {
        if(TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override public void afterCompletion(int status) {
                    if(status==STATUS_ROLLED_BACK) try { storage.delete(key); }
                    catch(Exception e) { org.slf4j.LoggerFactory.getLogger(RecordingHistory.class).warn("Recording rollback cleanup failed; run orphan audit"); }
                }
            });
    }
}
