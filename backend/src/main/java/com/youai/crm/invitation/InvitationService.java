package com.youai.crm.invitation;

import java.time.LocalDateTime;
import java.util.List;
import com.youai.crm.account.AccessPolicy;
import com.youai.crm.common.NotFoundException;
import com.youai.crm.customer.Customer;
import com.youai.crm.customer.CustomerRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class InvitationService {
    private final InvitationRecordRepository records;
    private final CustomerRepository customers;
    private final AccessPolicy accessPolicy;
    public InvitationService(InvitationRecordRepository records, CustomerRepository customers, AccessPolicy accessPolicy) {
        this.records = records; this.customers = customers; this.accessPolicy = accessPolicy;
    }
    public List<InvitationResponse> list(Authentication auth) {
        accessPolicy.scopedOwner(auth); // also rejects direct calls without an identity
        return records.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
            .filter(row -> accessPolicy.canAccessOwner(row.getInviter(), auth))
            .map(this::response).toList();
    }
    @Transactional
    public InvitationResponse create(InvitationRequest request, Authentication auth) {
        Customer customer = customers.findByCustomerNo(request.customerId()).orElseThrow(() -> new NotFoundException("未找到客户：" + request.customerId()));
        accessPolicy.requireOwner(customer.getOwner(), auth);
        InvitationRecord row = new InvitationRecord();
        row.setInvitationNo("YQ" + System.currentTimeMillis()); row.setCustomerNo(customer.getCustomerNo());
        row.setInviter(customer.getOwner()); row.setDepartment("销售部"); row.setInvitationMethod(request.invitationMethod().trim());
        row.setStoreName(request.storeName().trim()); row.setScheduledAt(request.scheduledAt()); row.setArrivalStatus("待到店");
        row.setMaritalStatus(text(customer.getMaritalStatus(), "未填写")); row.setAnnualIncome(text(customer.getAnnualIncome(), "未填写"));
        row.setSource(customer.getSource()); row.setRemark(text(request.remark(), "—"));
        return response(records.save(row));
    }
    @Transactional
    public InvitationResponse markArrival(String no, LocalDateTime at, Authentication auth) {
        InvitationRecord row = records.findByInvitationNo(no).orElseThrow(() -> new NotFoundException("未找到邀约记录：" + no));
        accessPolicy.requireOwner(row.getInviter(), auth); row.setArrivalAt(at == null ? LocalDateTime.now() : at); row.setArrivalStatus("会员已到本店1次");
        return response(records.save(row));
    }
    private InvitationResponse response(InvitationRecord row) {
        Customer c = customers.findByCustomerNo(row.getCustomerNo()).orElseThrow(() -> new NotFoundException("邀约关联客户不存在"));
        String year = c.getBirthday() != null && c.getBirthday().length() >= 4 ? c.getBirthday().substring(0, 4) : "—";
        return new InvitationResponse(row.getInvitationNo(), c.getCustomerNo(), c.getName(), text(c.getGender(), "—"), year, row.getInviter(), row.getDepartment(), row.getInvitationMethod(), row.getStoreName(), row.getCreatedAt(), row.getScheduledAt(), row.getArrivalAt(), row.getArrivalStatus(), row.getMaritalStatus(), row.getAnnualIncome(), row.getSource(), row.getReferrer(), row.getRelatedOrderNo(), row.getRemark());
    }
    private String text(String value, String fallback) { return StringUtils.hasText(value) ? value : fallback; }
}
