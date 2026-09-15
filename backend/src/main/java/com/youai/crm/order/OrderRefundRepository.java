package com.youai.crm.order;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRefundRepository extends JpaRepository<OrderRefund, String> {
    List<OrderRefund> findAllByOrderByCreatedAtDesc();
    List<OrderRefund> findAllByOwnerIgnoreCaseOrderByCreatedAtDesc(String owner);
}
