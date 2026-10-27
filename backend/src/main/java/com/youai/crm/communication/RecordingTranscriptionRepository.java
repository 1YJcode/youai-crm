package com.youai.crm.communication;
import org.springframework.data.jpa.repository.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
public interface RecordingTranscriptionRepository extends JpaRepository<RecordingTranscription,String> {
 @Modifying @Transactional
 @Query("update RecordingTranscription t set t.revision=:revision,t.status='PROCESSING',t.started=:now,t.error=null,t.transcript=null where t.id=:id and (t.status<>'PROCESSING' or t.started<:expired or t.revision<>:revision)")
 int claim(String id,String revision,LocalDateTime now,LocalDateTime expired);
 @Modifying @Transactional
 @Query("update RecordingTranscription t set t.status=:status,t.transcript=:text,t.error=:error where t.id=:id and t.revision=:revision and t.started=:started and t.status='PROCESSING'")
 int finish(String id,String revision,LocalDateTime started,String status,String text,String error);
}
