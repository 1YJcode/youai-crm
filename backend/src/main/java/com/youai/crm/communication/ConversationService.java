package com.youai.crm.communication;

import com.youai.crm.customer.CustomerAccessPolicy;
import com.youai.crm.common.ConflictException;

import java.time.LocalDateTime;
import java.util.List;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.common.NotFoundException;
import com.youai.crm.customer.CustomerResponse;
import com.youai.crm.customer.Customer;
import com.youai.crm.customer.CustomerRepository;
import com.youai.crm.customer.CustomerService;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ConversationService {

    private static final String INBOUND = "INBOUND";
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final AccessPolicy accessPolicy;
    private final SmsGatewayClient smsGateway;
    private final CustomerAccessPolicy customerAccess;

    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            CustomerService customerService,
            CustomerRepository customerRepository,
            AccessPolicy accessPolicy, CustomerAccessPolicy customerAccess, SmsGatewayClient smsGateway) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.customerService = customerService;
        this.customerRepository = customerRepository;
        this.accessPolicy = accessPolicy;
        this.smsGateway = smsGateway;
        this.customerAccess = customerAccess;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ConversationResponse> list(Authentication authentication) {
        accessPolicy.scopedOwner(authentication); // also rejects direct calls without an identity
        return conversationRepository.findAllByOrderByLastMessageAtDesc().stream()
                .filter(conversation -> customerAccess.canAccess(conversation.getCustomerNo(), authentication))
                .map(this::response)
                .toList();
    }

    public ConversationResponse create(String customerNo, Authentication authentication) {
        CustomerResponse customer = customerService.find(customerNo, authentication);
        customerRepository.lockByCustomerNo(customerNo).orElseThrow(() -> new NotFoundException("未找到客户"));
        Conversation conversation = conversationRepository.findByCustomerNo(customerNo)
                .orElseGet(() -> {
                    Conversation created = new Conversation();
                    created.setCustomerNo(customer.id());
                    created.setCustomerName(customer.name());
                    created.setCompany(customer.company());
                    created.setOwner(customer.owner());
                    created.setLastMessageAt(LocalDateTime.now());
                    return conversationRepository.save(created);
                });
        customerAccess.requireAccess(conversation.getCustomerNo(), authentication);
        return response(conversation);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<MessageResponse> messages(Long conversationId, Authentication authentication) {
        get(conversationId, authentication);
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId).stream()
                .map(MessageResponse::from)
                .toList();
    }

    public MessageResponse send(Long conversationId, MessageRequest request, Authentication authentication) {
        get(conversationId, authentication);
        Conversation conversation = conversationRepository.findLockedById(conversationId)
                .orElseThrow(() -> new NotFoundException("未找到会话"));
        customerAccess.requireAccess(conversation.getCustomerNo(), authentication);
        boolean sms = "SMS".equals(request.channel());
        Customer customer = customerRepository.findByCustomerNo(conversation.getCustomerNo())
                .orElseThrow(() -> new NotFoundException("未找到客户"));
        if (sms && request.requestId() == null) throw new IllegalArgumentException("短信发送需要请求编号");
        if (sms) {
            CrmMessage existing = messageRepository.findByConversationIdAndRequestKey(conversationId, request.requestId()).orElse(null);
            if (existing != null) {
                if (!"OUTBOUND".equals(existing.getDirection()) || !existing.getContent().equals(request.content().trim()))
                    throw new ConflictException("请求编号已用于其他消息");
                return MessageResponse.from(existing);
            }
            smsGateway.requireConfigured();
        }
        CrmMessage message = new CrmMessage();
        if (sms) {
            message.setRecipientPhone(smsGateway.normalizePhone(customer.getPhone()));
            message.setRequestKey(request.requestId());
            message.setDeliveryStatus("QUEUED");
            message.setDeliveryUpdatedAt(LocalDateTime.now());
        }
        message.setConversationId(conversationId);
        message.setSender(accessPolicy.currentDisplayName(authentication));
        message.setDirection("OUTBOUND");
        message.setContent(request.content().trim());
        message.setSentAt(LocalDateTime.now());
        message.setRead(true);
        CrmMessage saved = messageRepository.save(message);
        conversation.setLastMessageAt(saved.getSentAt());
        conversationRepository.save(conversation);
        if (!sms) {
            customer.setLastContactAt(saved.getSentAt());
            customerRepository.save(customer);
        }
        return MessageResponse.from(saved);
    }

    public void markAllRead(Authentication authentication) {
        for (Conversation conversation : visibleConversations(authentication)) {
            markRead(conversation.getId(), authentication);
        }
    }

    public void markRead(Long conversationId, Authentication authentication) {
        get(conversationId, authentication);
        List<CrmMessage> unreadMessages = messageRepository.findByConversationIdOrderBySentAtAsc(conversationId).stream()
                .filter(message -> INBOUND.equals(message.getDirection()) && !message.isRead())
                .toList();
        unreadMessages.forEach(message -> message.setRead(true));
        messageRepository.saveAll(unreadMessages);
    }

    private List<Conversation> visibleConversations(Authentication authentication) {
        accessPolicy.scopedOwner(authentication); // also rejects direct calls without an identity
        return conversationRepository.findAllByOrderByLastMessageAtDesc().stream()
                .filter(conversation -> customerAccess.canAccess(conversation.getCustomerNo(), authentication))
                .toList();
    }

    private Conversation get(Long id, Authentication authentication) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("未找到会话：" + id));
        customerAccess.requireAccess(conversation.getCustomerNo(), authentication);
        return conversation;
    }

    private ConversationResponse response(Conversation conversation) {
        String preview = messageRepository.findTopByConversationIdOrderBySentAtDesc(conversation.getId())
                .map(CrmMessage::getContent)
                .orElse("暂无消息，点击开始沟通");
        long unread = messageRepository.countByConversationIdAndDirectionAndReadFalse(conversation.getId(), INBOUND);
        return new ConversationResponse(conversation.getId(), conversation.getCustomerNo(), conversation.getCustomerName(),
                conversation.getCompany(), conversation.getOwner(), preview, conversation.getLastMessageAt(), unread);
    }
}
