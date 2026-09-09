package com.youke.crm.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.youke.crm.common.NotFoundException;
import com.youke.crm.account.AccessPolicy;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Sort;
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
    public List<OrderResponse> list(String keyword, String paymentStatus, String serviceStatus, Authentication authentication) {
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        String scopedOwner = accessPolicy.scopedOwner(authentication);
        return repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(order -> !StringUtils.hasText(scopedOwner) || order.getOwner().equalsIgnoreCase(scopedOwner))
                .filter(order -> normalizedKeyword == null
                        || order.getOrderNo().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || order.getCustomerName().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .filter(order -> !StringUtils.hasText(paymentStatus) || order.getPaymentStatus().equals(paymentStatus.trim()))
                .filter(order -> !StringUtils.hasText(serviceStatus) || order.getServiceStatus().equals(serviceStatus.trim()))
                .map(OrderResponse::from)
                .toList();
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
