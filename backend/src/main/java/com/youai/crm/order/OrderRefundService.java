package com.youai.crm.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.common.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class OrderRefundService {

    private static final String PENDING = "待审核";
    private final OrderRefundRepository refundRepository;
    private final SalesOrderRepository orderRepository;
    private final AccessPolicy accessPolicy;

    public OrderRefundService(OrderRefundRepository refundRepository, SalesOrderRepository orderRepository, AccessPolicy accessPolicy) {
        this.refundRepository = refundRepository;
        this.orderRepository = orderRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<RefundResponse> list(Authentication authentication) {
        accessPolicy.scopedOwner(authentication); // also rejects direct calls without an identity
        return refundRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(refund -> accessPolicy.canAccessOwner(refund.getOwner(), authentication))
                .map(RefundResponse::from).toList();
    }

    public RefundResponse create(RefundRequest request, Authentication authentication) {
        SalesOrder order = orderRepository.findByOrderNo(request.orderNo().trim())
                .orElseThrow(() -> new NotFoundException("未找到订单：" + request.orderNo()));
        accessPolicy.requireOwner(order.getOwner(), authentication);
        BigDecimal amount = request.amount();
        if (amount.compareTo(order.getPaidAmount()) > 0) {
            throw new IllegalArgumentException("退款金额不能超过已回款金额");
        }
        OrderRefund refund = new OrderRefund();
        refund.setId("RF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT));
        refund.setOrderNo(order.getOrderNo());
        refund.setCustomerName(order.getCustomerName());
        refund.setAmount(amount);
        refund.setReason(request.reason().trim());
        refund.setApplicant(accessPolicy.currentDisplayName(authentication));
        refund.setOwner(order.getOwner());
        refund.setStatus(PENDING);
        refund.setCreatedAt(LocalDateTime.now());
        return RefundResponse.from(refundRepository.save(refund));
    }

    public RefundResponse review(String id, RefundReviewRequest request, Authentication authentication) {
        if (!accessPolicy.isAdmin(authentication)) throw new AccessDeniedException("只有管理员可以审核退款");
        OrderRefund refund = refundRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("未找到退款申请：" + id));
        if (!PENDING.equals(refund.getStatus())) throw new IllegalArgumentException("该退款申请已审核");
        String status = request.status().trim();
        if (!"已通过".equals(status) && !"已拒绝".equals(status)) {
            throw new IllegalArgumentException("审核结果只能是已通过或已拒绝");
        }
        refund.setStatus(status);
        refund.setReviewer(accessPolicy.currentDisplayName(authentication));
        refund.setReviewedAt(LocalDateTime.now());
        return RefundResponse.from(refundRepository.save(refund));
    }
}
