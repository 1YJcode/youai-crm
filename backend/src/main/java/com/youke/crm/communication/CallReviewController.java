package com.youke.crm.communication;

import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/call-reviews")
public class CallReviewController {

    private final CallReviewService service;

    public CallReviewController(CallReviewService service) { this.service = service; }

    @GetMapping
    public Map<Long, Boolean> list(Authentication authentication) { return service.list(authentication); }

    @PatchMapping("/{callId}")
    public ResponseEntity<Map<String, Boolean>> update(@PathVariable Long callId, @Valid @RequestBody CallReviewRequest request, Authentication authentication) {
        return ResponseEntity.ok(Map.of("reviewed", service.update(callId, request, authentication)));
    }
}
