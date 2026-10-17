package com.youai.crm.communication;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRecordingRepository extends JpaRepository<CallRecording, Long> {
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("update CallRecording r set r.analysisStatus='PROCESSING', r.analysisStartedAt=:now, r.analysisError=null, r.transcript=null, r.summary=null, r.reviewedSummary=null, r.reviewedBy=null, r.reviewedAt=null, r.reviewVersion=r.reviewVersion+1 where r.callId=:id and r.revision=:revision and (r.analysisStatus<>'PROCESSING' or r.analysisStartedAt<:expired)")
    int claim(Long id, String revision, java.time.LocalDateTime now, java.time.LocalDateTime expired);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("update CallRecording r set r.analysisStatus=:status, r.transcript=:transcript, r.summary=:summary, r.analysisError=:error where r.callId=:id and r.revision=:revision and r.analysisStatus='PROCESSING' and r.analysisStartedAt=:started")
    int finish(Long id, String revision, java.time.LocalDateTime started, String status, String transcript, String summary, String error);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("update CallRecording r set r.reviewedSummary=:summary, r.reviewedBy=:reviewer, r.reviewedAt=:now, r.reviewVersion=r.reviewVersion+1 where r.callId=:id and r.revision=:revision and r.analysisStartedAt=:started and r.analysisStatus='COMPLETED' and r.reviewVersion=:version")
    int review(Long id, String revision, java.time.LocalDateTime started, long version, String summary, String reviewer, java.time.LocalDateTime now);
}
