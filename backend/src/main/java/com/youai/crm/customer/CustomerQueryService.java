package com.youai.crm.customer;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.common.NotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class CustomerQueryService {

    private static final String PUBLIC_POOL = "\u516c\u6d77";
    private final CustomerRepository repository;
    private final AccessPolicy accessPolicy;
    private final CustomerResponseMapper responseMapper;

    public CustomerQueryService(CustomerRepository repository, AccessPolicy accessPolicy,
            CustomerResponseMapper responseMapper) {
        this.repository = repository;
        this.accessPolicy = accessPolicy;
        this.responseMapper = responseMapper;
    }

    public Page<CustomerResponse> search(String keyword, String stage, String level, String owner, String tag,
            Boolean inPool, Authentication authentication) {
        return search(keyword, stage, level, owner, tag, inPool, Map.of(), PageRequest.of(0, 20), authentication);
    }

    public Page<CustomerResponse> search(String keyword, String stage, String level, String owner, String tag,
            Boolean inPool, Map<String, String> advanced, Pageable pageable, Authentication authentication) {
        accessPolicy.scopedOwner(authentication);
        boolean admin = accessPolicy.isAdmin(authentication);
        Pageable safePageable = safePageable(pageable, Sort.by(Sort.Direction.DESC, "id"));
        org.springframework.data.jpa.domain.Specification<Customer> specification = (root, query, builder) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(builder.or(builder.like(root.get("name"), like),
                        builder.like(root.get("phone"), like), builder.like(root.get("company"), like),
                        builder.like(root.get("customerNo"), like)));
            }
            if (StringUtils.hasText(stage)) predicates.add(builder.equal(root.get("stage"), stage));
            if (StringUtils.hasText(level)) predicates.add(builder.equal(root.get("level"), level));
            if (!admin && !Boolean.TRUE.equals(inPool)) {
                predicates.add(builder.equal(root.get("owner"), accessPolicy.currentOwner(authentication)));
            } else if (StringUtils.hasText(owner)) {
                predicates.add(builder.equal(root.get("owner"), owner.trim()));
            }
            if (inPool != null) {
                predicates.add(inPool ? builder.equal(root.get("owner"), PUBLIC_POOL)
                        : builder.notEqual(root.get("owner"), PUBLIC_POOL));
            }
            addAdvancedPredicates(root, query, builder, predicates, advanced, tag);
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        if (hasNumericRange(advanced)) {
            List<Customer> filtered = repository.findAll(specification, safePageable.getSort()).stream()
                    .filter(customer -> numericFiltersMatch(customer, advanced)).toList();
            int from = (int) Math.min((long) safePageable.getOffset(), filtered.size());
            int to = Math.min(from + safePageable.getPageSize(), filtered.size());
            List<CustomerResponse> content = filtered.subList(from, to).stream()
                    .map(customer -> responseMapper.toResponse(customer, authentication)).toList();
            return new PageImpl<>(content, safePageable, filtered.size());
        }
        return repository.findAll(specification, safePageable)
                .map(customer -> responseMapper.toResponse(customer, authentication));
    }

    private void addAdvancedPredicates(jakarta.persistence.criteria.From<?, Customer> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder builder, List<Predicate> predicates,
            Map<String, String> advanced, String tag) {
        Map<String, String> values = advanced == null ? Map.of() : advanced;
        addEquals(builder, root, predicates, values, "gender", "gender");
        addEquals(builder, root, predicates, values, "maritalStatus", "maritalStatus");
        addEquals(builder, root, predicates, values, "customerStatus", "stage");
        addLike(builder, root, predicates, values, "occupation", "occupation");
        addLike(builder, root, predicates, values, "housing", "housing");
        addLike(builder, root, predicates, values, "car", "car");
        addLike(builder, root, predicates, values, "nativePlace", "nativePlace");
        addLike(builder, root, predicates, values, "workLocation", "workLocation");
        addLike(builder, root, predicates, values, "personality", "matchPersonality");
        addLike(builder, root, predicates, values, "interest", "matchMostImportant");
        addLike(builder, root, predicates, values, "owner", "owner");
        addLike(builder, root, predicates, values, "collaborator", "collaborator");
        if (has(values, "note")) {
            String like = "%" + values.get("note").trim().toLowerCase() + "%";
            predicates.add(builder.or(builder.like(builder.lower(root.get("note")), like),
                    builder.like(builder.lower(root.get("remark")), like)));
        }
        if (has(values, "education")) {
            List<Predicate> education = java.util.Arrays.stream(values.get("education").split(","))
                    .map(String::trim).filter(StringUtils::hasText)
                    .map(value -> builder.like(builder.lower(root.get("education")), "%" + value.toLowerCase() + "%"))
                    .toList();
            if (!education.isEmpty()) predicates.add(builder.or(education.toArray(Predicate[]::new)));
        }
        if (has(values, "customerType")) {
            String type = values.get("customerType").trim();
            boolean member = "member".equalsIgnoreCase(type) || "\u4f1a\u5458".equals(type);
            predicates.add(member ? builder.like(root.get("level"), "%\u4f1a\u5458%")
                    : builder.notLike(root.get("level"), "%\u4f1a\u5458%"));
        }
        addDateRange(builder, root, predicates, values, "registrationStart", "registrationEnd", "createdAt");
        addDateRange(builder, root, predicates, values, "firstAllocationStart", "firstAllocationEnd", "firstAllocationAt");
        addDateRange(builder, root, predicates, values, "lastFollowUpStart", "lastFollowUpEnd", "lastContactAt");
        addDateRange(builder, root, predicates, values, "nextFollowStart", "nextFollowEnd", "nextFollowAt");
        if (has(values, "uncontactedDays")) {
            try {
                long days = Long.parseLong(values.get("uncontactedDays").trim());
                LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
                predicates.add(builder.or(builder.lessThanOrEqualTo(root.get("lastContactAt"), cutoff),
                        builder.and(builder.isNull(root.get("lastContactAt")),
                                builder.lessThanOrEqualTo(root.get("createdAt"), cutoff))));
            } catch (NumberFormatException ignored) { }
        }
        if (StringUtils.hasText(tag)) {
            query.distinct(true);
            predicates.add(builder.equal(root.join("tags"), tag.trim()));
        }
    }

    private void addEquals(jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.From<?, Customer> root, List<Predicate> predicates,
            Map<String, String> values, String key, String field) {
        if (has(values, key)) predicates.add(builder.equal(root.get(field), values.get(key).trim()));
    }

    private void addLike(jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.From<?, Customer> root, List<Predicate> predicates,
            Map<String, String> values, String key, String field) {
        if (has(values, key)) predicates.add(builder.like(builder.lower(root.get(field)),
                "%" + values.get(key).trim().toLowerCase() + "%"));
    }

    private void addDateRange(jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.From<?, Customer> root, List<Predicate> predicates,
            Map<String, String> values, String startKey, String endKey, String field) {
        try {
            if (has(values, startKey)) predicates.add(builder.greaterThanOrEqualTo(root.get(field),
                    LocalDateTime.parse(values.get(startKey).trim() + "T00:00:00")));
            if (has(values, endKey)) predicates.add(builder.lessThan(root.get(field),
                    LocalDateTime.parse(values.get(endKey).trim() + "T00:00:00").plusDays(1)));
        } catch (RuntimeException ignored) { }
    }

    private boolean has(Map<String, String> values, String key) {
        return StringUtils.hasText(values.get(key));
    }

    private boolean hasNumericRange(Map<String, String> values) {
        return values != null && (has(values, "ageMin") || has(values, "ageMax")
                || has(values, "heightMin") || has(values, "heightMax")
                || has(values, "incomeMin") || has(values, "incomeMax"));
    }

    private boolean numericFiltersMatch(Customer customer, Map<String, String> values) {
        if (!numberBetween(customer.getAge(), values.get("ageMin"), values.get("ageMax"))) return false;
        if (!numberBetween(customer.getHeight(), values.get("heightMin"), values.get("heightMax"))) return false;
        String income = StringUtils.hasText(customer.getMonthlyIncome())
                ? customer.getMonthlyIncome() : customer.getAnnualIncome();
        return numberBetween(income, values.get("incomeMin"), values.get("incomeMax"));
    }

    private boolean numberBetween(String value, String min, String max) {
        boolean hasMin = StringUtils.hasText(min);
        boolean hasMax = StringUtils.hasText(max);
        if (!hasMin && !hasMax) return true;
        if (!StringUtils.hasText(value)) return false;
        try {
            double number = Double.parseDouble(value.replaceAll("[^0-9.\\-]", ""));
            if (hasMin && number < Double.parseDouble(min.trim())) return false;
            if (hasMax && number > Double.parseDouble(max.trim())) return false;
            return true;
        } catch (RuntimeException ignored) {
            return true;
        }
    }

    public Page<CustomerResponse> pool(Pageable pageable, Authentication authentication) {
        accessPolicy.scopedOwner(authentication);
        return repository.findAll((root, query, builder) -> builder.equal(root.get("owner"), PUBLIC_POOL),
                safePageable(pageable, Sort.by(Sort.Direction.DESC, "id")))
                .map(customer -> responseMapper.toResponse(customer, authentication));
    }

    public Map<String, Long> tags(Authentication authentication) {
        accessPolicy.scopedOwner(authentication);
        return repository.findAll().stream()
                .filter(customer -> accessPolicy.isAdmin(authentication) || PUBLIC_POOL.equals(customer.getOwner())
                        || accessPolicy.canAccessOwner(customer.getOwner(), authentication))
                .flatMap(customer -> customer.getTags().stream())
                .collect(Collectors.groupingBy(tag -> tag, LinkedHashMap::new, Collectors.counting()));
    }

    public CustomerResponse find(String customerNo, Authentication authentication) {
        return responseMapper.toResponse(get(customerNo, authentication), authentication);
    }

    public Customer get(String customerNo, Authentication authentication) {
        Customer customer = repository.findByCustomerNo(customerNo)
                .orElseThrow(() -> new NotFoundException("鏈壘鍒板鎴凤細" + customerNo));
        accessPolicy.requireOwner(customer.getOwner(), authentication);
        return customer;
    }

    private Pageable safePageable(Pageable pageable, Sort fallbackSort) {
        Pageable requested = pageable == null ? PageRequest.of(0, 20, fallbackSort) : pageable;
        return PageRequest.of(Math.max(0, requested.getPageNumber()),
                Math.min(Math.max(requested.getPageSize(), 1), 100),
                requested.getSort().isSorted() ? requested.getSort() : fallbackSort);
    }
}
