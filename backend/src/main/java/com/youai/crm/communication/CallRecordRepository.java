package com.youai.crm.communication;

import java.util.List;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface CallRecordRepository extends JpaRepository<CallRecord, Long>, JpaSpecificationExecutor<CallRecord> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CallRecord c where c.id=:id")
    java.util.Optional<CallRecord> lockById(Long id);
    List<CallRecord> findAllByOrderByStartedAtDesc();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<CallRecord> findByAgentUserIdAndGatewayRequestKey(Long agentUserId, String requestKey);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<CallRecord> findByGatewayCallId(String gatewayCallId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    List<CallRecord> findByAgentUserIdAndGatewayStateIn(Long agentUserId, List<String> states);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update CallRecord c set c.browserLastSeenAt=:now where c.id=:id and c.agentUserId=:agentId and c.gatewayState not in ('LOCAL', 'ENDED', 'FAILED')")
    int touchBrowserCall(@Param("id") Long id, @Param("agentId") Long agentId, @Param("now") LocalDateTime now);

    List<CallRecord> findTop20ByGatewayStateInAndBrowserLastSeenAtBeforeOrderByIdAsc(List<String> states, LocalDateTime cutoff);

    List<CallRecord> findTop20ByGatewayStateOrderByIdAsc(String state);
    List<CallRecord> findTop20ByHangupRequestedTrueOrderByIdAsc();
    List<CallRecord> findTop20ByGatewayStateInAndGatewayUpdatedAtBeforeOrderByGatewayUpdatedAtAsc(
            List<String> states, LocalDateTime cutoff);
    List<CallRecord> findTop20ByAgentUserIdAndDirectionAndGatewayStateInOrderByIdDesc(
            Long agentUserId, String direction, List<String> states);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update CallRecord c set c.gatewayState='SUBMITTING', c.status='呼叫中', c.gatewayUpdatedAt=:now where c.id=:id and c.gatewayState='QUEUED' and c.hangupRequested=false")
    int claimGatewayCall(@Param("id") Long id, @Param("now") LocalDateTime now);

    @org.springframework.data.jpa.repository.Modifying
    @Query("update CallRecord c set c.gatewayState='UNKNOWN', c.status='结果待确认', c.gatewayUpdatedAt=:now where c.gatewayState='SUBMITTING' and c.gatewayUpdatedAt < :cutoff")
    int recoverGatewayCalls(@Param("now") LocalDateTime now, @Param("cutoff") LocalDateTime cutoff);

    interface CustomerRecordCount {
        String getCustomerNo();
        long getRecordCount();
    }

    @Query("select c.customerNo as customerNo, count(c) as recordCount from CallRecord c "
            + "where c.customerNo in :customerNos group by c.customerNo")
    List<CustomerRecordCount> countByCustomerNos(@Param("customerNos") Collection<String> customerNos);

    @Query("select coalesce(sum(c.durationSeconds), 0) from CallRecord c where (:owner is null or c.owner = :owner) "
            + "and c.startedAt >= :start and c.startedAt < :end")
    long sumDurationForDashboard(@Param("owner") String owner, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("select count(c) from CallRecord c where (:owner is null or c.owner = :owner) "
            + "and c.startedAt >= :start and c.startedAt < :end and c.durationSeconds >= :minimum")
    long countDeepForDashboard(@Param("owner") String owner, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, @Param("minimum") int minimum);

    @Query("select new com.youai.crm.communication.CustomerCallDuration(c.customerNo, coalesce(sum(c.durationSeconds), 0)) "
            + "from CallRecord c where c.customerNo in :customerNos and c.durationSeconds > 0 group by c.customerNo")
    List<CustomerCallDuration> sumDurationByCustomerNos(@Param("customerNos") Collection<String> customerNos);
}
