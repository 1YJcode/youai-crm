package com.youai.crm.communication;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@RestController
@RequestMapping("/api/calls")
public class CallController {

    private final CallService service;

    public CallController(CallService service) {
        this.service = service;
    }

    @GetMapping
    public Page<CallResponse> list(@RequestParam(required = false) String status,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String customerName,
                                   @RequestParam(required = false) String direction,
                                   @RequestParam(required = false) String agent,
                                   @PageableDefault(size = 20) Pageable pageable,
                                   Authentication authentication) {
        return service.list(status, keyword, customerName, direction, agent, pageable, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CallResponse create(@Valid @RequestBody CallRequest request, Authentication authentication) {
        return service.create(request, authentication);
    }
}
