package com.youke.crm.customer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.youke.crm.account.AccessPolicy;
import com.youke.crm.communication.CallRecord;
import com.youke.crm.communication.CallRecordRepository;
import com.youke.crm.communication.ConversationRepository;
import com.youke.crm.communication.CrmMessage;
import com.youke.crm.communication.MessageRepository;
import com.youke.crm.task.FollowUpTask;
import com.youke.crm.task.FollowUpTaskRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class CustomerFollowUpService {

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final FollowUpTaskRepository taskRepository;
    private final CallRecordRepository callRepository;
    private final ConversationRepository conversationRepository;
        private final MessageRepository messageRepository;
        private final FollowUpAnnotationRepository annotationRepository;
    private final CustomerRegistrationEventRepository registrationEventRepository;
    private final CustomerAssignmentEventRepository assignmentEventRepository;
    private final CustomerOperationEventRepository operationEventRepository;
        private final AccessPolicy accessPolicy;

    public CustomerFollowUpService(
            CustomerService customerService,
            CustomerRepository customerRepository,
            FollowUpTaskRepository taskRepository,
            CallRecordRepository callRepository,
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            FollowUpAnnotationRepository annotationRepository,
            CustomerRegistrationEventRepository registrationEventRepository,
            CustomerAssignmentEventRepository assignmentEventRepository,
            CustomerOperationEventRepository operationEventRepository,
            AccessPolicy accessPolicy) {
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.taskRepository = taskRepository;
        this.callRepository = callRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.annotationRepository = annotationRepository;
        this.registrationEventRepository = registrationEventRepository;
        this.assignmentEventRepository = assignmentEventRepository;
        this.operationEventRepository = operationEventRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<FollowUpRecordResponse> list(String customerNo, String type, String keyword,
                                             LocalDate from, LocalDate to, Authentication authentication) {
        CustomerResponse customer = customerService.find(customerNo, authentication);
        String normalizedType = StringUtils.hasText(type) ? type.trim().toLowerCase(Locale.ROOT) : "all";
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        LocalDateTime fromAt = from == null ? null : from.atStartOfDay();
        LocalDateTime toAt = to == null ? null : to.plusDays(1).atStartOfDay();
        Long customerId = customerRepository.findByCustomerNo(customer.id())
                .orElseThrow(() -> new com.youke.crm.common.NotFoundException("未找到客户：" + customer.id()))
                .getId();
        List<FollowUpRecordResponse> records = new ArrayList<>();

        taskRepository.findAll().stream()
                .filter(task -> sameCustomer(task, customer))
                .filter(task -> accessPolicy.canAccessOwner(task.getOwner(), authentication))
                .map(task -> taskRecord(task, customer))
                .filter(record -> matches(record, normalizedType, normalizedKeyword, fromAt, toAt))
                .forEach(records::add);

        callRepository.findAllByOrderByStartedAtDesc().stream()
                .filter(call -> Objects.equals(call.getCustomerNo(), customer.id()))
                .filter(call -> accessPolicy.canAccessOwner(call.getOwner(), authentication))
                .map(call -> callRecord(call, customer))
                .filter(record -> matches(record, normalizedType, normalizedKeyword, fromAt, toAt))
                .forEach(records::add);

        conversationRepository.findByCustomerNo(customer.id()).stream()
                .filter(conversation -> accessPolicy.canAccessOwner(conversation.getOwner(), authentication))
                .flatMap(conversation -> messageRepository.findByConversationIdOrderBySentAtAsc(conversation.getId()).stream())
                .map(message -> messageRecord(message, customer))
                .filter(record -> matches(record, normalizedType, normalizedKeyword, fromAt, toAt))
                .forEach(records::add);

        registrationEventRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(event -> registrationRecord(event, customer))
                .filter(record -> matches(record, normalizedType, normalizedKeyword, fromAt, toAt))
                .forEach(records::add);

        assignmentEventRepository.findAllByCustomerIdOrderByAssignedAtDesc(customerId).stream()
                .map(event -> assignmentRecord(event, customer))
                .filter(record -> matches(record, normalizedType, normalizedKeyword, fromAt, toAt))
                .forEach(records::add);

        operationEventRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(event -> operationRecord(event, customer))
                .filter(record -> matches(record, normalizedType, normalizedKeyword, fromAt, toAt))
                .forEach(records::add);

        return records.stream()
                .sorted(Comparator.comparing(FollowUpRecordResponse::occurredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<FollowUpAnnotationResponse> annotations(String customerNo, Authentication authentication) {
        customerService.find(customerNo, authentication);
        String author = accessPolicy.currentDisplayName(authentication);
        return annotationRepository.findAllByCustomerNoAndAuthor(customerNo, author).stream()
                .map(FollowUpAnnotationResponse::from)
                .toList();
    }

    public FollowUpAnnotationResponse saveAnnotation(String customerNo, FollowUpAnnotationRequest request,
                                                     Authentication authentication) {
        customerService.find(customerNo, authentication);
        String author = accessPolicy.currentDisplayName(authentication);
        String recordType = request.recordType().trim().toLowerCase(Locale.ROOT);
        FollowUpAnnotation annotation = annotationRepository
                .findByCustomerNoAndRecordIdAndRecordTypeAndAuthor(customerNo, request.recordId().trim(), recordType, author)
                .orElseGet(FollowUpAnnotation::new);
        annotation.setCustomerNo(customerNo);
        annotation.setRecordId(request.recordId().trim());
        annotation.setRecordType(recordType);
        annotation.setAuthor(author);
        annotation.setFavorite(request.favorite());
        annotation.setComment(StringUtils.hasText(request.comment()) ? request.comment().trim() : null);
        return FollowUpAnnotationResponse.from(annotationRepository.save(annotation));
    }

    private boolean sameCustomer(FollowUpTask task, CustomerResponse customer) {
        return Objects.equals(task.getCustomerId(), customer.id())
                || (!StringUtils.hasText(task.getCustomerId()) && Objects.equals(task.getCustomerName(), customer.name()));
    }

    private FollowUpRecordResponse taskRecord(FollowUpTask task, CustomerResponse customer) {
        LocalDateTime occurredAt = task.getFollowedAt() != null ? task.getFollowedAt() : task.getCreatedAt();
        return new FollowUpRecordResponse("task:" + task.getId(), customer.id(), customer.name(), "task",
                StringUtils.hasText(task.getTaskType()) ? task.getTaskType() : "跟进", task.getTitle(), task.getTitle(),
                task.getCustomerStatus(), task.getOwner(), occurredAt, task.isCompleted(), "operation");
    }

    private FollowUpRecordResponse callRecord(CallRecord call, CustomerResponse customer) {
        String direction = StringUtils.hasText(call.getDirection()) ? call.getDirection() : "通话";
        return new FollowUpRecordResponse("call:" + call.getId(), customer.id(), customer.name(), "call", direction,
                "客户通话", StringUtils.hasText(call.getNote()) ? call.getNote() : "暂无沟通备注", null,
                call.getAgent(), call.getStartedAt(), false, "operation");
    }

    private FollowUpRecordResponse messageRecord(CrmMessage message, CustomerResponse customer) {
        String direction = "OUTBOUND".equalsIgnoreCase(message.getDirection()) ? "发送消息" : "微信";
        return new FollowUpRecordResponse("message:" + message.getId(), customer.id(), customer.name(), "message", direction,
                "客户消息", message.getContent(), null, message.getSender(), message.getSentAt(), false, "operation");
    }

    private FollowUpRecordResponse registrationRecord(CustomerRegistrationEvent event, CustomerResponse customer) {
        String source = StringUtils.hasText(event.getSource()) ? event.getSource() : "系统";
        boolean first = event.getRegistrationNumber() != null && event.getRegistrationNumber() == 1;
        String title = first && "批量导入".equals(source) ? "首次导入系统" : first ? "首次登记" : "重复注册";
        String content = first
                ? "客户首次进入系统，来源：" + source
                : "检测到客户第" + event.getRegistrationNumber() + "次注册，来源：" + source;
        return new FollowUpRecordResponse("registration:" + event.getId(), customer.id(), customer.name(), "system",
                "系统跟进", title, content, null, event.getOperator(), event.getCreatedAt(), false, "system");
    }

    private FollowUpRecordResponse assignmentRecord(CustomerAssignmentEvent event, CustomerResponse customer) {
        StringBuilder content = new StringBuilder(event.getPreviousOwner()).append(" → ").append(event.getOwner());
        if (StringUtils.hasText(event.getType())) content.append("，类型：").append(event.getType());
        if (StringUtils.hasText(event.getMaturity())) content.append("，成熟度：").append(event.getMaturity());
        if (StringUtils.hasText(event.getReason())) content.append("，原因：").append(event.getReason());
        return new FollowUpRecordResponse("assignment:" + event.getId(), customer.id(), customer.name(), "operation",
                "管理员操作", "调整客户归属", content.toString(), null, event.getOperator(), event.getAssignedAt(), false, "operation");
    }

    private FollowUpRecordResponse operationRecord(CustomerOperationEvent event, CustomerResponse customer) {
        return new FollowUpRecordResponse("operation:" + event.getId(), customer.id(), customer.name(), "operation",
                "员工及管理员操作", event.getOperationType(), event.getDetail(), null, event.getOperator(),
                event.getCreatedAt(), false, "operation");
    }

    private boolean matches(FollowUpRecordResponse record, String type, String keyword,
                            LocalDateTime from, LocalDateTime to) {
        if (from != null && (record.occurredAt() == null || record.occurredAt().isBefore(from))) return false;
        if (to != null && (record.occurredAt() == null || !record.occurredAt().isBefore(to))) return false;
        String haystack = String.join(" ", safe(record.channel()), safe(record.title()), safe(record.content()), safe(record.customerStatus()), safe(record.owner())).toLowerCase(Locale.ROOT);
        if (!matchesBusinessType(record, type, haystack)) return false;
        return keyword == null || keyword.isBlank() || haystack.contains(keyword.toLowerCase(Locale.ROOT));
    }

    private boolean matchesBusinessType(FollowUpRecordResponse record, String type, String haystack) {
        if (!StringUtils.hasText(type) || "all".equalsIgnoreCase(type)) return true;
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "sales" -> "task".equals(record.type()) || "call".equals(record.type()) || containsAny(haystack, "销售", "跟进");
            case "invitation" -> containsAny(haystack, "邀约", "到访", "见面");
            case "service" -> containsAny(haystack, "服务", "售后", "开通");
            case "system" -> "system".equals(record.type()) || "system".equals(record.category());
            case "custom" -> "custom".equals(record.category());
            case "wechat_customer_lost" -> containsAny(haystack, "客户流失", "流失");
            case "wechat_add_friend" -> containsAny(haystack, "添加客户好友", "添加好友", "加好友");
            default -> type.equalsIgnoreCase(record.type());
        };
    }

    private boolean containsAny(String haystack, String... values) {
        for (String value : values) if (haystack.contains(value)) return true;
        return false;
    }

    private String safe(String value) { return value == null ? "" : value; }}
