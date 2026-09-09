package com.youke.crm.customer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAssignmentEventRepository extends JpaRepository<CustomerAssignmentEvent, Long> {
    List<CustomerAssignmentEvent> findAllByCustomerIdOrderByAssignedAtDesc(Long customerId);
}
