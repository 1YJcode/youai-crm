package com.youai.crm.communication;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/telephony")
public class TelephonyController {
    private final TelephonyService service;
    private final TelephonyGatewayClient gateway;
    private final com.youai.crm.account.AccessPolicy access;

    public TelephonyController(TelephonyService service, TelephonyGatewayClient gateway, com.youai.crm.account.AccessPolicy access) {
        this.service = service;
        this.gateway = gateway;
        this.access = access;
    }

    @GetMapping("/browser-config")
    public Object config(Authentication auth) {
        if (!gateway.enabled()) return Map.of("configured", false, "mode", "WEBRTC");
        try {
            return gateway.browserConfig(access.currentUserId(auth));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "电话网关暂时不可用");
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "无法获取电话网关 WebRTC 配置");
        }
    }

    @GetMapping("/customer")
    public Map<String, String> customer(@RequestParam(required = false) String phone,
            @RequestParam(required = false) String customerId, Authentication auth) {
        return service.resolveCustomer(phone, customerId, auth);
    }

    @PostMapping("/calls")
    @ResponseStatus(HttpStatus.CREATED)
    public TelephonyService.BrowserCall dial(@Valid @RequestBody TelephonyRequest request, Authentication auth) {
        return service.create(request, auth);
    }

    @GetMapping("/calls/{id}")
    public TelephonyService.BrowserCall get(@PathVariable Long id, Authentication auth) { return service.get(id, auth); }

    @PostMapping("/calls/{id}/answer")
    public TelephonyService.BrowserCall answer(@PathVariable Long id, @Valid @RequestBody BrowserOfferRequest request, Authentication auth) {
        return service.answer(id, request, auth);
    }

    @PostMapping("/calls/{id}/hangup")
    public TelephonyService.BrowserCall hangup(@PathVariable Long id, Authentication auth) { return service.hangup(id, auth); }

    @GetMapping("/incoming")
    public List<CallResponse> incoming(Authentication auth) { return gateway.enabled() ? service.incoming(auth) : List.of(); }
}
