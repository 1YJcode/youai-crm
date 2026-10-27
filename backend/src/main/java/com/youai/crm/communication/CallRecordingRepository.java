package com.youai.crm.communication;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRecordingRepository extends JpaRepository<CallRecording, Long> {
    @org.springframework.data.jpa.repository.Query("select r.callId from CallRecording r where r.storageKey is null or r.storageBackend<>:backend order by r.callId")
    java.util.List<Long> legacyIds(String backend,org.springframework.data.domain.Pageable page);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("update CallRecording r set r.audio=null,r.storageBackend=:backend,r.storageKey=:key,r.audioSize=:size,r.audioSha256=:hash where r.callId=:id and r.revision=:revision and (r.storageKey is null or r.storageBackend<>:backend)")
    int externalize(Long id,String revision,String backend,String key,Long size,String hash);
    @org.springframework.data.jpa.repository.Query("select r.revision from CallRecording r where r.callId=:id")
    java.util.Optional<String> findRevision(Long id);

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
