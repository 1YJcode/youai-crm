package com.youai.crm.customer;

import com.youai.crm.account.AccessPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/customers")
public class CustomerPhoneCheckController {
    private final CustomerRepository repository;
    private final AccessPolicy accessPolicy;

    public CustomerPhoneCheckController(CustomerRepository repository, AccessPolicy accessPolicy) {
        this.repository = repository;
        this.accessPolicy = accessPolicy;
    }

    public record PhoneCheckRequest(String phone, String customerId) {}
    public record PhoneCheckResponse(boolean duplicate) {}

    @PostMapping("/phone-check")
    public PhoneCheckResponse check(@RequestBody PhoneCheckRequest request, Authentication authentication) {
        accessPolicy.currentUserId(authentication);
        String phone = request.phone() == null ? "" : request.phone().trim();
        if (!phone.matches("1[3-9][0-9]{9}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请输入正确的11位手机号码");
        }
        // An exclusion is accepted only for a record this user may edit.
        if (request.customerId() != null && !request.customerId().isBlank()) {
            Customer current = repository.findByCustomerNo(request.customerId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            accessPolicy.requireUserId(current.getOwnerId(), authentication);
        }
        boolean duplicate = repository.findByPhone(phone)
                .filter(customer -> !customer.getCustomerNo().equals(request.customerId())).isPresent();
        // Never expose another customer's identity, owner or contact details.
        return new PhoneCheckResponse(duplicate);
    }
}
