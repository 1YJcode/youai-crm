package com.youai.crm.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface OrderRefundRepository extends JpaRepository<OrderRefund, String> {
    List<OrderRefund> findAllByOrderByCreatedAtDesc();
    List<OrderRefund> findAllByOwnerIgnoreCaseOrderByCreatedAtDesc(String owner);

    @Query("select coalesce(sum(r.amount), 0) from OrderRefund r where (:owner is null or r.owner = :owner) "
            + "and r.createdAt >= :start and r.createdAt < :end and r.status = :status")
    BigDecimal sumApprovedForDashboard(@Param("owner") String owner, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, @Param("status") String status);
}
