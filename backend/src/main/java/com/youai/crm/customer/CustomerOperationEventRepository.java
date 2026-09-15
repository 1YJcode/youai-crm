package com.youai.crm.customer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOperationEventRepository extends JpaRepository<CustomerOperationEvent, Long> {
    List<CustomerOperationEvent> findAllByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
