package com.youai.crm.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.common.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Owns create/update commands and customer field normalization. */
@Service
@Transactional
public class CustomerCommandService {

    private static final long CUSTOMER_NO_START = 1_430_038_038L;
    private static final String PUBLIC_POOL = "\u516c\u6d77";
    private static final String WHITEBOARD = "\u767d\u677f";

    private final CustomerRepository repository;
    private final AccessPolicy accessPolicy;
    private final CrmUserRepository userRepository;
    private final CustomerQueryService queryService;
    private final CustomerResponseMapper responseMapper;
    private final CustomerEventService eventService;

    public CustomerCommandService(CustomerRepository repository, AccessPolicy accessPolicy,
            CrmUserRepository userRepository, CustomerQueryService queryService,
            CustomerResponseMapper responseMapper, CustomerEventService eventService) {
        this.repository = repository;
        this.accessPolicy = accessPolicy;
        this.userRepository = userRepository;
        this.queryService = queryService;
        this.responseMapper = responseMapper;
        this.eventService = eventService;
    }

    public CustomerResponse create(CustomerRequest request) {
        return create(request, null);
    }

    public CustomerResponse create(CustomerRequest request, Authentication authentication) {
        return create(request, authentication, "\u624b\u52a8\u5f55\u5165");
    }

    public CustomerResponse create(CustomerRequest request, Authentication authentication, String registrationSource) {
        String phone = request.phone().trim();
        Customer duplicate = repository.findByPhone(phone).orElse(null);
        if (duplicate != null) {
            accessPolicy.requireOwner(duplicate.getOwner(), authentication);
            int nextRegistrationNumber = Math.max(1,
                    duplicate.getRegistrationCount() == null ? 1 : duplicate.getRegistrationCount()) + 1;
            duplicate.setRegistrationCount(nextRegistrationNumber);
            Customer saved = repository.save(duplicate);
            eventService.saveRegistrationEvent(saved, nextRegistrationNumber, registrationSource, authentication);
            return responseMapper.toResponse(saved, authentication);
        }

        Customer customer = new Customer();
        customer.setCustomerNo(nextCustomerNo());
        apply(customer, request, authentication, false);
        boolean imported = "\u6279\u91cf\u5bfc\u5165".equals(registrationSource);
        if (authentication == null) {
            // Internal demo/bootstrap data keeps its declared owner. HTTP
            // callers always provide authentication and follow the rules below.
        } else if (imported && PUBLIC_POOL.equals(request.owner().trim())) {
            customer.setOwner(PUBLIC_POOL);
            customer.setPoolEntryType("主动放弃");
        } else if (imported || accessPolicy.isAdmin(authentication)) {
            customer.setOwner(WHITEBOARD);
        } else {
            customer.setOwner(accessPolicy.currentOwner(authentication));
        }
        if (!PUBLIC_POOL.equals(customer.getOwner())) customer.setPoolEnteredAt(null);
        customer.setStage(defaultText(request.stage(), "\u521d\u6b65\u6c9f\u901a"));
        customer.setLastContactAt(LocalDateTime.now());
        customer.setTags(request.tags() == null || request.tags().isEmpty()
                ? List.of("\u65b0\u5ba2\u6237") : request.tags());
        customer.setRegistrationCount(1);
        Customer saved = repository.save(customer);
        eventService.saveRegistrationEvent(saved, 1, registrationSource, authentication);
        return responseMapper.toResponse(saved, authentication);
    }

    public CustomerResponse update(String customerNo, CustomerRequest request, Authentication authentication) {
        Customer customer = queryService.get(customerNo, authentication);
        String previousOwner = customer.getOwner();
        apply(customer, request, authentication, true);
        if (!accessPolicy.isAdmin(authentication)) customer.setOwner(accessPolicy.currentOwner(authentication));
        if (!PUBLIC_POOL.equals(previousOwner) && PUBLIC_POOL.equals(customer.getOwner())) {
            customer.setPreviousOwner(previousOwner);
            customer.setPoolEntryType("主动放弃");
        }
        else if (!PUBLIC_POOL.equals(customer.getOwner())) customer.setPoolEntryType(null);
        if (StringUtils.hasText(request.stage())) customer.setStage(request.stage());
        if (request.tags() != null) customer.setTags(request.tags());
        Customer saved = repository.save(customer);
        eventService.recordAssignmentIfNeeded(saved, previousOwner, saved.getOwner(), authentication);
        if (!PUBLIC_POOL.equals(previousOwner) && PUBLIC_POOL.equals(saved.getOwner())) {
            eventService.recordOperation(saved, "移入公海", "编辑归属移入公海", authentication);
        }
        if (Objects.equals(previousOwner, saved.getOwner())) {
            eventService.recordOperation(saved, "\u7f16\u8f91\u5ba2\u6237\u8d44\u6599",
                    "\u5458\u5de5\u53ca\u7ba1\u7406\u5458\u66f4\u65b0\u4e86\u5ba2\u6237\u8d44\u6599", authentication);
        }
        return responseMapper.toResponse(saved, authentication);
    }

    public CustomerResponse updateStage(String customerNo, String stage, Authentication authentication) {
        if (!StringUtils.hasText(stage)) throw new IllegalArgumentException("\u5ba2\u6237\u9636\u6bb5\u4e0d\u80fd\u4e3a\u7a7a");
        Customer customer = queryService.get(customerNo, authentication);
        String previousStage = customer.getStage();
        customer.setStage(stage.trim());
        customer.setLastContactAt(LocalDateTime.now());
        Customer saved = repository.save(customer);
        if (!Objects.equals(previousStage, saved.getStage())) {
            eventService.recordOperation(saved, "\u66f4\u65b0\u5ba2\u6237\u9636\u6bb5",
                    "\u5ba2\u6237\u9636\u6bb5\u7531\u201c" + previousStage + "\u201d\u66f4\u65b0\u4e3a\u201c" + saved.getStage() + "\u201d", authentication);
        }
        return responseMapper.toResponse(saved, authentication);
    }

    private void apply(Customer customer, CustomerRequest request, Authentication authentication,
            boolean resolveRequestedOwner) {
        customer.setName(request.name().trim());
        customer.setPhone(request.phone().trim());
        customer.setCompany(defaultText(request.company(), "\u4e2a\u4eba\u5ba2\u6237"));
        customer.setSource(request.source().trim());
        String requestedOwner = request.owner().trim();
        customer.setOwner(resolveRequestedOwner && authentication != null && accessPolicy.isAdmin(authentication)
                ? resolveOwner(requestedOwner) : requestedOwner);
        customer.setLevel(defaultText(request.level(), "\u666e\u901a\u5ba2\u6237"));
        customer.setExpectedAmount(request.amount() == null ? BigDecimal.ZERO : request.amount());
        customer.setCity(defaultText(request.city(), "\u5f85\u8865\u5145"));
        customer.setNote(defaultText(request.note(), "\u6682\u65e0\u5907\u6ce8"));
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
        if (request.customerType() != null) customer.setCustomerType(request.customerType());
        customer.setLastLoginAt(request.lastLoginAt());
        if (request.avatarUrl() != null) customer.setAvatarUrl(request.avatarUrl());
    }

    private long nextNumber() {
        return repository.findAll().stream().map(Customer::getCustomerNo)
                .filter(value -> value != null && value.matches("\\d{10}"))
                .mapToLong(Long::parseLong).max().orElse(CUSTOMER_NO_START - 1) + 1;
    }

    private String nextCustomerNo() {
        return Long.toString(nextNumber());
    }

    private String resolveOwner(String requestedOwner) {
        if (PUBLIC_POOL.equals(requestedOwner) || WHITEBOARD.equals(requestedOwner)) return requestedOwner;
        return resolveEmployeeOwner(requestedOwner);
    }

    private String resolveEmployeeOwner(String requestedOwner) {
        if (!StringUtils.hasText(requestedOwner) || PUBLIC_POOL.equals(requestedOwner) || WHITEBOARD.equals(requestedOwner)) {
            throw new IllegalArgumentException("\u8d1f\u8d23\u4eba\u5fc5\u987b\u9009\u62e9\u771f\u5b9e\u5458\u5de5\u8d26\u53f7");
        }
        return userRepository.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
                .filter(user -> requestedOwner.equalsIgnoreCase(user.getDisplayName()))
                .filter(user -> user.getRoles().stream().noneMatch(role -> "ADMIN".equals(role.getCode())))
                .map(user -> user.getDisplayName()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("\u8d1f\u8d23\u4eba\u8d26\u53f7\u4e0d\u5b58\u5728\uff0c\u8bf7\u9009\u62e9\u771f\u5b9e\u5458\u5de5\u8d26\u53f7"));
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}
