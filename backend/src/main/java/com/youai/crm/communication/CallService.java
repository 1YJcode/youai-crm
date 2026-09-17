package com.youai.crm.communication;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.customer.CustomerResponse;
import com.youai.crm.customer.CustomerService;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class CallService {

    private final CallRecordRepository repository;
    private final CustomerService customerService;
    private final AccessPolicy accessPolicy;

    public CallService(CallRecordRepository repository, CustomerService customerService, AccessPolicy accessPolicy) {
        this.repository = repository;
        this.customerService = customerService;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<CallResponse> list(String status, String keyword, String customerName, String direction, String agent,
            Pageable pageable, Authentication authentication) {
        accessPolicy.scopedOwner(authentication); // also rejects direct calls without an identity
        boolean admin = accessPolicy.isAdmin(authentication);
        var spec = (org.springframework.data.jpa.domain.Specification<CallRecord>) (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!admin) predicates.add(builder.equal(root.get("owner"), accessPolicy.currentOwner(authentication)));
            if (StringUtils.hasText(status)) predicates.add(builder.equal(root.get("status"), status.trim()));
            if (StringUtils.hasText(direction)) predicates.add(builder.equal(root.get("direction"), direction.trim()));
            if (StringUtils.hasText(agent)) predicates.add(builder.equal(root.get("agent"), agent.trim()));
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("customerNo")), like), builder.like(builder.lower(root.get("phone")), like)));
            }
            if (StringUtils.hasText(customerName)) predicates.add(builder.like(builder.lower(root.get("customerName")), "%" + customerName.trim().toLowerCase() + "%"));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Pageable requested = pageable == null ? PageRequest.of(0, 20) : pageable;
        Pageable safe = PageRequest.of(Math.max(0, requested.getPageNumber()), Math.min(Math.max(requested.getPageSize(), 1), 100),
                requested.getSort().isSorted() ? requested.getSort() : Sort.by(Sort.Direction.DESC, "startedAt"));
        return repository.findAll(spec, safe).map(CallResponse::from);
    }

    public CallResponse create(CallRequest request, Authentication authentication) {
        CustomerResponse customer = customerService.find(request.customerId(), authentication);
        CallRecord call = new CallRecord();
        call.setCustomerNo(customer.id());
        call.setCustomerName(customer.name());
        call.setPhone(customer.phone());
        call.setOwner(customer.owner());
        call.setAgent(accessPolicy.currentDisplayName(authentication));
        call.setDirection(request.direction().trim());
        call.setStatus(request.status().trim());
        call.setDurationSeconds(request.durationSeconds() == null ? 0 : request.durationSeconds());
        call.setNote(StringUtils.hasText(request.note()) ? request.note().trim() : "暂无沟通备注");
        return CallResponse.from(repository.save(call));
    }
}
