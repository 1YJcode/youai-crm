package com.youai.crm.communication;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/communications/events")
public class CommunicationEventsController {
    private final String secret;
    private final TelephonyService telephony;
    private final SmsInboxService sms;

    public CommunicationEventsController(@Value("${communication.webhook-token:}") String secret,
            TelephonyService telephony, SmsInboxService sms) {
        this.secret = secret;
        this.telephony = telephony;
        this.sms = sms;
    }

    private void authorize(String supplied) {
        if (secret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "通信事件接口尚未配置");
        if (supplied == null || !MessageDigest.isEqual(secret.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "通信事件凭证无效");
    }

    @PostMapping("/calls")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void call(@RequestHeader(value = "X-Communication-Token", required = false) String token,
            @Valid @RequestBody TelephonyEvent event) {
        authorize(token);
        telephony.event(event);
    }

    @PostMapping("/sms")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void sms(@RequestHeader(value = "X-Communication-Token", required = false) String token,
            @Valid @RequestBody SmsInboundEvent event) {
        authorize(token);
        sms.receive(event);
    }
}
