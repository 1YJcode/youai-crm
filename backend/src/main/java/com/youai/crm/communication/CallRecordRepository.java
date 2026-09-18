package com.youai.crm.communication;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

public interface CallRecordRepository extends JpaRepository<CallRecord, Long>, JpaSpecificationExecutor<CallRecord> {
    List<CallRecord> findAllByOrderByStartedAtDesc();

    @Query("select coalesce(sum(c.durationSeconds), 0) from CallRecord c where (:owner is null or c.owner = :owner) "
            + "and c.startedAt >= :start and c.startedAt < :end")
    long sumDurationForDashboard(@Param("owner") String owner, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("select count(c) from CallRecord c where (:owner is null or c.owner = :owner) "
            + "and c.startedAt >= :start and c.startedAt < :end and c.durationSeconds >= :minimum")
    long countDeepForDashboard(@Param("owner") String owner, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, @Param("minimum") int minimum);
}
