package com.youai.crm.communication;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageTemplateRepository extends JpaRepository<MessageTemplate, String> {
    List<MessageTemplate> findAllByOrderByUpdatedAtDesc();
}
