package com.youai.crm.customer;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService service;

    public CustomerController(CustomerService service) {
        this.service = service;
    }

    @GetMapping
    public List<CustomerResponse> search(
            Authentication authentication,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String stage,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) Boolean inPool,
            @RequestParam Map<String, String> advanced) {
        return service.search(keyword, stage, level, owner, tag, inPool, advanced, authentication);
    }

    @GetMapping("/pool")
    public List<CustomerResponse> pool(Authentication authentication) {
        return service.pool(authentication);
    }

    @GetMapping("/tags")
    public Map<String, Long> tags(Authentication authentication) {
        return service.tags(authentication);
    }

    @GetMapping("/{customerNo}")
    public CustomerResponse find(@PathVariable String customerNo, Authentication authentication) {
        return service.find(customerNo, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CustomerRequest request, Authentication authentication) {
        return service.create(request, authentication, "手动录入");
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse importCustomer(@Valid @RequestBody CustomerRequest request, Authentication authentication) {
        return service.create(request, authentication, "批量导入");
    }

    @GetMapping("/{customerNo}/registration-events")
    public List<CustomerRegistrationEventResponse> registrationEvents(
            @PathVariable String customerNo, Authentication authentication) {
        return service.registrationEvents(customerNo, authentication);
    }

    @GetMapping("/{customerNo}/assignment-events")
    public List<CustomerAssignmentEventResponse> assignmentEvents(
            @PathVariable String customerNo, Authentication authentication) {
        return service.assignmentEvents(customerNo, authentication);
    }

    @PutMapping("/{customerNo}")
    public CustomerResponse update(
            @PathVariable String customerNo,
            @Valid @RequestBody CustomerRequest request,
            Authentication authentication) {
        return service.update(customerNo, request, authentication);
    }

    @PatchMapping("/{customerNo}/stage")
    public CustomerResponse updateStage(
            @PathVariable String customerNo,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        return service.updateStage(customerNo, body.get("stage"), authentication);
    }

    @PatchMapping("/{customerNo}/pool")
    public CustomerResponse updatePool(
            @PathVariable String customerNo,
            @RequestBody Map<String, Boolean> body,
            Authentication authentication) {
        return service.updatePool(customerNo, Boolean.TRUE.equals(body.get("inPool")), authentication);
    }

    @PatchMapping("/{customerNo}/assignment")
    public CustomerResponse assign(
        @PathVariable String customerNo,
            @Valid @RequestBody CustomerAssignmentRequest request,
            Authentication authentication) {
        return service.assign(customerNo, request, authentication);
    }
}
