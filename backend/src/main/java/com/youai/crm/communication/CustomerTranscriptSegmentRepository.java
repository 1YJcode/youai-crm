package com.youai.crm.communication;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
public interface CustomerTranscriptSegmentRepository extends JpaRepository<CustomerTranscriptSegment,Long> {
    @Query("select distinct s.customerNo from CustomerTranscriptSegment s,CallIntelligence i where i.segmentId=s.id and i.processingConsent=true")
    java.util.List<String> reviewCustomers();
    @Query("select s from CustomerTranscriptSegment s where s.customerNo=:customerNo and not exists (select n.id from CustomerTranscriptSegment n where n.callId=s.callId and n.id>s.id) order by s.sourceTime,s.id")
    java.util.List<CustomerTranscriptSegment> reviewSources(String customerNo,Pageable page);
    @Query("select s.id from CustomerTranscriptSegment s left join CallIntelligence i on i.segmentId=s.id where not exists (select newer.id from CustomerTranscriptSegment newer where newer.callId=s.callId and newer.id>s.id) and not exists (select w.callId from CallFollowupWork w where w.callId=s.callId and w.followUpId is not null) and not exists (select old.segmentId from CallIntelligence old,CustomerTranscriptSegment source where old.segmentId=source.id and source.callId=s.callId and old.followUpId is not null) and (i is null or i.state='WAITING_CONSENT' or (coalesce(i.promptVersion,'')<>:version and i.state not in ('QUEUED','PROCESSING'))) order by s.id")
    java.util.List<Long> needingAutomaticDrafts(String version,Pageable page);
    java.util.Optional<CustomerTranscriptSegment> findFirstByCallIdOrderByIdDesc(Long callId);
    java.util.Optional<CustomerTranscriptSegment> findFirstByCustomerNoOrderBySourceTimeDescIdDesc(String customerNo);
    java.util.Optional<CustomerTranscriptSegment> findByCallIdAndRecordingRevision(Long callId,String revision);
    Page<CustomerTranscriptSegment> findByCustomerNo(String customerNo,Pageable page);
    interface Totals {long getSegments(); Long getDuration(); Long getUnknownDuration();}
    @Query("select count(s) as segments,coalesce(sum(s.durationSeconds),0) as duration,coalesce(sum(case when s.durationSeconds is null then 1 else 0 end),0) as unknownDuration from CustomerTranscriptSegment s where s.customerNo=:customerNo")
    Totals totals(String customerNo);
}
