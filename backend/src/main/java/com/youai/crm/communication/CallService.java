package com.youai.crm.communication;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.customer.CustomerResponse;
import com.youai.crm.customer.Customer;
import com.youai.crm.customer.CustomerRepository;
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
    private final CustomerRepository customerRepository;
    private final AccessPolicy accessPolicy;
    private final com.youai.crm.customer.CustomerAccessPolicy customerAccess;

    public CallService(CallRecordRepository repository, CustomerService customerService,
            CustomerRepository customerRepository, AccessPolicy accessPolicy, com.youai.crm.customer.CustomerAccessPolicy customerAccess) {
        this.repository = repository;
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.accessPolicy = accessPolicy;
        this.customerAccess = customerAccess;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<CallResponse> list(String status, String keyword, String customerName, String direction, String agent,
            Pageable pageable, Authentication authentication) {
        accessPolicy.scopedOwner(authentication); // also rejects direct calls without an identity
        Long viewerId = accessPolicy.currentUserId(authentication);
        var spec = (org.springframework.data.jpa.domain.Specification<CallRecord>) (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.or(customerAccess.visibleReference(root.get("customerNo"), query, builder, authentication), builder.equal(root.get("agentUserId"), viewerId)));
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
        return responses(repository.findAll(spec, safe), authentication);
    }

    public Page<CallResponse> responses(Page<CallRecord> page, Authentication authentication) {
        return page.map(responseMapper(page.getContent(), authentication));
    }

    public CallResponse response(CallRecord call, Authentication authentication) {
        return responseMapper(java.util.List.of(call), authentication).apply(call);
    }

    private java.util.function.Function<CallRecord, CallResponse> responseMapper(
            java.util.List<CallRecord> calls, Authentication authentication) {
        boolean admin = accessPolicy.isAdmin(authentication);
        Long viewerId = accessPolicy.currentUserId(authentication);
        var owners = new java.util.HashMap<String, Long>();
        var customerNos = calls.stream().map(CallRecord::getCustomerNo).distinct().toList();
        if (!customerNos.isEmpty()) {
            // Only fetch the two scalar columns needed here, never full customer profiles/collections.
            customerRepository.findOwnershipByCustomerNos(customerNos)
                    .forEach(customer -> owners.put(customer.getCustomerNo(), customer.getOwnerId()));
        }
        return call -> {
            Long ownerId = owners.get(call.getCustomerNo());
            boolean following = ownerId != null && ownerId.equals(call.getAgentUserId());
            boolean canAccess = admin || (ownerId != null && ownerId.equals(viewerId));
            return CallResponse.from(call, following ? "FOLLOWING" : "TRANSFERRED", canAccess);
        };
    }

    public CallResponse create(CallRequest request, Authentication authentication) {
        CustomerResponse customer = customerService.find(request.customerId(), authentication);
        CallRecord call = new CallRecord();
        call.setCustomerNo(customer.id());
        call.setCustomerName(customer.name());
        call.setPhone(customer.phone());
        call.setOwner(customer.owner());
        call.setAgent(accessPolicy.currentDisplayName(authentication));
        call.setAgentUserId(accessPolicy.currentUserId(authentication));
        call.setDirection(request.direction().trim());
        call.setStatus(request.status().trim());
        call.setDurationSeconds(request.durationSeconds() == null ? 0 : request.durationSeconds());
        call.setNote(StringUtils.hasText(request.note()) ? request.note().trim() : "暂无沟通备注");
        call.setStartedAt(java.time.LocalDateTime.now());
        CallRecord saved = repository.save(call);
        Customer customerRecord = customerRepository.findByCustomerNo(customer.id()).orElse(null);
        if (customerRecord != null) {
            customerRecord.setLastContactAt(saved.getStartedAt());
            customerRepository.save(customerRecord);
        }
        return response(saved, authentication);
    }
}
