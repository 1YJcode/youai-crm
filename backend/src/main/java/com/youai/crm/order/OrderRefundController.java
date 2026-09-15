package com.youai.crm.order;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order-refunds")
public class OrderRefundController {

    private final OrderRefundService service;

    public OrderRefundController(OrderRefundService service) {
        this.service = service;
    }

    @GetMapping
    public List<RefundResponse> list(Authentication authentication) {
        return service.list(authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RefundResponse create(@Valid @RequestBody RefundRequest request, Authentication authentication) {
        return service.create(request, authentication);
    }

    @PatchMapping("/{id}/review")
    public RefundResponse review(@PathVariable String id, @Valid @RequestBody RefundReviewRequest request, Authentication authentication) {
        return service.review(id, request, authentication);
    }
}
