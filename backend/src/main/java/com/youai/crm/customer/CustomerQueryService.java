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
        Map<String, String> queryValues = advanced == null ? Map.of() : advanced;
        Pageable safePageable = safePageable(pageable, Sort.by(Sort.Direction.DESC, "id"));
        org.springframework.data.jpa.domain.Specification<Customer> specification = (root, query, builder) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(builder.or(builder.like(root.get("name"), like),
                        builder.like(root.get("phone"), like), builder.like(root.get("company"), like),
                        builder.like(root.get("customerNo"), like)));
            }
            if (StringUtils.hasText(queryValues.get("nameKeyword"))) {
                String like = "%" + queryValues.get("nameKeyword").trim().toLowerCase() + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("name")), like),
                        builder.like(builder.lower(root.get("note")), like),
                        builder.like(builder.lower(root.get("remark")), like)));
            }
            addAnyEquals(builder, root, predicates, stage, "stage");
            addAnyEquals(builder, root, predicates, level, "level");
            boolean collaborationScope = "collab".equalsIgnoreCase(queryValues.get("scope"));
            if (!admin && !Boolean.TRUE.equals(inPool) && !collaborationScope) {
                predicates.add(builder.equal(root.get("owner"), accessPolicy.currentOwner(authentication)));
            } else if (StringUtils.hasText(owner)) {
                addAnyEquals(builder, root, predicates, owner, "owner");
            }
            if (inPool != null) {
                predicates.add(inPool ? builder.equal(root.get("owner"), PUBLIC_POOL)
                        : builder.notEqual(root.get("owner"), PUBLIC_POOL));
            }
            addAdvancedPredicates(root, query, builder, predicates, queryValues, tag, authentication);
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
            Map<String, String> advanced, String tag, Authentication authentication) {
        Map<String, String> values = advanced == null ? Map.of() : advanced;
        addEquals(builder, root, predicates, values, "gender", "gender");
        addEquals(builder, root, predicates, values, "maritalStatus", "maritalStatus");
        addEquals(builder, root, predicates, values, "customerStatus", "stage");
        addAnyEquals(builder, root, predicates, values.get("levels"), "level");
        addLike(builder, root, predicates, values, "source", "source");
        addLike(builder, root, predicates, values, "occupation", "occupation");
        addLike(builder, root, predicates, values, "housing", "housing");
        addLike(builder, root, predicates, values, "car", "car");
        addLike(builder, root, predicates, values, "nativePlace", "nativePlace");
        addLike(builder, root, predicates, values, "workLocation", "workLocation");
        addLike(builder, root, predicates, values, "personality", "matchPersonality");
        addLike(builder, root, predicates, values, "interest", "matchMostImportant");
        addLike(builder, root, predicates, values, "owner", "owner");
        addAnyLike(builder, root, predicates, values.get("collaborator"), "collaborator", "、,，");
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
        // Customer has no independent member/non-member field. Keep the
        // request key for forwards compatibility, but do not infer a type
        // from the unrelated level text.
        addDateRange(builder, root, predicates, values, "registrationStart", "registrationEnd", "createdAt");
        addDateRange(builder, root, predicates, values, "firstAllocationStart", "firstAllocationEnd", "firstAllocationAt");
        addDateRange(builder, root, predicates, values, "lastFollowUpStart", "lastFollowUpEnd", "lastContactAt");
        addDateRange(builder, root, predicates, values, "nextFollowStart", "nextFollowEnd", "nextFollowAt");
        addAllocationDateRange(builder, root, predicates, values);
        addQuickFilter(builder, root, predicates, values);
        addSceneFilter(builder, root, predicates, values);
        if (has(values, "scope")) {
            String scope = values.get("scope").trim();
            String currentDisplayName = accessPolicy.currentDisplayName(authentication);
            if ("mine".equalsIgnoreCase(scope)) {
                predicates.add(builder.equal(root.get("owner"), currentDisplayName));
            } else if ("collab".equalsIgnoreCase(scope)) {
                addAnyLike(builder, root, predicates, currentDisplayName, "collaborator", "、,，");
            }
        }
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
        if ("true".equalsIgnoreCase(values.get("noTag"))) {
            predicates.add(builder.isEmpty(root.get("tags")));
        }
    }

    private void addEquals(jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.From<?, Customer> root, List<Predicate> predicates,
            Map<String, String> values, String key, String field) {
        if (has(values, key)) predicates.add(builder.equal(root.get(field), values.get(key).trim()));
    }

    private void addAnyEquals(jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.From<?, Customer> root, List<Predicate> predicates,
            String rawValues, String field) {
        List<String> options = splitValues(rawValues);
        if (options.isEmpty()) return;
        jakarta.persistence.criteria.CriteriaBuilder.In<String> in = builder.in(root.get(field));
        options.forEach(in::value);
        predicates.add(in);
    }

    private void addLike(jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.From<?, Customer> root, List<Predicate> predicates,
            Map<String, String> values, String key, String field) {
        if (has(values, key)) predicates.add(builder.like(builder.lower(root.get(field)),
                "%" + values.get(key).trim().toLowerCase() + "%"));
    }

    private void addAnyLike(jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.From<?, Customer> root, List<Predicate> predicates,
            String rawValues, String field, String delimiters) {
        List<String> options = splitValues(rawValues, delimiters);
        if (options.isEmpty()) return;
        predicates.add(builder.or(options.stream()
                .map(value -> builder.like(builder.lower(root.get(field)), "%" + value.toLowerCase() + "%"))
                .toArray(Predicate[]::new)));
    }

    private List<String> splitValues(String rawValues) {
        return splitValues(rawValues, ",，、");
    }

    private List<String> splitValues(String rawValues, String delimiters) {
        if (!StringUtils.hasText(rawValues)) return List.of();
        return java.util.Arrays.stream(rawValues.split("[" + delimiters + "]"))
                .map(String::trim).filter(StringUtils::hasText).distinct().toList();
    }

    private void addAllocationDateRange(jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.From<?, Customer> root, List<Predicate> predicates,
            Map<String, String> values) {
        if (!has(values, "allocationStart") && !has(values, "allocationEnd")) return;
        try {
            LocalDateTime start = has(values, "allocationStart")
                    ? LocalDateTime.parse(values.get("allocationStart").trim() + "T00:00:00") : null;
            LocalDateTime end = has(values, "allocationEnd")
                    ? LocalDateTime.parse(values.get("allocationEnd").trim() + "T00:00:00").plusDays(1) : null;
            List<Predicate> fields = new java.util.ArrayList<>();
            for (String field : List.of("lastAllocationAt", "firstAllocationAt", "createdAt")) {
                jakarta.persistence.criteria.Path<LocalDateTime> path = root.get(field);
                if (start != null && end != null) fields.add(builder.and(builder.greaterThanOrEqualTo(path, start), builder.lessThan(path, end)));
                else if (start != null) fields.add(builder.greaterThanOrEqualTo(path, start));
                else fields.add(builder.lessThan(path, end));
            }
            predicates.add(builder.or(fields.toArray(Predicate[]::new)));
        } catch (RuntimeException ignored) { }
    }

    private void addQuickFilter(jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.From<?, Customer> root, List<Predicate> predicates,
            Map<String, String> values) {
        if (!has(values, "quickFilter")) return;
        String quickFilter = values.get("quickFilter").trim();
        if ("重点客户".equals(quickFilter)) predicates.add(builder.equal(root.get("level"), "重点客户"));
        else if ("即将成交".equals(quickFilter)) predicates.add(root.get("stage").in("方案报价", "商务谈判"));
        else if ("今日待跟进".equals(quickFilter)) addTodayRange(builder, root, predicates, "nextFollowAt");
    }

    private void addSceneFilter(jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.From<?, Customer> root, List<Predicate> predicates,
            Map<String, String> values) {
        if (!has(values, "scene")) return;
        String scene = values.get("scene").trim();
        if ("today-new".equals(scene)) addTodayRange(builder, root, predicates, "createdAt");
        else if ("today-follow".equals(scene)) addTodayRange(builder, root, predicates, "nextFollowAt");
        else if ("protected".equals(scene)) predicates.add(builder.equal(root.get("level"), "重点客户"));
        else if ("duplicate-unfollowed".equals(scene)) predicates.add(builder.and(
                builder.greaterThan(root.get("registrationCount"), 1), builder.isNull(root.get("lastContactAt"))));
        else if ("pool-claimed".equals(scene)) predicates.add(builder.and(
                builder.notEqual(root.get("owner"), PUBLIC_POOL), builder.isNotNull(root.get("previousOwner"))));
        else if ("new-unfollowed".equals(scene)) predicates.add(builder.isNull(root.get("lastContactAt")));
        else if ("two-days-unfollowed".equals(scene)) {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(2);
            predicates.add(builder.or(builder.lessThanOrEqualTo(root.get("lastContactAt"), cutoff),
                    builder.and(builder.isNull(root.get("lastContactAt")), builder.lessThanOrEqualTo(root.get("createdAt"), cutoff))));
        }
    }

    private void addTodayRange(jakarta.persistence.criteria.CriteriaBuilder builder,
            jakarta.persistence.criteria.From<?, Customer> root, List<Predicate> predicates, String field) {
        LocalDateTime start = LocalDateTime.now().toLocalDate().atStartOfDay();
        predicates.add(builder.and(builder.greaterThanOrEqualTo(root.get(field), start),
                builder.lessThan(root.get(field), start.plusDays(1))));
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
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+(?:\\.\\d+)?").matcher(value);
            List<Double> numbers = new java.util.ArrayList<>();
            while (matcher.find()) numbers.add(Double.parseDouble(matcher.group()));
            if (numbers.isEmpty()) return true;
            double multiplier = value.contains("万") ? 10000d : 1d;
            double lower = numbers.getFirst() * multiplier;
            double upper = numbers.getLast() * multiplier;
            if (hasMin && upper < Double.parseDouble(min.trim())) return false;
            if (hasMax && lower > Double.parseDouble(max.trim())) return false;
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
