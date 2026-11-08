package com.youai.crm.communication;

import com.youai.crm.common.ConflictException;
import com.youai.crm.customer.CustomerRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SmsInboxService {
    private final SmsGatewayClient gateway;
    private final GatewayPhoneMatcher matcher;
    private final CustomerRepository customers;
    private final ConversationRepository conversations;
    private final MessageRepository messages;

    public SmsInboxService(SmsGatewayClient gateway, GatewayPhoneMatcher matcher, CustomerRepository customers,
            ConversationRepository conversations, MessageRepository messages) {
        this.gateway = gateway;
        this.matcher = matcher;
        this.customers = customers;
        this.conversations = conversations;
        this.messages = messages;
    }

    @Transactional
    public void receive(SmsInboundEvent event) {
        gateway.requireConfigured();
        var matched = matcher.uniqueCustomer(event.from()).orElseThrow(() -> new ConflictException("来信号码未匹配到唯一客户，请在网关中保留该消息待处理"));
        var customer = customers.lockByCustomerNo(matched.getCustomerNo()).orElseThrow();
        if (!gateway.normalizePhone(customer.getPhone()).equals(gateway.normalizePhone(event.from())))
            throw new ConflictException("客户手机号已更新，请重新处理来信");
        Conversation conversation = conversations.findByCustomerNo(customer.getCustomerNo()).orElseGet(() -> {
            var created = new Conversation();
            created.setCustomerNo(customer.getCustomerNo());
            created.setCustomerName(customer.getName());
            created.setCompany(customer.getCompany());
            created.setOwner(customer.getOwner());
            return conversations.saveAndFlush(created);
        });
        String key;
        try {
            key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(("inbound:" + event.messageId()).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
        var previous = messages.findByConversationIdAndRequestKey(conversation.getId(), key).orElse(null);
        if (previous != null) {
            if (!"INBOUND".equals(previous.getDirection()) || !event.content().equals(previous.getContent())) throw new ConflictException("来信编号已用于不同内容");
            return;
        }
        var message = new CrmMessage();
        message.setConversationId(conversation.getId());
        message.setSender(customer.getName());
        message.setDirection("INBOUND");
        message.setContent(event.content());
        message.setSentAt(LocalDateTime.now());
        message.setRead(false);
        message.setRequestKey(key);
        message.setGatewayMessageId(event.messageId());
        message.setDeliveryStatus("RECEIVED");
        messages.save(message);
        conversation.setLastMessageAt(message.getSentAt());
        customer.setLastContactAt(message.getSentAt());
    }
}
