package com.youke.crm.order;

import java.util.List;

import java.math.BigDecimal;
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
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrderResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String serviceStatus,
            Authentication authentication) {
        return service.list(keyword, paymentStatus, serviceStatus, authentication);
    }

    @GetMapping("/{orderNo}")
    public OrderResponse find(@PathVariable String orderNo, Authentication authentication) {
        return service.find(orderNo, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody OrderRequest request, Authentication authentication) {
        return service.create(request, authentication);
    }

    @PutMapping("/{orderNo}")
    public OrderResponse update(@PathVariable String orderNo, @Valid @RequestBody OrderRequest request, Authentication authentication) {
        return service.update(orderNo, request, authentication);
    }

    @PatchMapping("/{orderNo}/payment")
    public OrderResponse updatePayment(@PathVariable String orderNo, @RequestBody Map<String, BigDecimal> body, Authentication authentication) {
        return service.updatePayment(orderNo, body.get("paid"), authentication);
    }

    @PatchMapping("/{orderNo}/service")
    public OrderResponse updateService(@PathVariable String orderNo, @RequestBody Map<String, String> body, Authentication authentication) {
        return service.updateService(orderNo, body.get("status"), authentication);
    }

    @PatchMapping("/{orderNo}/confirmation")
    public OrderResponse updateConfirmation(@PathVariable String orderNo, @RequestBody Map<String, Boolean> body, Authentication authentication) {
        return service.updateConfirmation(orderNo, Boolean.TRUE.equals(body.get("confirmed")), authentication);
    }
}
