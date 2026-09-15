package com.youai.crm.communication;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {
    List<NotificationRead> findAllByUsername(String username);
    Optional<NotificationRead> findByUsernameAndNotificationId(String username, String notificationId);
}
