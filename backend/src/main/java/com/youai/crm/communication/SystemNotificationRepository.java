package com.youai.crm.communication;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemNotificationRepository extends JpaRepository<SystemNotification, Long> {
    List<SystemNotification> findAllByUsernameOrderByCreatedAtDesc(String username);
    Optional<SystemNotification> findByNotificationKey(String notificationKey);
}
