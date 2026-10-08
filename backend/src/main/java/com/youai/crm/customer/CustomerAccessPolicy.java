package com.youai.crm.customer;

import com.youai.crm.account.AccessPolicy;
import jakarta.persistence.criteria.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** Shared authorization for records referring to a customer, including calls and messages. */
@Component
public class CustomerAccessPolicy {
    private final CustomerRepository customers;
    private final AccessPolicy access;

    public CustomerAccessPolicy(CustomerRepository customers, AccessPolicy access) {
        this.customers = customers;
        this.access = access;
    }

    public boolean canAccess(String customerNo, Authentication authentication) {
        if (access.isAdmin(authentication)) return true;
        return customerNo != null && customers.findByCustomerNo(customerNo)
                .map(customer -> access.canAccessUserId(customer.getOwnerId(), authentication)).orElse(false);
    }

    public void requireAccess(String customerNo, Authentication authentication) {
        if (!canAccess(customerNo, authentication)) throw new AccessDeniedException("只能操作本人负责的客户记录");
    }

    public Predicate visibleReference(Expression<String> customerNo, CriteriaQuery<?> query,
            CriteriaBuilder builder, Authentication authentication) {
        if (access.isAdmin(authentication)) return builder.conjunction();
        Subquery<String> visible = query.subquery(String.class);
        Root<Customer> customer = visible.from(Customer.class);
        visible.select(customer.get("customerNo"))
                .where(builder.equal(customer.get("ownerId"), access.currentUserId(authentication)));
        return customerNo.in(visible);
    }
}
