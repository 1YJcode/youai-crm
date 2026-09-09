package com.youke.crm.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import com.youke.crm.common.NotFoundException;
import com.youke.crm.account.CrmUserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.youke.crm.account.AccessPolicy;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private static final long CUSTOMER_NO_START = 1_430_038_038L;
    private final CustomerRepository repository;
    private final AccessPolicy accessPolicy;
    private final CrmUserRepository userRepository;
    private final CustomerRegistrationEventRepository registrationEventRepository;
    private final CustomerAssignmentEventRepository assignmentEventRepository;

    public CustomerService(CustomerRepository repository, AccessPolicy accessPolicy, CrmUserRepository userRepository,
            CustomerRegistrationEventRepository registrationEventRepository,
            CustomerAssignmentEventRepository assignmentEventRepository) {
        this.repository = repository;
        this.accessPolicy = accessPolicy;
        this.userRepository = userRepository;
        this.registrationEventRepository = registrationEventRepository;
        this.assignmentEventRepository = assignmentEventRepository;
    }

    public List<CustomerResponse> search(String keyword, String stage, String level, String owner, String tag, Boolean inPool, Authentication authentication) {
        String scopedOwner = accessPolicy.scopedOwner(authentication);
        return repository.findAll((root, query, builder) -> {
                    List<Predicate> predicates = new java.util.ArrayList<>();
                    if (StringUtils.hasText(keyword)) {
                        String like = "%" + keyword.trim() + "%";
                        predicates.add(builder.or(
                                builder.like(root.get("name"), like),
                                builder.like(root.get("phone"), like),
                                builder.like(root.get("company"), like),
                                builder.like(root.get("customerNo"), like)));
                    }
                    if (StringUtils.hasText(stage)) {
                        predicates.add(builder.equal(root.get("stage"), stage));
                    }
                    if (StringUtils.hasText(level)) {
                        predicates.add(builder.equal(root.get("level"), level));
                    }
                    if (StringUtils.hasText(scopedOwner)) {
                        predicates.add(builder.equal(root.get("owner"), scopedOwner));
                    } else if (StringUtils.hasText(owner)) {
                        predicates.add(builder.equal(root.get("owner"), owner.trim()));
                    }
                    if (inPool != null) predicates.add(inPool ? builder.equal(root.get("owner"), "公海") : builder.notEqual(root.get("owner"), "公海"));
                    return builder.and(predicates.toArray(Predicate[]::new));
                }, Sort.by(Sort.Direction.DESC, "id")).stream()
                .filter(customer -> !StringUtils.hasText(tag) || customer.getTags().contains(tag.trim()))
                .map(customer -> toResponse(customer, authentication)).toList();
    }

    public List<CustomerResponse> pool(Authentication authentication) {
        return repository.findAll((root, query, builder) -> builder.equal(root.get("owner"), "公海"),
                        Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(customer -> toResponse(customer, authentication))
                .toList();
    }

    public Map<String, Long> tags(Authentication authentication) {
        return repository.findAll().stream().filter(customer -> accessPolicy.isAdmin(authentication) || "公海".equals(customer.getOwner()) || customer.getOwner().equals(accessPolicy.currentOwner(authentication)))
                .flatMap(customer -> customer.getTags().stream()).collect(Collectors.groupingBy(tag -> tag, LinkedHashMap::new, Collectors.counting()));
    }

    public CustomerResponse find(String customerNo, Authentication authentication) {
        return toResponse(get(customerNo, authentication), authentication);
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        return create(request, null);
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request, Authentication authentication) {
        return create(request, authentication, "手动录入");
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request, Authentication authentication, String registrationSource) {
        String phone = request.phone().trim();
        Customer duplicate = repository.findByPhone(phone).orElse(null);
        if (duplicate != null) {
            accessPolicy.requireOwner(duplicate.getOwner(), authentication);
            int nextRegistrationNumber = Math.max(1, duplicate.getRegistrationCount() == null ? 1 : duplicate.getRegistrationCount()) + 1;
            duplicate.setRegistrationCount(nextRegistrationNumber);
            Customer saved = repository.save(duplicate);
            saveRegistrationEvent(saved, nextRegistrationNumber, registrationSource, authentication);
            return toResponse(saved, authentication);
        }
        Customer customer = new Customer();
        customer.setCustomerNo(nextCustomerNo());
        apply(customer, request, authentication);
        if (!accessPolicy.isAdmin(authentication)) customer.setOwner(accessPolicy.currentOwner(authentication));
        customer.setStage(defaultText(request.stage(), "初步沟通"));
        customer.setLastContactAt(LocalDateTime.now());
        customer.setTags(request.tags() == null || request.tags().isEmpty()
                ? List.of("新客户")
                : request.tags());
        customer.setRegistrationCount(1);
        Customer saved = repository.save(customer);
        saveRegistrationEvent(saved, 1, registrationSource, authentication);
        return toResponse(saved, authentication);
    }

    public List<CustomerRegistrationEventResponse> registrationEvents(String customerNo, Authentication authentication) {
        Customer customer = get(customerNo, authentication);
        return registrationEventRepository.findAllByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                .filter(event -> event.getRegistrationNumber() >= 2)
                .map(CustomerRegistrationEventResponse::from)
                .toList();
    }

    public List<CustomerAssignmentEventResponse> assignmentEvents(String customerNo, Authentication authentication) {
        Customer customer = get(customerNo, authentication);
        return assignmentEventRepository.findAllByCustomerIdOrderByAssignedAtDesc(customer.getId()).stream()
                .map(CustomerAssignmentEventResponse::from)
                .toList();
    }

    @Transactional
    public CustomerResponse update(String customerNo, CustomerRequest request, Authentication authentication) {
        Customer customer = get(customerNo, authentication);
        String previousOwner = customer.getOwner();
        apply(customer, request, authentication);
        if (!accessPolicy.isAdmin(authentication)) customer.setOwner(accessPolicy.currentOwner(authentication));
        if (StringUtils.hasText(request.stage())) {
            customer.setStage(request.stage());
        }
        if (request.tags() != null) {
            customer.setTags(request.tags());
        }
        Customer saved = repository.save(customer);
        recordAssignmentIfNeeded(saved, previousOwner, saved.getOwner(), authentication);
        return toResponse(saved, authentication);
    }

    @Transactional
    public CustomerResponse updateStage(String customerNo, String stage, Authentication authentication) {
        if (!StringUtils.hasText(stage)) {
            throw new IllegalArgumentException("客户阶段不能为空");
        }
        Customer customer = get(customerNo, authentication);
        customer.setStage(stage.trim());
        customer.setLastContactAt(LocalDateTime.now());
        return toResponse(repository.save(customer), authentication);
    }

    @Transactional
    public CustomerResponse updatePool(String customerNo, boolean inPool, Authentication authentication) {
        Customer customer = repository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new NotFoundException("未找到客户：" + customerNo));
        if (inPool) {
            accessPolicy.requireOwner(customer.getOwner(), authentication);
            if (!"公海".equals(customer.getOwner())) customer.setPreviousOwner(customer.getOwner());
            customer.setOwner("公海");
        } else {
            if (!"公海".equals(customer.getOwner()) && !accessPolicy.isAdmin(authentication)) {
                throw new org.springframework.security.access.AccessDeniedException("只能领取公海客户");
            }
            String previousOwner = customer.getOwner();
            customer.setOwner(accessPolicy.isAdmin(authentication)
                    ? accessPolicy.currentDisplayName(authentication)
                    : accessPolicy.currentOwner(authentication));
            recordAssignmentIfNeeded(customer, previousOwner, customer.getOwner(), authentication);
        }
        customer.setLastContactAt(LocalDateTime.now());
        return toResponse(repository.save(customer), authentication);
    }

    @Transactional
    public CustomerResponse assign(String customerNo, CustomerAssignmentRequest request, Authentication authentication) {
        if (!accessPolicy.isAdmin(authentication)) {
            throw new org.springframework.security.access.AccessDeniedException("仅管理员可以分配客户");
        }
        Customer customer = repository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new NotFoundException("未找到客户：" + customerNo));
        String owner = resolveEmployeeOwner(request.owner());
        String previousOwner = customer.getOwner();
        customer.setOwner(owner);
        Customer saved = repository.save(customer);
        recordAssignmentIfNeeded(saved, previousOwner, owner, request, authentication);
        return toResponse(saved, authentication);
    }

    private Customer get(String customerNo, Authentication authentication) {
        Customer customer = repository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new NotFoundException("未找到客户：" + customerNo));
        accessPolicy.requireOwner(customer.getOwner(), authentication);
        return customer;
    }

    private CustomerResponse toResponse(Customer customer, Authentication authentication) {
        boolean contactVisible = accessPolicy.isAdmin(authentication)
                || (StringUtils.hasText(customer.getOwner())
                    && customer.getOwner().equalsIgnoreCase(accessPolicy.currentOwner(authentication)));
        return CustomerResponse.from(customer, contactVisible);
    }

    private void apply(Customer customer, CustomerRequest request, Authentication authentication) {
        customer.setName(request.name().trim());
        customer.setPhone(request.phone().trim());
        customer.setCompany(defaultText(request.company(), "个人客户"));
        customer.setSource(request.source().trim());
        String requestedOwner = request.owner().trim();
        customer.setOwner(authentication != null && accessPolicy.isAdmin(authentication)
                ? resolveOwner(requestedOwner)
                : requestedOwner);
        customer.setLevel(defaultText(request.level(), "普通客户"));
        customer.setExpectedAmount(request.amount() == null ? BigDecimal.ZERO : request.amount());
        customer.setCity(defaultText(request.city(), "待补充"));
        customer.setNote(defaultText(request.note(), "暂无备注"));
        customer.setNextFollowAt(request.nextFollowAt());
        if (request.gender() != null) customer.setGender(request.gender());
        if (request.birthday() != null) customer.setBirthday(request.birthday());
        if (request.age() != null) customer.setAge(request.age());
        if (request.height() != null) customer.setHeight(request.height());
        if (request.maritalStatus() != null) customer.setMaritalStatus(request.maritalStatus());
        if (request.education() != null) customer.setEducation(request.education());
        if (request.monthlyIncome() != null) customer.setMonthlyIncome(request.monthlyIncome());
        if (request.annualIncome() != null) customer.setAnnualIncome(request.annualIncome());
        if (request.occupation() != null) customer.setOccupation(request.occupation());
        if (request.housing() != null) customer.setHousing(request.housing());
        if (request.car() != null) customer.setCar(request.car());
        if (request.nativePlace() != null) customer.setNativePlace(request.nativePlace());
        if (request.workLocation() != null) customer.setWorkLocation(request.workLocation());
        if (request.wechat() != null) customer.setWechat(request.wechat());
        if (request.idCard() != null) customer.setIdCard(request.idCard());
        if (request.remark() != null) customer.setRemark(request.remark());
        if (request.certificationStatus() != null) customer.setCertificationStatus(request.certificationStatus());
        if (request.familyStatus() != null) customer.setFamilyStatus(request.familyStatus());
        if (request.childrenStatus() != null) customer.setChildrenStatus(request.childrenStatus());
        if (request.vehicleHousing() != null) customer.setVehicleHousing(request.vehicleHousing());
        if (request.matchAgeRange() != null) customer.setMatchAgeRange(request.matchAgeRange());
        if (request.matchMaritalStatus() != null) customer.setMatchMaritalStatus(request.matchMaritalStatus());
        if (request.matchHeightRange() != null) customer.setMatchHeightRange(request.matchHeightRange());
        if (request.matchEducation() != null) customer.setMatchEducation(request.matchEducation());
        if (request.matchMonthlyIncome() != null) customer.setMatchMonthlyIncome(request.matchMonthlyIncome());
        if (request.matchMostImportant() != null) customer.setMatchMostImportant(request.matchMostImportant());
        if (request.matchPersonality() != null) customer.setMatchPersonality(request.matchPersonality());
        if (request.matchChildren() != null) customer.setMatchChildren(request.matchChildren());
        if (request.matchDealbreakers() != null) customer.setMatchDealbreakers(request.matchDealbreakers());
        if (request.collaborator() != null) customer.setCollaborator(request.collaborator());
    }

    private String nextCustomerNo() {
        long next = repository.findAll().stream()
                .map(Customer::getCustomerNo)
                .filter(value -> value != null && value.matches("\\d{10}"))
                .mapToLong(Long::parseLong)
                .max()
                .orElse(CUSTOMER_NO_START - 1) + 1;
        return Long.toString(next);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String resolveOwner(String requestedOwner) {
        if ("公海".equals(requestedOwner) || "白板".equals(requestedOwner)) return requestedOwner;
        return resolveEmployeeOwner(requestedOwner);
    }

    private String resolveEmployeeOwner(String requestedOwner) {
        if (!StringUtils.hasText(requestedOwner) || "公海".equals(requestedOwner) || "白板".equals(requestedOwner)) {
            throw new IllegalArgumentException("负责人必须选择真实员工账号");
        }
        return userRepository.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
                .filter(user -> requestedOwner.equalsIgnoreCase(user.getDisplayName()))
                .filter(user -> user.getRoles().stream().noneMatch(role -> "ADMIN".equals(role.getCode())))
                .map(user -> user.getDisplayName())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("负责人账号不存在，请选择真实员工账号"));
    }

    private void recordAssignmentIfNeeded(Customer customer, String previousOwner, String owner,
            Authentication authentication) {
        recordAssignmentIfNeeded(customer, previousOwner, owner,
                new CustomerAssignmentRequest(owner, null, null, null), authentication);
    }

    private void recordAssignmentIfNeeded(Customer customer, String previousOwner, String owner,
            CustomerAssignmentRequest request, Authentication authentication) {
        boolean hasDetails = StringUtils.hasText(request.type()) || StringUtils.hasText(request.maturity())
                || StringUtils.hasText(request.reason());
        if (!StringUtils.hasText(owner) || "公海".equals(owner) || "白板".equals(owner)
                || (Objects.equals(previousOwner, owner) && !hasDetails)) return;
        LocalDateTime now = LocalDateTime.now();
        if (customer.getFirstAllocationAt() == null) customer.setFirstAllocationAt(now);
        customer.setLastAllocationAt(now);
        CustomerAssignmentEvent event = new CustomerAssignmentEvent();
        event.setCustomerId(customer.getId());
        event.setPreviousOwner(StringUtils.hasText(previousOwner) ? previousOwner : "未分配");
        event.setOwner(owner);
        event.setType(trimToNull(request.type()));
        event.setMaturity(trimToNull(request.maturity()));
        event.setReason(trimToNull(request.reason()));
        event.setOperator(authentication == null ? "系统" : accessPolicy.currentDisplayName(authentication));
        event.setAssignedAt(now);
        assignmentEventRepository.save(event);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private void saveRegistrationEvent(Customer customer, int registrationNumber, String source, Authentication authentication) {
        CustomerRegistrationEvent event = new CustomerRegistrationEvent();
        event.setCustomerId(customer.getId());
        event.setRegistrationNumber(registrationNumber);
        event.setSource(defaultText(source, "手动录入"));
        event.setOperator(authentication == null ? "系统" : accessPolicy.currentDisplayName(authentication));
        registrationEventRepository.save(event);
    }
}
