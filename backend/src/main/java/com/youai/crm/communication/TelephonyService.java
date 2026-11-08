package com.youai.crm.communication;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.common.ConflictException;
import com.youai.crm.common.NotFoundException;
import com.youai.crm.customer.Customer;
import com.youai.crm.customer.CustomerAccessPolicy;
import com.youai.crm.customer.CustomerRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TelephonyService {
    private static final Set<String> FINAL = Set.of("ENDED", "FAILED");
    private final CallRecordRepository calls;
    private final CustomerRepository customers;
    private final CrmUserRepository users;
    private final CustomerAccessPolicy customerAccess;
    private final AccessPolicy access;
    private final CallService callService;
    private final TelephonyGatewayClient gateway;
    private final SmsGatewayClient phones;
    private final GatewayPhoneMatcher matcher;

    public TelephonyService(CallRecordRepository calls, CustomerRepository customers, CrmUserRepository users,
            CustomerAccessPolicy customerAccess, AccessPolicy access, CallService callService,
            TelephonyGatewayClient gateway, SmsGatewayClient phones, GatewayPhoneMatcher matcher) {
        this.calls = calls;
        this.customers = customers;
        this.users = users;
        this.customerAccess = customerAccess;
        this.access = access;
        this.callService = callService;
        this.gateway = gateway;
        this.phones = phones;
        this.matcher = matcher;
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, String> resolveCustomer(String phone, String customerId, Authentication auth) {
        boolean byPhone = phone != null && !phone.isBlank();
        boolean byId = customerId != null && !customerId.isBlank();
        if (byPhone == byId) throw new IllegalArgumentException("请填写客户手机号或客户编号");
        Customer customer = byPhone ? matcher.uniqueCustomer(phone).orElseThrow(() -> new NotFoundException("未找到唯一的可拨打客户"))
                : customers.findByCustomerNo(customerId.trim()).orElseThrow(() -> new NotFoundException("未找到客户"));
        customerAccess.requireAccess(customer.getCustomerNo(), auth);
        return java.util.Map.of("id", customer.getCustomerNo(), "name", customer.getName());
    }

    public BrowserCall create(TelephonyRequest request, Authentication auth) {
        customerAccess.requireAccess(request.customerId(), auth);
        Long agentId = access.currentUserId(auth);
        users.lockById(agentId).orElseThrow();
        CallRecord existing = calls.findByAgentUserIdAndGatewayRequestKey(agentId, request.requestId()).orElse(null);
        if (existing != null) {
            if (!request.customerId().equals(existing.getCustomerNo())
                    || (!FINAL.contains(existing.getGatewayState()) && !request.offerSdp().equals(existing.getBrowserOffer())))
                throw new ConflictException("该请求编号已用于其他通话");
            return view(existing, auth);
        }
        gateway.requireConfigured();
        requireIdle(agentId, null);
        Customer customer = customers.lockByCustomerNo(request.customerId()).orElseThrow(() -> new NotFoundException("未找到客户"));
        customerAccess.requireAccess(request.customerId(), auth);
        CallRecord call = new CallRecord();
        call.setCustomerNo(customer.getCustomerNo());
        call.setCustomerName(customer.getName());
        call.setPhone(phones.normalizePhone(customer.getPhone()));
        call.setOwner(customer.getOwner());
        call.setAgent(access.currentDisplayName(auth));
        call.setAgentUserId(agentId);
        call.setDirection("呼出");
        call.setStatus("待拨号");
        call.setNote("浏览器 WebRTC 外呼");
        call.setGatewayState("QUEUED");
        call.setGatewayRequestKey(request.requestId());
        call.setBrowserOffer(request.offerSdp());
        call.setGatewayUpdatedAt(LocalDateTime.now());
        call.setBrowserLastSeenAt(LocalDateTime.now());
        return view(calls.saveAndFlush(call), auth);
    }

    public BrowserCall answer(Long id, BrowserOfferRequest request, Authentication auth) {
        gateway.requireConfigured();
        Long agentId = access.currentUserId(auth);
        users.lockById(agentId).orElseThrow();
        CallRecord call = locked(id);
        requireAgent(call, auth);
        if (!agentId.equals(call.getAgentUserId())) throw new ConflictException("只能在本人的坐席接听");
        if (!"呼入".equals(call.getDirection())) throw new ConflictException("只能接听呼入通话");
        if (request.offerSdp().equals(call.getBrowserOffer())) return view(call, auth);
        if (!"RINGING".equals(call.getGatewayState()) || call.getBrowserOffer() != null || call.getHangupRequested())
            throw new ConflictException("来电已被接听或已结束");
        requireIdle(agentId, call.getId());
        call.setBrowserOffer(request.offerSdp());
        call.setGatewayState("QUEUED");
        call.setStatus("接听中");
        call.setBrowserLastSeenAt(LocalDateTime.now());
        call.setGatewayUpdatedAt(LocalDateTime.now());
        return view(call, auth);
    }

    public BrowserCall hangup(Long id, Authentication auth) {
        CallRecord call = locked(id);
        requireAgent(call, auth);
        if ("LOCAL".equals(call.getGatewayState())) throw new ConflictException("此通话不属于电话网关");
        if (FINAL.contains(call.getGatewayState())) return view(call, auth);
        if ("呼出".equals(call.getDirection()) && "QUEUED".equals(call.getGatewayState())) {
            call.setGatewayState("ENDED");
            call.setStatus("已取消");
            clearMedia(call);
        } else {
            gateway.requireConfigured();
            call.setHangupRequested(true);
        }
        call.setGatewayUpdatedAt(LocalDateTime.now());
        return view(call, auth);
    }

    public BrowserCall get(Long id, Authentication auth) {
        CallRecord call = calls.findById(id).orElseThrow(() -> new NotFoundException("未找到通话"));
        requireAgent(call, auth);
        calls.touchBrowserCall(id, access.currentUserId(auth), LocalDateTime.now());
        return view(call, auth);
    }

    @Transactional(readOnly = true)
    public List<CallResponse> incoming(Authentication auth) {
        return calls.findTop20ByAgentUserIdAndDirectionAndGatewayStateInOrderByIdDesc(
                access.currentUserId(auth), "呼入", List.of("RINGING"))
                .stream().filter(call -> !call.getHangupRequested()).map(call -> callService.response(call, auth)).toList();
    }

    public void event(TelephonyEvent event) {
        gateway.requireConfigured();
        CallRecord call;
        if ("OUTBOUND".equals(event.direction())) {
            if (event.requestId() == null) throw new IllegalArgumentException("呼出事件缺少请求编号");
            call = locked(Long.valueOf(event.requestId().substring(5)));
            if (!"呼出".equals(call.getDirection()) || "LOCAL".equals(call.getGatewayState()))
                throw new ConflictException("通话方向或来源不匹配");
        } else {
            call = calls.findByGatewayCallId(event.callId()).orElse(null);
            if (call == null) call = inbound(event);
            if (!"呼入".equals(call.getDirection())) throw new ConflictException("通话方向不匹配");
        }
        if (event.agentUserId() != null && !event.agentUserId().equals(call.getAgentUserId()))
            throw new ConflictException("坐席不匹配");
        apply(call, event.receipt());
    }

    private CallRecord inbound(TelephonyEvent event) {
        var agent = users.findById(event.agentUserId() == null ? -1L : event.agentUserId())
                .filter(user -> user.isEnabled()).orElseThrow(() -> new IllegalArgumentException("呼入事件必须指定有效坐席"));
        CallRecord call = new CallRecord();
        call.setAgentUserId(agent.getId());
        call.setAgent(agent.getDisplayName());
        call.setDirection("呼入");
        call.setGatewayState("RINGING");
        call.setGatewayCallId(event.callId());
        call.setPhone(phones.normalizePhone(event.from()));
        call.setCustomerNo("");
        call.setCustomerName("未匹配客户");
        call.setOwner("");
        call.setStatus("振铃中");
        call.setNote("浏览器 WebRTC 来电");
        matcher.uniqueCustomer(event.from()).filter(customer -> agent.getId().equals(customer.getOwnerId())
                || customer.getCollaboratorIds().contains(agent.getId())).ifPresent(customer -> {
            call.setCustomerNo(customer.getCustomerNo());
            call.setCustomerName(customer.getName());
            call.setOwner(customer.getOwner());
        });
        call.setGatewayUpdatedAt(LocalDateTime.now());
        return calls.saveAndFlush(call);
    }

    public void receipt(Long id, TelephonyGatewayClient.Receipt receipt) { apply(locked(id), receipt); }

    public void unknown(Long id) {
        CallRecord call = locked(id);
        if ("SUBMITTING".equals(call.getGatewayState())) {
            call.setGatewayState("UNKNOWN");
            call.setStatus("结果待确认");
            call.setGatewayUpdatedAt(LocalDateTime.now());
        }
    }

    public void expireBrowser(Long id, LocalDateTime cutoff) {
        CallRecord call = locked(id);
        if (FINAL.contains(call.getGatewayState()) || call.getBrowserLastSeenAt() == null || !call.getBrowserLastSeenAt().isBefore(cutoff)) return;
        if ("呼出".equals(call.getDirection()) && "QUEUED".equals(call.getGatewayState())) {
            call.setGatewayState("ENDED");
            call.setStatus("已取消");
            clearMedia(call);
        } else call.setHangupRequested(true);
    }

    public CallRecord takeHangup(Long id) {
        CallRecord call = locked(id);
        if (!call.getHangupRequested()) return null;
        call.setHangupRequested(false);
        call.setGatewayUpdatedAt(LocalDateTime.now());
        return FINAL.contains(call.getGatewayState()) ? null : call;
    }

    public void queried(Long id) { locked(id).setGatewayUpdatedAt(LocalDateTime.now()); }

    private void apply(CallRecord call, TelephonyGatewayClient.Receipt receipt) {
        if (call.getGatewayCallId() != null && !call.getGatewayCallId().equals(receipt.callId()))
            throw new ConflictException("网关通话编号不匹配");
        if (FINAL.contains(call.getGatewayState()) || receipt.sequence() < call.getGatewaySequence()) return;
        if (receipt.sequence() == call.getGatewaySequence() && !receipt.state().equals(call.getGatewayState())) return;
        call.setGatewayCallId(receipt.callId());
        call.setGatewayUpdatedAt(LocalDateTime.now());
        if (receipt.answerSdp() != null && call.getBrowserOffer() != null && call.getBrowserAnswer() == null)
            call.setBrowserAnswer(receipt.answerSdp());
        // A late ringing/accepted receipt must not undo a confirmed connected state.
        if ("CONNECTED".equals(call.getGatewayState()) && Set.of("ACCEPTED", "RINGING", "UNKNOWN").contains(receipt.state())) return;
        call.setGatewaySequence(receipt.sequence());
        call.setGatewayState(receipt.state());
        call.setDurationSeconds(Math.max(call.getDurationSeconds(), receipt.durationSeconds()));
        if ("CONNECTED".equals(receipt.state()) || ("ENDED".equals(receipt.state()) && receipt.durationSeconds() > 0)) {
            if (call.getConnectedAt() == null) call.setConnectedAt(LocalDateTime.now());
            if (!call.getCustomerNo().isBlank()) customers.lockByCustomerNo(call.getCustomerNo()).ifPresent(customer -> {
                if (customer.getLastContactAt() == null || customer.getLastContactAt().isBefore(call.getConnectedAt()))
                    customer.setLastContactAt(call.getConnectedAt());
            });
        }
        call.setStatus(switch (receipt.state()) {
            case "ACCEPTED" -> "呼叫中";
            case "RINGING" -> "振铃中";
            case "CONNECTED" -> "已接通";
            case "ENDED" -> call.getConnectedAt() == null ? "未接通" : "已接通";
            case "FAILED" -> "未接通";
            default -> "结果待确认";
        });
        if (FINAL.contains(receipt.state())) {
            call.setHangupRequested(false);
            clearMedia(call);
        }
    }

    private void requireIdle(Long agentId, Long exceptId) {
        var active = calls.findByAgentUserIdAndGatewayStateIn(agentId,
                List.of("QUEUED", "SUBMITTING", "ACCEPTED", "CONNECTED", "UNKNOWN"));
        if (active.stream().anyMatch(call -> !call.getId().equals(exceptId)))
            throw new ConflictException("当前坐席已有进行中的通话，请先结束该通话");
    }

    private void clearMedia(CallRecord call) { call.setBrowserOffer(null); call.setBrowserAnswer(null); }
    private CallRecord locked(Long id) { return calls.lockById(id).orElseThrow(() -> new NotFoundException("未找到通话")); }
    private void requireAgent(CallRecord call, Authentication auth) { access.requireUserId(call.getAgentUserId(), auth); }
    private BrowserCall view(CallRecord call, Authentication auth) { return new BrowserCall(callService.response(call, auth), call.getBrowserAnswer(), call.getHangupRequested()); }
    public record BrowserCall(CallResponse call, String answerSdp, boolean hangupRequested) { }
}
