package com.youai.crm.invitation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InvitationRecordRepository extends JpaRepository<InvitationRecord, Long>, JpaSpecificationExecutor<InvitationRecord> {
    Optional<InvitationRecord> findByInvitationNo(String invitationNo);
}
