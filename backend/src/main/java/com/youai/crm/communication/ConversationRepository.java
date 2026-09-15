package com.youai.crm.communication;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByCustomerNo(String customerNo);
    List<Conversation> findAllByOrderByLastMessageAtDesc();
}
