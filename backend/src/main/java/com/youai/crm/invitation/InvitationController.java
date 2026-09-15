package com.youai.crm.invitation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {
    private final InvitationService service;
    public InvitationController(InvitationService service) { this.service = service; }
    @GetMapping public List<InvitationResponse> list(Authentication auth) { return service.list(auth); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public InvitationResponse create(@Valid @RequestBody InvitationRequest request, Authentication auth) { return service.create(request, auth); }
    @PatchMapping("/{id}/arrival") public InvitationResponse arrival(@PathVariable String id, @RequestBody(required = false) Map<String, LocalDateTime> body, Authentication auth) {
        return service.markArrival(id, body == null ? null : body.get("arrivalAt"), auth);
    }
}
