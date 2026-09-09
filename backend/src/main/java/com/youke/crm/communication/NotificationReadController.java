package com.youke.crm.communication;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/read")
public class NotificationReadController {

    private final NotificationReadService service;

    public NotificationReadController(NotificationReadService service) { this.service = service; }

    @GetMapping
    public List<String> list(Authentication authentication) { return service.list(authentication); }

    @PatchMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@Valid @RequestBody NotificationReadRequest request, Authentication authentication) {
        service.update(request, authentication);
    }
}
