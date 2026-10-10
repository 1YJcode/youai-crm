package com.youai.crm.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowUpTaskRepository extends JpaRepository<FollowUpTask, Long>, JpaSpecificationExecutor<FollowUpTask> {
    List<FollowUpTask> findAllByOrderByDueAtAsc();
    long countByCompletedFalseAndDueAtBetween(LocalDateTime start, LocalDateTime end);
    long countByCompletedFalse();

    interface CustomerRecordCount {
        String getCustomerNo();
        long getRecordCount();
    }

    @Query("select t.customerId as customerNo, count(t) as recordCount from FollowUpTask t "
            + "where t.customerId in :customerNos group by t.customerId")
    List<CustomerRecordCount> countByCustomerNos(@Param("customerNos") Collection<String> customerNos);

    @Query("select count(distinct t.customerName) from FollowUpTask t where (:owner is null or t.owner = :owner) "
            + "and t.dueAt >= :start and t.dueAt < :end")
    long countDueCustomersForDashboard(@Param("owner") String owner, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("select count(t) from FollowUpTask t where (:owner is null or t.owner = :owner) and t.completed = false")
    long countPendingForDashboard(@Param("owner") String owner);
}

