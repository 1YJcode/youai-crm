package com.youai.crm.customer;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    Optional<Customer> findByCustomerNo(String customerNo);
    Optional<Customer> findByPhone(String phone);
    long countByOwner(String owner);
    List<Customer> findAllByOwner(String owner);
    List<Customer> findAllByOwnerNotAndLastContactAtLessThan(String owner, LocalDateTime cutoff);
    List<Customer> findAllByOwnerNotAndLastContactAtIsNullAndCreatedAtLessThan(String owner, LocalDateTime cutoff);

    @Query("select count(c) from Customer c where (:owner is null or c.owner = :owner)")
    long countForDashboard(@Param("owner") String owner);

    @Query("select count(c) from Customer c where (:owner is null or c.owner = :owner) "
            + "and c.createdAt >= :start and c.createdAt < :end")
    long countNewForDashboard(@Param("owner") String owner, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("select coalesce(sum(c.expectedAmount), 0) from Customer c where (:owner is null or c.owner = :owner)")
    java.math.BigDecimal sumPipelineForDashboard(@Param("owner") String owner);

    @Query("select count(c) from Customer c where (:owner is null or c.owner = :owner) and c.stage = :stage")
    long countByStageForDashboard(@Param("owner") String owner, @Param("stage") String stage);
}
