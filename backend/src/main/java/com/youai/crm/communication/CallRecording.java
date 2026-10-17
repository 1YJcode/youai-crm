package com.youai.crm.communication;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "crm_call_recording")
public class CallRecording {
    @Id
    private Long callId;
    @Column(nullable = false, length = 64)
    private String mediaType;
    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] audio;
    @Column(nullable = false)
    private LocalDateTime uploadedAt;
    @Column(nullable = false, length = 36)
    private String revision = java.util.UUID.randomUUID().toString();
    @Column(nullable = false, length = 20)
    private String analysisStatus = "PENDING";
    private LocalDateTime analysisStartedAt;
    @Column(columnDefinition = "LONGTEXT")
    private String transcript;
    @Column(columnDefinition = "LONGTEXT")
    private String summary;
    @Column(length = 200)
    private String analysisError;
    @Column(columnDefinition = "LONGTEXT")
    private String reviewedSummary;
    @Column(length = 128)
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    @Column(nullable = false)
    private long reviewVersion;

    protected CallRecording() {}
    CallRecording(Long callId, String mediaType, byte[] audio) {
        this.callId = callId;
        this.mediaType = mediaType;
        this.audio = audio;
        this.uploadedAt = LocalDateTime.now();
    }
    public Long getCallId() { return callId; }
    public String getMediaType() { return mediaType; }
    public byte[] getAudio() { return audio; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public String getRevision() { return revision; }
    public String getAnalysisStatus() { return analysisStatus; }
    public LocalDateTime getAnalysisStartedAt() { return analysisStartedAt; }
    public String getTranscript() { return transcript; }
    public String getSummary() { return summary; }
    public String getAnalysisError() { return analysisError; }
    public String getReviewedSummary() { return reviewedSummary; }
    public String getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public long getReviewVersion() { return reviewVersion; }
}
