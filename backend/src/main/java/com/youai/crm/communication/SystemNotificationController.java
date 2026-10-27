package com.youai.crm.communication;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system-notifications")
public class SystemNotificationController {
    private final SystemNotificationRepository repository;
    private final NotificationReadRepository readRepository;
    private final CallFollowupWorkService work;
    private final com.youai.crm.customer.CustomerAccessPolicy access;
    public SystemNotificationController(SystemNotificationRepository repository, NotificationReadRepository readRepository,CallFollowupWorkService work,com.youai.crm.customer.CustomerAccessPolicy access) {
        this.repository = repository; this.readRepository = readRepository;this.work=work;this.access=access;
    }
    @GetMapping
    public List<SystemNotificationResponse> list(Authentication authentication) {
        String username = authentication == null ? "system" : authentication.getName();
        var read = readRepository.findAllByUsername(username).stream().map(NotificationRead::getNotificationId).collect(java.util.stream.Collectors.toSet());
        return repository.findAllByUsernameOrderByCreatedAtDesc(username).stream()
                .filter(n->n.getNotificationKind()==null||n.getCustomerNo()==null||access.canAccess(n.getCustomerNo(),authentication))
                .map(n -> new SystemNotificationResponse(n.getNotificationKey(), n.getTitle(), n.getContent(), n.getCustomerNo(), n.getCreatedAt(), read.contains(n.getNotificationKey()),
                        n.getCallId(),n.getCallId(),n.getNotificationKind(),n.getCallId()!=null&&work.completed(n.getCallId())))
                .toList();
    }
}
