package com.youke.crm.communication;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<CrmMessage, Long> {
    List<CrmMessage> findByConversationIdOrderBySentAtAsc(Long conversationId);
    Optional<CrmMessage> findTopByConversationIdOrderBySentAtDesc(Long conversationId);
    long countByConversationIdAndDirectionAndReadFalse(Long conversationId, String direction);
}
