package com.youai.crm.communication;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface CallFollowupWorkRepository extends JpaRepository<CallFollowupWork,Long>,JpaSpecificationExecutor<CallFollowupWork> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from CallFollowupWork w where w.callId=:id") Optional<CallFollowupWork> lockById(Long id);
    List<CallFollowupWork> findByFollowUpIdIsNullOrderByStartedAtDesc();
    Optional<CallFollowupWork> findByFollowUpId(Long id);
}
