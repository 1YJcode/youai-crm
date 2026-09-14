package com.youke.crm.communication;

import java.util.List;

import com.youke.crm.account.AccessPolicy;
import com.youke.crm.customer.CustomerResponse;
import com.youke.crm.customer.CustomerService;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
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
    public List<CallResponse> list(String status, Authentication authentication) {
        accessPolicy.scopedOwner(authentication); // also rejects direct calls without an identity
        return repository.findAllByOrderByStartedAtDesc().stream()
                .filter(call -> accessPolicy.canAccessOwner(call.getOwner(), authentication))
                .filter(call -> !StringUtils.hasText(status) || call.getStatus().equals(status.trim()))
                .map(CallResponse::from)
                .toList();
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
