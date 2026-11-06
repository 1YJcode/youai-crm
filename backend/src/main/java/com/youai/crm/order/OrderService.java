package com.youai.crm.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

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

    private static final java.security.SecureRandom ORDER_RANDOM = new java.security.SecureRandom();
    private final com.youai.crm.account.CrmUserRepository users;
    private final SalesOrderRepository repository;
    private final com.youai.crm.invitation.InvitationRecordRepository invitations;
    private final SalesContractRepository contracts;
    private final AccessPolicy accessPolicy;
    private final com.youai.crm.account.UserIdentityResolver identities;

    private final com.youai.crm.customer.CustomerRepository customers;
    private final com.youai.crm.customer.CustomerQueryService customerQueries;

    public OrderService(SalesContractRepository contracts, SalesOrderRepository repository, AccessPolicy accessPolicy, com.youai.crm.account.UserIdentityResolver identities,
            com.youai.crm.customer.CustomerRepository customers, com.youai.crm.customer.CustomerQueryService customerQueries, com.youai.crm.account.CrmUserRepository users, com.youai.crm.invitation.InvitationRecordRepository invitations) {
        this.invitations = invitations;
        this.contracts = contracts;
        this.users = users;
        this.repository = repository;
        this.accessPolicy = accessPolicy;
        this.identities = identities;
        this.customers = customers;
        this.customerQueries = customerQueries;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<OrderResponse> list(String keyword, String paymentStatus, String serviceStatus, Pageable pageable, Authentication authentication) {
        return list(keyword, paymentStatus, serviceStatus, null, null, null, pageable, authentication);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<OrderResponse> list(String keyword, String paymentStatus, String serviceStatus,
            java.time.LocalDate createdFrom, java.time.LocalDate createdTo, String customerId, Pageable pageable, Authentication authentication) {
        return list(keyword, paymentStatus, serviceStatus, createdFrom, createdTo, customerId, null, null, pageable, authentication);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<OrderResponse> list(String keyword, String paymentStatus, String serviceStatus,
            java.time.LocalDate createdFrom, java.time.LocalDate createdTo, String customerId,
            Long ownerId, String inviter, Pageable pageable, Authentication authentication) {
        if (createdFrom != null && createdTo != null && createdTo.isBefore(createdFrom))
            throw new IllegalArgumentException("结束日期不能早于开始日期");
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        accessPolicy.scopedOwner(authentication); // also rejects direct calls without an identity
        boolean admin = accessPolicy.isAdmin(authentication);
        var spec = (org.springframework.data.jpa.domain.Specification<SalesOrder>) (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (!admin) predicates.add(builder.or(builder.equal(root.get("ownerId"), accessPolicy.currentUserId(authentication)), builder.equal(root.get("confirmationSalesId"), accessPolicy.currentUserId(authentication))));
            if (normalizedKeyword != null) {
                String like = "%" + normalizedKeyword + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("orderNo")), like), builder.like(builder.lower(root.get("customerName")), like)));
            }
            if (StringUtils.hasText(customerId)) predicates.add(builder.equal(root.get("customerNo"), customerId.trim()));
            if (createdFrom != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom.atStartOfDay()));
            if (createdTo != null) predicates.add(builder.lessThan(root.get("createdAt"), createdTo.plusDays(1).atStartOfDay()));
            if (StringUtils.hasText(paymentStatus)) predicates.add(builder.equal(root.get("paymentStatus"), paymentStatus.trim()));
            if (StringUtils.hasText(serviceStatus)) {
                if ("__empty__".equals(serviceStatus)) predicates.add(builder.or(builder.isNull(root.get("serviceStatus")), builder.equal(root.get("serviceStatus"), "")));
                else predicates.add(builder.equal(root.get("serviceStatus"), serviceStatus.trim()));
            }
            if (ownerId != null) predicates.add(builder.equal(root.get("ownerId"), ownerId));
            if (StringUtils.hasText(inviter)) {
                if ("__empty__".equals(inviter)) {
                    var linked = query.subquery(Long.class);
                    var invitation = linked.from(com.youai.crm.invitation.InvitationRecord.class);
                    linked.select(invitation.get("id")).where(builder.equal(invitation.get("relatedOrderNo"), root.get("orderNo")));
                    predicates.add(builder.not(builder.exists(linked)));
                } else {
                    var latest = query.subquery(Long.class);
                    var allInvitations = latest.from(com.youai.crm.invitation.InvitationRecord.class);
                    latest.select(builder.max(allInvitations.<Long>get("id"))).where(builder.equal(allInvitations.get("relatedOrderNo"), root.get("orderNo")));
                    var matching = query.subquery(Long.class);
                    var invitation = matching.from(com.youai.crm.invitation.InvitationRecord.class);
                    matching.select(invitation.get("id")).where(builder.equal(invitation.get("id"), latest), builder.equal(invitation.get("inviter"), inviter.trim()));
                    predicates.add(builder.exists(matching));
                }
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        Pageable requested = pageable == null ? PageRequest.of(0, 20) : pageable;
        Pageable safe = PageRequest.of(Math.max(0, requested.getPageNumber()), Math.min(Math.max(requested.getPageSize(), 1), 100),
                requested.getSort().isSorted() ? requested.getSort() : Sort.by(Sort.Direction.DESC, "createdAt"));
        return repository.findAll(spec, safe).map(order -> response(order, authentication));
    }

    public record FilterOption(String value, String label) {}
    public record FilterOptions(List<FilterOption> sales, List<FilterOption> inviters, List<FilterOption> paymentStatuses, List<FilterOption> serviceStatuses) {}

    @Transactional(Transactional.TxType.SUPPORTS)
    public FilterOptions filterOptions(Authentication authentication) {
        Long userId = accessPolicy.currentUserId(authentication);
        boolean admin = accessPolicy.isAdmin(authentication);
        var sales = new java.util.TreeMap<String, String>();
        var payments = new java.util.TreeMap<String, String>();
        var services = new java.util.TreeMap<String, String>();
        for (Object[] row : repository.visibleFilterValues(admin, userId)) {
            if (admin && row[0] != null) sales.put(row[0].toString(), (String) row[1]);
            if (row[2] != null) payments.put(row[2].toString(), "已支付".equals(row[2]) ? "支付成功" : row[2].toString());
            String service = row[3] == null ? "" : row[3].toString();
            services.put(service.isEmpty() ? "__empty__" : service, service.isEmpty() ? "空" : service);
        }
        // The account directory supplies the complete selectable roster, including people with no orders.
        // Keep historical order owners for administrators so old orders remain searchable after an account is disabled.
        for (var employee : users.findAllByEnabledTrueOrderByDisplayNameAsc()) {
            if (isSalesUser(employee) && (admin || employee.getId().equals(userId)))
                sales.put(employee.getId().toString(), employee.getDisplayName());
        }
        var inviters = new java.util.ArrayList<FilterOption>();
        inviters.add(new FilterOption("__empty__", "未关联邀约"));
        repository.visibleInviters(admin, userId).forEach(value -> inviters.add(new FilterOption(value, value)));
        return new FilterOptions(options(sales), inviters, options(payments), options(services));
    }

    private List<FilterOption> options(java.util.Map<String, String> values) {
        return values.entrySet().stream().map(entry -> new FilterOption(entry.getKey(), entry.getValue())).toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public OrderResponse find(String orderNo, Authentication authentication) {
        return response(get(orderNo, authentication), authentication);
    }

    public OrderResponse create(OrderRequest request, Authentication authentication) {
        SalesOrder order = new SalesOrder();
        apply(order, request, true, authentication);
        if (!accessPolicy.isAdmin(authentication)) {
            if (order.getPaidAmount().signum() != 0) throw new org.springframework.security.access.AccessDeniedException("只有管理员可以登记回款");
            order.setPaymentStatus(paymentStatus(order.getAmount(), BigDecimal.ZERO));
            order.setOwnerId(accessPolicy.currentUserId(authentication));
        }
        if (!accessPolicy.isAdmin(authentication)) order.setOwner(accessPolicy.currentOwner(authentication));
        repository.saveAndFlush(order);
        contracts.save(SalesContract.from(order));
        return response(order, authentication);
    }

    public OrderResponse update(String orderNo, OrderRequest request, Authentication authentication) {
        SalesOrder order = getForUpdate(orderNo, authentication);
        if (!accessPolicy.isAdmin(authentication) && (request.paid() == null ? BigDecimal.ZERO : request.paid()).compareTo(order.getPaidAmount()) != 0)
            throw new org.springframework.security.access.AccessDeniedException("只有管理员可以修改回款");
        apply(order, request, false, authentication);
        if (!accessPolicy.isAdmin(authentication)) {
            order.setOwnerId(accessPolicy.currentUserId(authentication));
            order.setPaymentStatus(paymentStatus(order.getAmount(), order.getPaidAmount()));
        }
        if (!accessPolicy.isAdmin(authentication)) order.setOwner(accessPolicy.currentOwner(authentication));
        return response(repository.save(order), authentication);
    }

    public OrderResponse updatePayment(String orderNo, BigDecimal paid, Authentication authentication) {
        if (!accessPolicy.isAdmin(authentication)) throw new org.springframework.security.access.AccessDeniedException("只有管理员可以操作财务数据");
        SalesOrder order = getForUpdate(orderNo, authentication);
        validatePaid(order.getAmount(), paid);
        if (order.getPaidAmount().compareTo(paid) != 0) order.resetConfirmations();
        order.setPaidAmount(paid);
        order.setPaymentStatus(paymentStatus(order.getAmount(), paid));
        order.setServiceStatus("已支付".equals(order.getPaymentStatus()) ? "待启用" : "");
        return response(repository.save(order), authentication);
    }

    public OrderResponse updateService(String orderNo, String serviceStatus, Authentication authentication) {
        if (!StringUtils.hasText(serviceStatus)) throw new IllegalArgumentException("服务状态不能为空");
        SalesOrder order = getForUpdate(orderNo, authentication);
        String expected = "已支付".equals(order.getPaymentStatus()) ? "待启用" : "";
        if (!expected.equals(serviceStatus.trim())) throw new IllegalArgumentException("服务状态由订单支付状态确定");
        order.setServiceStatus(expected);
        return response(repository.save(order), authentication);
    }

    public OrderResponse updateConfirmation(String orderNo, boolean confirmed, Authentication authentication) {
        SalesOrder order = repository.lockByOrderNo(orderNo)
                .orElseThrow(() -> new NotFoundException("未找到订单：" + orderNo));
        Long userId = accessPolicy.currentUserId(authentication);
        boolean sales = java.util.Objects.equals(userId, order.getConfirmationSalesId()) && !accessPolicy.isAdmin(authentication);
        boolean admin = java.util.Objects.equals(userId, order.getConfirmationAdminId()) && accessPolicy.isAdmin(authentication);
        if (!sales && !admin) throw new org.springframework.security.access.AccessDeniedException("仅负责销售和指定管理员可分别确认本人的业绩");
        if (!confirmed) throw new IllegalArgumentException("业绩确认不支持撤销");
        if (!isPaymentSuccessful(order))
            throw new IllegalArgumentException("订单支付成功后才能确认业绩");
        if (sales && order.getSalesConfirmedAt() == null) order.setSalesConfirmedAt(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        if (admin && order.getAdminConfirmedAt() == null) order.setAdminConfirmedAt(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        order.setPerformanceConfirmed(order.getSalesConfirmedAt() != null && order.getAdminConfirmedAt() != null);
        return response(repository.save(order), authentication);
    }

    private OrderResponse response(SalesOrder order, Authentication authentication) {
        Long userId = accessPolicy.currentUserId(authentication);
        boolean sales = !accessPolicy.isAdmin(authentication) && java.util.Objects.equals(userId, order.getConfirmationSalesId()) && order.getSalesConfirmedAt() == null;
        boolean admin = accessPolicy.isAdmin(authentication) && java.util.Objects.equals(userId, order.getConfirmationAdminId()) && order.getAdminConfirmedAt() == null;
        boolean canConfirm = isPaymentSuccessful(order) && !order.isPerformanceConfirmed() && (sales || admin);
        String inviter = invitations.findFirstByRelatedOrderNoOrderByIdDesc(order.getOrderNo()).map(com.youai.crm.invitation.InvitationRecord::getInviter).orElse(null);
        return OrderResponse.from(order, canConfirm, inviter);
    }

    private boolean isPaymentSuccessful(SalesOrder order) {
        return "已支付".equals(order.getPaymentStatus()) && order.getPaidAmount() != null
                && order.getPaidAmount().signum() > 0 && order.getPaidAmount().compareTo(order.getAmount()) >= 0;
    }

    private boolean isSalesUser(com.youai.crm.account.CrmUser user) {
        return user != null && user.isEnabled() && user.getRoles().stream().noneMatch(role -> "ADMIN".equals(role.getCode()));
    }

    private void assignConfirmers(SalesOrder order) {
        var salesUser = order.getOwnerId() == null ? null : users.findById(order.getOwnerId()).orElse(null);
        // The order's salesperson signs with Zhao Di; customer ownership may be unassigned or held by an administrator.
        if (!isSalesUser(salesUser) && order.getCustomerNo() != null) {
            var customer = customers.findByCustomerNo(order.getCustomerNo()).orElse(null);
            salesUser = customer == null || customer.getOwnerId() == null ? null : users.findById(customer.getOwnerId()).orElse(null);
        }
        Long salesId = isSalesUser(salesUser) ? salesUser.getId() : null;
        String salesName = salesId == null ? order.getOwner() : salesUser.getDisplayName();
        var administrator = users.findByUsernameIgnoreCase("admin").filter(user -> user.isEnabled() && user.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getCode()))).orElse(null);
        Long adminId = administrator == null ? null : administrator.getId();
        if (!java.util.Objects.equals(salesId, order.getConfirmationSalesId()) || !java.util.Objects.equals(adminId, order.getConfirmationAdminId())) order.resetConfirmations();
        order.setConfirmationSalesId(salesId);
        order.setConfirmationSalesName(salesName);
        order.setConfirmationAdminId(adminId);
        order.setConfirmationAdminName(administrator == null ? "赵娣" : administrator.getDisplayName());
    }

    private void apply(SalesOrder order, OrderRequest request, boolean creating, Authentication authentication) {
        Long originalOwnerId = order.getOwnerId();
        String originalCustomerNo = order.getCustomerNo();
        BigDecimal amount = request.amount();
        BigDecimal paid = request.paid() == null ? BigDecimal.ZERO : request.paid();
        validatePaid(amount, paid);
        if (creating) {
            String orderNo = StringUtils.hasText(request.orderNo()) ? request.orderNo().trim() : nextOrderNo();
            if (repository.findByOrderNo(orderNo).isPresent()) throw new IllegalArgumentException("订单编号已存在");
            order.setOrderNo(orderNo);
            order.setCreatedAt(LocalDateTime.now());
        }
        String customerName = request.customer().trim();
        if (StringUtils.hasText(request.customerId())) {
            var customer = customerQueries.find(request.customerId().trim(), authentication);
            order.setCustomerNo(customer.id());
            customerName = customer.name();
        } else if (creating || !customerName.equals(order.getCustomerName())) {
            // Compatibility for callers that still send names. Never guess between namesakes.
            var matches = customers.findAllByName(customerName).stream()
                    .filter(customer -> accessPolicy.canAccessUserId(customer.getOwnerId(), authentication)).toList();
            order.setCustomerNo(matches.size() == 1 ? matches.getFirst().getCustomerNo() : null);
        }
        if (!creating && (!java.util.Objects.equals(originalCustomerNo, order.getCustomerNo()) || !java.util.Objects.equals(order.getCustomerName(), customerName)
                || !java.util.Objects.equals(order.getProduct(), request.product().trim())
                || order.getAmount().compareTo(amount) != 0 || order.getPaidAmount().compareTo(paid) != 0
                || "已取消".equals(request.status()))) order.resetConfirmations();
        order.setCustomerName(customerName);
        order.setProduct(request.product().trim());
        order.setAmount(amount);
        order.setPaidAmount(paid);
        order.setPaymentStatus(StringUtils.hasText(request.status()) ? request.status().trim() : paymentStatus(amount, paid));
        order.setServiceStatus("已支付".equals(order.getPaymentStatus()) ? "待启用" : "");
        // Resolve ownership once when writing; display names are snapshots.
        if (accessPolicy.isAdmin(authentication)) {
            var employee = identities.enabledEmployee(request.owner());
            order.setOwner(employee.getDisplayName());
            order.setOwnerId(employee.getId());
        } else {
            order.setOwner(accessPolicy.currentDisplayName(authentication));
            order.setOwnerId(accessPolicy.currentUserId(authentication));
        }
        var salesperson = users.findById(order.getOwnerId()).orElseThrow();
        if (creating || !java.util.Objects.equals(order.getOwnerId(), originalOwnerId))
            order.setStoreName(salesperson.getDepartment().getStoreName());
        assignConfirmers(order);
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
        for (int attempt = 0; attempt < 1000; attempt++) {
            String candidate = String.valueOf(100000 + ORDER_RANDOM.nextInt(900000));
            if (repository.findByOrderNo(candidate).isEmpty()) return candidate;
        }
        throw new IllegalStateException("暂时无法生成可用订单编号，请稍后重试");
    }

    private SalesOrder getForUpdate(String orderNo, Authentication authentication) {
        SalesOrder order = repository.lockByOrderNo(orderNo).orElseThrow(() -> new NotFoundException("未找到订单：" + orderNo));
        accessPolicy.requireUserId(order.getOwnerId(), authentication);
        return order;
    }

    private SalesOrder get(String orderNo, Authentication authentication) {
        SalesOrder order = repository.findByOrderNo(orderNo)
                .orElseThrow(() -> new NotFoundException("未找到订单：" + orderNo));
        if (!accessPolicy.canAccessUserId(order.getOwnerId(), authentication) && !accessPolicy.canAccessUserId(order.getConfirmationSalesId(), authentication))
            throw new org.springframework.security.access.AccessDeniedException("只能查看本人负责的订单");
        return order;
    }
}
