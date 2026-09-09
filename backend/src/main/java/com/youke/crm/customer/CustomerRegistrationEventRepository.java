package com.youke.crm.customer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRegistrationEventRepository extends JpaRepository<CustomerRegistrationEvent, Long> {
    List<CustomerRegistrationEvent> findAllByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
