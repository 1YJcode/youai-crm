package com.youai.crm.communication;

import java.time.LocalDateTime;
import java.util.List;
import com.youai.crm.customer.CustomerRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SmsDeliveryWorker {
    private final MessageRepository messages;
    private final ConversationRepository conversations;
    private final CustomerRepository customers;
    private final SmsGatewayClient gateway;
    private final TransactionTemplate transaction;

    public SmsDeliveryWorker(MessageRepository messages, ConversationRepository conversations,
            CustomerRepository customers, SmsGatewayClient gateway, PlatformTransactionManager manager) {
        this.messages = messages;
        this.conversations = conversations;
        this.customers = customers;
        this.gateway = gateway;
        this.transaction = new TransactionTemplate(manager);
    }

    @Scheduled(scheduler = "communicationTaskScheduler", fixedDelayString = "${sms.gateway.poll-ms:5000}", initialDelayString = "${sms.gateway.initial-ms:15000}")
    public void dispatch() {
        if (!gateway.enabled()) return;
        LocalDateTime now = LocalDateTime.now();
        transaction.executeWithoutResult(tx -> messages.recoverSms(now, now.minusMinutes(2)));
        for (CrmMessage message : messages.findTop20ByDeliveryStatusOrderByIdAsc("QUEUED")) {
            Integer claimed = transaction.execute(tx -> messages.claimSms(message.getId(), LocalDateTime.now()));
            if (!Integer.valueOf(1).equals(claimed)) continue;
            SmsGatewayClient.Receipt receipt;
            try {
                receipt = gateway.send(message);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                complete(message, new SmsGatewayClient.Receipt("UNKNOWN", null, "发送结果待确认"));
                return;
            } catch (Exception error) {
                receipt = new SmsGatewayClient.Receipt("UNKNOWN", null, "发送结果待确认");
            }
            complete(message, receipt);
        }
        for (CrmMessage message : messages.findTop20ByDeliveryStatusInAndDeliveryUpdatedAtBeforeOrderByDeliveryUpdatedAtAsc(
                List.of("ACCEPTED", "UNKNOWN"), LocalDateTime.now().minusSeconds(30))) {
            try {
                complete(message, gateway.query(message));
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception error) {
                // Keep the last confirmed state. Never automatically resend an ambiguous request.
                complete(message, new SmsGatewayClient.Receipt(message.getDeliveryStatus(),
                        message.getGatewayMessageId(), message.getDeliveryError()));
            }
        }
    }

    private void complete(CrmMessage message, SmsGatewayClient.Receipt receipt) {
        transaction.executeWithoutResult(tx -> {
            // Receipt queries can overlap across instances. Final states stay final.
            String expected = "QUEUED".equals(message.getDeliveryStatus()) ? "SENDING" : message.getDeliveryStatus();
            int updated = messages.completeSms(message.getId(), expected, receipt.status(), receipt.messageId(), receipt.error(), LocalDateTime.now());
            if (updated == 1 && "DELIVERED".equals(receipt.status())) {
                conversations.findById(message.getConversationId()).flatMap(conversation ->
                        customers.findByCustomerNo(conversation.getCustomerNo())).ifPresent(customer -> {
                    if (customer.getLastContactAt() == null || customer.getLastContactAt().isBefore(message.getSentAt())) {
                        customer.setLastContactAt(message.getSentAt());
                        customers.save(customer);
                    }
                });
            }
        });
    }
}
