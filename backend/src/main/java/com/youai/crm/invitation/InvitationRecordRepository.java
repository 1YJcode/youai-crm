package com.youai.crm.invitation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface InvitationRecordRepository extends JpaRepository<InvitationRecord, Long>, JpaSpecificationExecutor<InvitationRecord> {
    Optional<InvitationRecord> findByInvitationNo(String invitationNo);

    @Query("select count(distinct i.customerNo) from InvitationRecord i where (:owner is null or i.inviter = :owner) "
            + "and (:store is null or :store = '' or i.storeName = :store) and i.scheduledAt >= :start and i.scheduledAt < :end "
            + "and i.arrivalAt is not null")
    long countArrivedForDashboard(@Param("owner") String owner, @Param("store") String store,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select i.inviter, i.department from InvitationRecord i "
            + "where (:owner is null or i.inviter = :owner) and (:store is null or :store = '' or i.storeName = :store) "
            + "and i.scheduledAt >= :start and i.scheduledAt < :end group by i.inviter, i.department")
    List<Object[]> aggregateEmployeesForDashboard(@Param("owner") String owner, @Param("store") String store,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("select i.inviter, i.department, count(distinct i.customerNo) from InvitationRecord i "
            + "where (:owner is null or i.inviter = :owner) and (:store is null or :store = '' or i.storeName = :store) "
            + "and i.scheduledAt >= :start and i.scheduledAt < :end and i.arrivalAt is not null group by i.inviter, i.department")
    List<Object[]> aggregateArrivalsForDashboard(@Param("owner") String owner, @Param("store") String store,
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
