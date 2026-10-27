package com.youai.crm.communication;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.transaction.annotation.Transactional;

public interface RecordingJobRepository extends JpaRepository<RecordingJob,String> {
    @Query("select j.id from RecordingJob j where (j.state='QUEUED' and j.availableAt<=:now) or (j.state='RUNNING' and j.leaseUntil<:now) order by j.createdAt")
    List<String> available(LocalDateTime now,Pageable page);
    @Modifying @Transactional
    @Query("update RecordingJob j set j.state='RUNNING',j.token=:token,j.leaseUntil=:lease,j.attempts=j.attempts+1 where j.id=:id and ((j.state='QUEUED' and j.availableAt<=:now) or (j.state='RUNNING' and j.leaseUntil<:now))")
    int claim(String id,String token,LocalDateTime now,LocalDateTime lease);
    @Modifying @Transactional
    @Query("update RecordingJob j set j.leaseUntil=:lease where j.id=:id and j.token=:token and j.state='RUNNING'")
    int heartbeat(String id,String token,LocalDateTime lease);
    @Modifying @Transactional
    @Query("update RecordingJob j set j.state=:state,j.availableAt=:next,j.leaseUntil=null where j.id=:id and j.token=:token and j.state='RUNNING'")
    int finish(String id,String token,String state,LocalDateTime next);
}
