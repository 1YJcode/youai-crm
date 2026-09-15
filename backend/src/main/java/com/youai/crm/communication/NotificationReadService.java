package com.youai.crm.communication;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class NotificationReadService {

    private final NotificationReadRepository repository;

    public NotificationReadService(NotificationReadRepository repository) { this.repository = repository; }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<String> list(Authentication authentication) {
        String username = username(authentication);
        return repository.findAllByUsername(username).stream().map(NotificationRead::getNotificationId).toList();
    }

    public void update(NotificationReadRequest request, Authentication authentication) {
        String username = username(authentication);
        for (String notificationId : request.notificationIds()) {
            repository.findByUsernameAndNotificationId(username, notificationId).ifPresentOrElse(existing -> {
                if (request.read()) {
                    existing.setReadAt(LocalDateTime.now());
                    repository.save(existing);
                } else {
                    repository.delete(existing);
                }
            }, () -> {
                if (request.read()) {
                    NotificationRead created = new NotificationRead();
                    created.setUsername(username);
                    created.setNotificationId(notificationId);
                    created.setReadAt(LocalDateTime.now());
                    repository.save(created);
                }
            });
        }
    }

    private String username(Authentication authentication) { return authentication == null ? "system" : authentication.getName(); }
}
