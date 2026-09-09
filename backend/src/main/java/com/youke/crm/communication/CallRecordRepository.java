package com.youke.crm.communication;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {
    List<CallRecord> findAllByOrderByStartedAtDesc();
}
