package com.youai.crm.communication;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Pageable;
import java.util.*;
public interface CallIntelligenceRepository extends JpaRepository<CallIntelligence,Long>{
    @Query("select i.followUpId from CallIntelligence i,CustomerTranscriptSegment s where i.segmentId=s.id and s.callId=:callId and i.followUpId is not null")
    List<Long> confirmedForCall(Long callId);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @Query("select i from CallIntelligence i where i.segmentId=:id")
    Optional<CallIntelligence> lockById(Long id);
    @Query("select i.segmentId from CallIntelligence i where i.state='WAITING_CONFIG' and i.processingConsent=true order by i.requestedAt")
    List<Long> waiting(Pageable page);
    Optional<CallIntelligence> findByActiveJobId(String id);
    Optional<CallIntelligence> findFirstByCustomerNoOrderBySegmentIdDesc(String customerNo);
    @Query("select i.followUpId from CallIntelligence i where i.customerNo=:customerNo and i.followUpId is not null")
    List<Long> confirmedFollowUps(String customerNo);
}
