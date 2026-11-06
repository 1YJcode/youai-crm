package com.youai.crm.order;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from SalesOrder o where o.orderNo = :orderNo")
    Optional<SalesOrder> lockByOrderNo(@Param("orderNo") String orderNo);

    Optional<SalesOrder> findByOrderNo(String orderNo);

    @Query("select distinct o.ownerId, o.owner, o.paymentStatus, o.serviceStatus from SalesOrder o where (:admin=true or o.ownerId=:userId or o.confirmationSalesId=:userId)")
    List<Object[]> visibleFilterValues(@Param("admin") boolean admin, @Param("userId") Long userId);

    @Query("select distinct i.inviter from InvitationRecord i, SalesOrder o where i.relatedOrderNo=o.orderNo "
            + "and i.id=(select max(i2.id) from InvitationRecord i2 where i2.relatedOrderNo=o.orderNo) "
            + "and (:admin=true or o.ownerId=:userId or o.confirmationSalesId=:userId) order by i.inviter")
    List<String> visibleInviters(@Param("admin") boolean admin, @Param("userId") Long userId);

    @Query("select coalesce(sum(o.amount), 0) from SalesOrder o where o.paymentStatus <> '已取消'")
    BigDecimal sumActiveOrderAmount();

    @Query("select coalesce(sum(o.paidAmount), 0) from SalesOrder o")
    BigDecimal sumPaidAmount();

    @Query("select coalesce(sum(o.amount), 0) from SalesOrder o where (:owner is null or o.owner = :owner) "
            + "and o.createdAt >= :start and o.createdAt < :end and o.paymentStatus <> :cancelled")
    BigDecimal sumActiveAmountForDashboard(@Param("owner") String owner, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, @Param("cancelled") String cancelled);

    @Query("select coalesce(sum(o.paidAmount), 0) from SalesOrder o where (:owner is null or o.owner = :owner) "
            + "and o.createdAt >= :start and o.createdAt < :end")
    BigDecimal sumPaidForDashboard(@Param("owner") String owner, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("select count(o) from SalesOrder o where (:owner is null or o.owner = :owner) "
            + "and o.createdAt >= :start and o.createdAt < :end and o.serviceStatus = :status")
    long countServiceStatusForDashboard(@Param("owner") String owner, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, @Param("status") String status);

    @Query("select count(o) from SalesOrder o where (:owner is null or o.owner = :owner) "
            + "and o.createdAt >= :start and o.createdAt < :end and o.serviceStatus in :statuses")
    long countServiceStatusesForDashboard(@Param("owner") String owner, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, @Param("statuses") List<String> statuses);

    @Query("select o.owner, coalesce(sum(case when o.paymentStatus <> :cancelled then o.amount else 0 end), 0), "
            + "coalesce(sum(o.paidAmount), 0), count(distinct case when o.paidAmount > 0 then o.customerName else null end) "
            + "from SalesOrder o where (:owner is null or o.owner = :owner) and o.createdAt >= :start and o.createdAt < :end "
            + "group by o.owner")
    List<Object[]> aggregateSalesRanking(@Param("owner") String owner, @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, @Param("cancelled") String cancelled);
}
