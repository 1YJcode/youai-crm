package com.youai.crm.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.youai.crm.common.NotFoundException;
import com.youai.crm.account.AccessPolicy;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class OrderService {

    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss", Locale.ROOT);
    private final SalesOrderRepository repository;
    private final AccessPolicy accessPolicy;

    public OrderService(SalesOrderRepository repository, AccessPolicy accessPolicy) {
        this.repository = repository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<OrderResponse> list(String keyword, String paymentStatus, String serviceStatus, Pageable pageable, Authentication authentication) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        accessPolicy.scopedOwner(authentication); // also rejects direct calls without an identity
        boolean admin = accessPolicy.isAdmin(authentication);
        var spec = (org.springframework.data.jpa.domain.Specification<SalesOrder>) (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!admin) predicates.add(builder.equal(root.get("owner"), accessPolicy.currentOwner(authentication)));
            if (normalizedKeyword != null) {
                String like = "%" + normalizedKeyword + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("orderNo")), like), builder.like(builder.lower(root.get("customerName")), like)));
            }
            if (StringUtils.hasText(paymentStatus)) predicates.add(builder.equal(root.get("paymentStatus"), paymentStatus.trim()));
            if (StringUtils.hasText(serviceStatus)) predicates.add(builder.equal(root.get("serviceStatus"), serviceStatus.trim()));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Pageable requested = pageable == null ? PageRequest.of(0, 20) : pageable;
        Pageable safe = PageRequest.of(Math.max(0, requested.getPageNumber()), Math.min(Math.max(requested.getPageSize(), 1), 100),
                requested.getSort().isSorted() ? requested.getSort() : Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findAll(spec, safe).map(OrderResponse::from);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public OrderResponse find(String orderNo, Authentication authentication) {
        return OrderResponse.from(get(orderNo, authentication));
    }

    public OrderResponse create(OrderRequest request, Authentication authentication) {
        SalesOrder order = new SalesOrder();
        apply(order, request, true);
        if (!accessPolicy.isAdmin(authentication)) order.setOwner(accessPolicy.currentOwner(authentication));
        return OrderResponse.from(repository.save(order));
    }

    public OrderResponse update(String orderNo, OrderRequest request, Authentication authentication) {
        SalesOrder order = get(orderNo, authentication);
        apply(order, request, false);
        if (!accessPolicy.isAdmin(authentication)) order.setOwner(accessPolicy.currentOwner(authentication));
        return OrderResponse.from(repository.save(order));
    }

    public OrderResponse updatePayment(String orderNo, BigDecimal paid, Authentication authentication) {
        SalesOrder order = get(orderNo, authentication);
        validatePaid(order.getAmount(), paid);
        order.setPaidAmount(paid);
        order.setPaymentStatus(paymentStatus(order.getAmount(), paid));
        return OrderResponse.from(repository.save(order));
    }

    public OrderResponse updateService(String orderNo, String serviceStatus, Authentication authentication) {
        if (!StringUtils.hasText(serviceStatus)) throw new IllegalArgumentException("服务状态不能为空");
        SalesOrder order = get(orderNo, authentication);
        order.setServiceStatus(serviceStatus.trim());
        return OrderResponse.from(repository.save(order));
    }

    public OrderResponse updateConfirmation(String orderNo, boolean confirmed, Authentication authentication) {
        SalesOrder order = get(orderNo, authentication);
        if (confirmed && (order.getPaidAmount() == null || order.getPaidAmount().signum() <= 0)) {
            throw new IllegalArgumentException("订单尚未回款，暂不能确认业绩");
        }
        order.setPerformanceConfirmed(confirmed);
        return OrderResponse.from(repository.save(order));
    }

    private void apply(SalesOrder order, OrderRequest request, boolean creating) {
        BigDecimal amount = request.amount();
        BigDecimal paid = request.paid() == null ? BigDecimal.ZERO : request.paid();
        validatePaid(amount, paid);
        if (creating) {
            String orderNo = StringUtils.hasText(request.orderNo()) ? request.orderNo().trim() : nextOrderNo();
            if (repository.findByOrderNo(orderNo).isPresent()) throw new IllegalArgumentException("订单编号已存在");
            order.setOrderNo(orderNo);
            order.setCreatedAt(LocalDateTime.now());
        }
        order.setCustomerName(request.customer().trim());
        order.setProduct(request.product().trim());
        order.setAmount(amount);
        order.setPaidAmount(paid);
        order.setPaymentStatus(StringUtils.hasText(request.status()) ? request.status().trim() : paymentStatus(amount, paid));
        order.setServiceStatus(StringUtils.hasText(request.service()) ? request.service().trim() : "未开始");
        order.setOwner(request.owner().trim());
    }

    private void validatePaid(BigDecimal amount, BigDecimal paid) {
        if (paid == null || paid.signum() < 0) throw new IllegalArgumentException("已付金额不能小于 0");
        if (amount == null || paid.compareTo(amount) > 0) throw new IllegalArgumentException("已付金额不能超过订单金额");
    }

    private String paymentStatus(BigDecimal amount, BigDecimal paid) {
        if (paid.signum() == 0) return "待支付";
        if (paid.compareTo(amount) >= 0) return "已支付";
        return "部分支付";
    }

    private String nextOrderNo() {
        String prefix = "SO" + LocalDateTime.now().format(ORDER_DATE);
        String candidate = prefix + "01";
        while (repository.findByOrderNo(candidate).isPresent()) {
            candidate = prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase(Locale.ROOT);
        }
        return candidate;
    }

    private SalesOrder get(String orderNo, Authentication authentication) {
        SalesOrder order = repository.findByOrderNo(orderNo)
                .orElseThrow(() -> new NotFoundException("未找到订单：" + orderNo));
        accessPolicy.requireOwner(order.getOwner(), authentication);
        return order;
    }
}
