package com.youai.crm.communication;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id = :id")
    Optional<Conversation> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Conversation> findByCustomerNo(String customerNo);
    List<Conversation> findAllByOrderByLastMessageAtDesc();
}
