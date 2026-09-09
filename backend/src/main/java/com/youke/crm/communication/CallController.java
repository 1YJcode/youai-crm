package com.youke.crm.communication;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calls")
public class CallController {

    private final CallService service;

    public CallController(CallService service) {
        this.service = service;
    }

    @GetMapping
    public List<CallResponse> list(@RequestParam(required = false) String status, Authentication authentication) {
        return service.list(status, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CallResponse create(@Valid @RequestBody CallRequest request, Authentication authentication) {
        return service.create(request, authentication);
    }
}
