package com.youai.crm.communication;

import com.youai.crm.account.*;
import com.youai.crm.customer.CustomerService;
import java.io.IOException;
import java.time.LocalDateTime;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class CustomerCallController {
    private final CallRecordRepository calls;
    private final CustomerService customers;
    private final AccessPolicy access;
    private final CallRecordingController recordings;
    private final CallService callService;
    private final RecordingAutomation automation;
    public CustomerCallController(CallRecordRepository calls, CustomerService customers, AccessPolicy access,
            CallRecordingController recordings, CallService callService,RecordingAutomation automation) {
        this.calls = calls; this.customers = customers; this.access = access;
        this.recordings = recordings; this.callService = callService;
        this.automation=automation;
    }

    @GetMapping("/customers/{customerId}/calls")
    public Page<CallResponse> list(@PathVariable String customerId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        customers.find(customerId, authentication);
        var records = calls.findAll((root, query, builder) -> builder.equal(root.get("customerNo"), customerId),
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)),
                        Sort.by(Sort.Order.desc("startedAt"), Sort.Order.desc("id"))));
        return callService.responses(records, authentication);
    }

    @PostMapping(value = "/call-uploads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional(rollbackFor = Exception.class)
    public CallResponse uploadRequest(@RequestParam String customerId,@RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue="") String note,@RequestParam(defaultValue="false") boolean autoProcess,
            @RequestParam(defaultValue="false") boolean processingConsent,Authentication authentication)throws IOException {
        customers.find(customerId,authentication);automation.validate(autoProcess,processingConsent);
        var result=upload(customerId,file,note,authentication);automation.enqueue(result.id(),autoProcess,processingConsent,authentication);return result;
    }
    @Transactional(rollbackFor = Exception.class)
    public CallResponse upload(@RequestParam String customerId, @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "") String note,
            Authentication authentication) throws IOException {
        var customer = customers.find(customerId, authentication);
        Long uploader = access.currentUserId(authentication);
        if (note.length() > 1000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "备注不能超过1000个字符");
        var call = new CallRecord();
        call.setCustomerNo(customer.id()); call.setCustomerName(customer.name()); call.setPhone(customer.phone());
        call.setOwner(customer.owner()); call.setAgent(access.currentDisplayName(authentication)); call.setAgentUserId(uploader);
        call.setUploadedByUserId(uploader); call.setRecordingSource("UPLOAD");
        call.setDirection("外部通话"); call.setStatus("已接通"); call.setStartedAt(LocalDateTime.now());
        call.setNote(note.trim());
        call = calls.saveAndFlush(call);
        recordings.upload(call.getId(), file, authentication);
        return callService.response(call, authentication);
    }
}
