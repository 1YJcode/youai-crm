package com.youai.crm.communication;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TelephonyWorker {
    private final CallRecordRepository calls;
    private final TelephonyService service;
    private final TelephonyGatewayClient gateway;
    private final SmsGatewayClient phones;
    private final TransactionTemplate transaction;

    public TelephonyWorker(CallRecordRepository calls, TelephonyService service, TelephonyGatewayClient gateway,
            SmsGatewayClient phones, PlatformTransactionManager manager) {
        this.calls = calls;
        this.service = service;
        this.gateway = gateway;
        this.phones = phones;
        this.transaction = new TransactionTemplate(manager);
    }

    @Scheduled(scheduler = "communicationTaskScheduler", fixedDelayString = "${telephony.gateway.poll-ms:2000}", initialDelayString = "${telephony.gateway.initial-ms:15000}")
    public void dispatch() {
        if (!gateway.enabled()) return;
        LocalDateTime now = LocalDateTime.now();
        transaction.executeWithoutResult(tx -> calls.recoverGatewayCalls(now, now.minusMinutes(2)));
        LocalDateTime browserCutoff = now.minusSeconds(90);
        for (CallRecord stale : calls.findTop20ByGatewayStateInAndBrowserLastSeenAtBeforeOrderByIdAsc(
                List.of("QUEUED", "SUBMITTING", "ACCEPTED", "CONNECTED", "UNKNOWN"), browserCutoff)) {
            service.expireBrowser(stale.getId(), browserCutoff);
        }
        for (CallRecord pending : calls.findTop20ByHangupRequestedTrueOrderByIdAsc()) {
            CallRecord call = service.takeHangup(pending.getId());
            if (call == null) continue;
            try {
                service.receipt(call.getId(), gateway.hangup(call));
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception error) {
                service.queried(call.getId());
            }
        }
        for (CallRecord call : calls.findTop20ByGatewayStateOrderByIdAsc("QUEUED")) {
            Integer claimed = transaction.execute(tx -> calls.claimGatewayCall(call.getId(), LocalDateTime.now()));
            if (!Integer.valueOf(1).equals(claimed)) continue;
            try {
                service.receipt(call.getId(), gateway.submit(call, phones.normalizePhone(call.getPhone())));
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                service.unknown(call.getId());
                return;
            } catch (Exception error) {
                service.unknown(call.getId());
            }
        }
        for (CallRecord call : calls.findTop20ByGatewayStateInAndGatewayUpdatedAtBeforeOrderByGatewayUpdatedAtAsc(
                List.of("ACCEPTED", "RINGING", "CONNECTED", "UNKNOWN"), LocalDateTime.now().minusSeconds(10))) {
            try {
                service.receipt(call.getId(), gateway.query(call));
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception error) {
                service.queried(call.getId());
            }
        }
    }
}
