package com.youai.crm.order;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    Optional<SalesOrder> findByOrderNo(String orderNo);

    @Query("select coalesce(sum(o.amount), 0) from SalesOrder o where o.paymentStatus <> '已取消'")
    BigDecimal sumActiveOrderAmount();

    @Query("select coalesce(sum(o.paidAmount), 0) from SalesOrder o")
    BigDecimal sumPaidAmount();
}
