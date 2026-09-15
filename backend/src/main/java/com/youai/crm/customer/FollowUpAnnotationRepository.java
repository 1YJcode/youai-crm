package com.youai.crm.customer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowUpAnnotationRepository extends JpaRepository<FollowUpAnnotation, Long> {
    List<FollowUpAnnotation> findAllByCustomerNoAndAuthor(String customerNo, String author);

    Optional<FollowUpAnnotation> findByCustomerNoAndRecordIdAndRecordTypeAndAuthor(
            String customerNo, String recordId, String recordType, String author);
}
