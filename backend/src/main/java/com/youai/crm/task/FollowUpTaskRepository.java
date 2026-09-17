package com.youai.crm.task;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FollowUpTaskRepository extends JpaRepository<FollowUpTask, Long>, JpaSpecificationExecutor<FollowUpTask> {
    List<FollowUpTask> findAllByOrderByDueAtAsc();
    long countByCompletedFalseAndDueAtBetween(LocalDateTime start, LocalDateTime end);
    long countByCompletedFalse();
}

