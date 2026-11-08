package com.youai.crm.communication;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<CrmMessage, Long> {
    List<CrmMessage> findByConversationIdOrderBySentAtAsc(Long conversationId);
    Optional<CrmMessage> findTopByConversationIdOrderBySentAtDesc(Long conversationId);
    // A current read also sees a concurrent request committed while waiting for the conversation lock.
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<CrmMessage> findByConversationIdAndRequestKey(Long conversationId, String requestKey);
    List<CrmMessage> findTop20ByDeliveryStatusOrderByIdAsc(String status);
    List<CrmMessage> findTop20ByDeliveryStatusInAndDeliveryUpdatedAtBeforeOrderByDeliveryUpdatedAtAsc(
            List<String> statuses, LocalDateTime cutoff);

    @Modifying
    @Query("update CrmMessage m set m.deliveryStatus = 'SENDING', m.deliveryUpdatedAt = :now where m.id = :id and m.deliveryStatus = 'QUEUED'")
    int claimSms(@Param("id") Long id,
            @Param("now") LocalDateTime now);

    @Modifying
    @Query("update CrmMessage m set m.deliveryStatus = 'UNKNOWN', m.deliveryError = '发送结果待确认', m.deliveryUpdatedAt = :now where m.deliveryStatus = 'SENDING' and m.deliveryUpdatedAt < :cutoff")
    int recoverSms(@Param("now") LocalDateTime now,
            @Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("update CrmMessage m set m.deliveryStatus = :status, m.gatewayMessageId = :providerId, m.deliveryError = :error, m.deliveryUpdatedAt = :now where m.id = :id and m.deliveryStatus = :expected")
    int completeSms(@Param("id") Long id,
            @Param("expected") String expected,
            @Param("status") String status,
            @Param("providerId") String providerId,
            @Param("error") String error,
            @Param("now") LocalDateTime now);

    long countByConversationIdAndDirectionAndReadFalse(Long conversationId, String direction);
}
