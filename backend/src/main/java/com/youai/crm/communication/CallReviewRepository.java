package com.youai.crm.communication;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CallReviewRepository extends JpaRepository<CallReview, Long> {
    List<CallReview> findAllByCallIdIn(List<Long> callIds);
}
