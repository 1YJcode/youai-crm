package com.youke.crm.communication;

import java.time.LocalDateTime;
import java.util.List;

import com.youke.crm.account.AccessPolicy;
import com.youke.crm.common.NotFoundException;
import com.youke.crm.customer.CustomerResponse;
import com.youke.crm.customer.CustomerService;
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
    private final AccessPolicy accessPolicy;

    public ConversationService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            CustomerService customerService,
            AccessPolicy accessPolicy) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.customerService = customerService;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ConversationResponse> list(Authentication authentication) {
        accessPolicy.scopedOwner(authentication); // also rejects direct calls without an identity
        return conversationRepository.findAllByOrderByLastMessageAtDesc().stream()
                .filter(conversation -> accessPolicy.canAccessOwner(conversation.getOwner(), authentication))
                .map(this::response)
                .toList();
    }

    public ConversationResponse create(String customerNo, Authentication authentication) {
        CustomerResponse customer = customerService.find(customerNo, authentication);
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
        accessPolicy.requireOwner(conversation.getOwner(), authentication);
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
        Conversation conversation = get(conversationId, authentication);
        CrmMessage message = new CrmMessage();
        message.setConversationId(conversationId);
        message.setSender(accessPolicy.currentDisplayName(authentication));
        message.setDirection("OUTBOUND");
        message.setContent(request.content().trim());
        message.setSentAt(LocalDateTime.now());
        message.setRead(true);
        CrmMessage saved = messageRepository.save(message);
        conversation.setLastMessageAt(saved.getSentAt());
        conversationRepository.save(conversation);
        return MessageResponse.from(saved);
    }

    public void markAllRead(Authentication authentication) {
        for (Conversation conversation : visibleConversations(authentication)) {
            List<CrmMessage> messages = messageRepository.findByConversationIdOrderBySentAtAsc(conversation.getId());
            messages.stream()
                    .filter(message -> INBOUND.equals(message.getDirection()) && !message.isRead())
                    .forEach(message -> message.setRead(true));
            messageRepository.saveAll(messages);
        }
    }

    private List<Conversation> visibleConversations(Authentication authentication) {
        accessPolicy.scopedOwner(authentication); // also rejects direct calls without an identity
        return conversationRepository.findAllByOrderByLastMessageAtDesc().stream()
                .filter(conversation -> accessPolicy.canAccessOwner(conversation.getOwner(), authentication))
                .toList();
    }

    private Conversation get(Long id, Authentication authentication) {
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("未找到会话：" + id));
        accessPolicy.requireOwner(conversation.getOwner(), authentication);
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
