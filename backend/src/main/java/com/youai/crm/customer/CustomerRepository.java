package com.youai.crm.customer;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    Optional<Customer> findByCustomerNo(String customerNo);
    Optional<Customer> findByPhone(String phone);
    long countByOwner(String owner);
    List<Customer> findAllByOwner(String owner);
    List<Customer> findAllByOwnerNotAndLastContactAtLessThanEqual(String owner, LocalDateTime cutoff);
    List<Customer> findAllByOwnerNotAndLastContactAtIsNullAndCreatedAtLessThanEqual(String owner, LocalDateTime cutoff);
}
