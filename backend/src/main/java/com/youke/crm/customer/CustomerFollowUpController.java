package com.youke.crm.customer;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/{customerNo}/follow-ups")
public class CustomerFollowUpController {

    private final CustomerFollowUpService service;

    public CustomerFollowUpController(CustomerFollowUpService service) {
        this.service = service;
    }

    @GetMapping
    public List<FollowUpRecordResponse> list(
            @PathVariable String customerNo,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            Authentication authentication) {
        return service.list(customerNo, type, keyword, from, to, authentication);
    }

    @GetMapping("/annotations")
    public List<FollowUpAnnotationResponse> annotations(@PathVariable String customerNo, Authentication authentication) {
        return service.annotations(customerNo, authentication);
    }

    @PatchMapping("/annotations")
    @ResponseStatus(HttpStatus.OK)
    public FollowUpAnnotationResponse saveAnnotation(
            @PathVariable String customerNo,
            @Valid @RequestBody FollowUpAnnotationRequest request,
            Authentication authentication) {
        return service.saveAnnotation(customerNo, request, authentication);
    }
}
