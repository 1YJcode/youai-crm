package com.youai.crm.communication;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.youai.crm.customer.CustomerService;

@RestController @RequestMapping("/api/recording-uploads")
public class RecordingDirectUploadController {
    private final RecordingStorage storage;
    private final RecordingUploadRepository uploads;
    private final CustomerService customers;
    private final CallRecordRepository calls;
    private final CallRecordingController recordings;
    private final CustomerCallController customerCalls;
    private final com.youai.crm.account.AccessPolicy userAccess;
    private final RecordingAutomation automation;
    private final java.util.concurrent.Semaphore validations=new java.util.concurrent.Semaphore(1);
    public RecordingDirectUploadController(RecordingStorage storage,RecordingUploadRepository uploads,CustomerService customers,
            CallRecordRepository calls,CallRecordingController recordings,CustomerCallController customerCalls,com.youai.crm.account.AccessPolicy userAccess,RecordingAutomation automation) {
        this.storage=storage; this.uploads=uploads; this.customers=customers; this.calls=calls;
        this.recordings=recordings; this.customerCalls=customerCalls;
        this.userAccess=userAccess;
        this.automation=automation;
    }
    public record Request(String customerId,Long callId,long size,String note,boolean autoProcess,boolean processingConsent) {}
    public record Ticket(boolean direct,String id,String url) {}
    @PostMapping @Transactional
    public Ticket prepare(@RequestBody Request request,Authentication auth,jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        String customerId=request.customerId();
        if(request.callId()!=null) customerId=calls.findById(request.callId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"未找到通话记录")).getCustomerNo();
        if(customerId==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"请选择客户");
        customers.find(customerId,auth);
        automation.validate(request.autoProcess(),request.processingConsent());
        if(request.size()<=0 || request.size()>CallRecordingController.MAX_BYTES)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"请选择非空且不超过200MB的录音");
        String note=request.note()==null?"":request.note().trim();
        if(note.length()>1000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"备注不能超过1000个字符");
        if(!"s3".equals(storage.backend())) return new Ticket(false,null,null);
        var upload=new RecordingUpload(); upload.id=UUID.randomUUID().toString(); upload.actor=auth.getName();
        upload.actorUserId=userAccess.currentUserId(auth);
        upload.customerId=customerId; upload.callId=request.callId(); upload.size=request.size(); upload.note=note;
        upload.autoProcess=request.autoProcess();upload.processingConsent=request.processingConsent();
        upload.expiresAt=LocalDateTime.now().plusMinutes(30); uploads.saveAndFlush(upload);
        return new Ticket(true,upload.id,storage.presignUpload("uploads/"+upload.id,upload.size));
    }
    @PostMapping("/{id}/complete") @Transactional(rollbackFor=Exception.class)
    public Object complete(@PathVariable String id,Authentication auth) throws IOException {
        var upload=uploads.lockById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"上传凭证不存在"));
        if(!upload.actorUserId.equals(userAccess.currentUserId(auth))) throw new org.springframework.security.access.AccessDeniedException("上传凭证不属于当前用户");
        customers.find(upload.customerId,auth);
        if(upload.completed || upload.expiresAt.isBefore(LocalDateTime.now()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"上传凭证已完成或过期，请重新上传");
        if(!validations.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"正在校验其他录音，请稍后重试");
        Path temporary=null;
        try {
            temporary=Files.createTempFile("youai-direct-",".audio");
            storage.receive("uploads/"+id,upload.size,temporary);
            // Copy validated bytes to a new immutable key. A reusable PUT URL cannot alter saved recordings.
            var file=new FileUpload(temporary,upload.size);
            Object result=upload.callId==null?customerCalls.upload(upload.customerId,file,upload.note,auth)
                    :recordings.upload(upload.callId,file,auth);
            Long callId=result instanceof CallResponse call?call.id():((CallRecordingController.RecordingInfo)result).callId();
            automation.enqueue(callId,upload.autoProcess,upload.processingConsent,auth);
            upload.completed=true; uploads.save(upload);
            return result;
        } finally {
            if(temporary!=null) Files.deleteIfExists(temporary);
            validations.release();
        }
    }
    private record FileUpload(Path path,long size) implements MultipartFile {
        public String getName() { return "file"; }
        public String getOriginalFilename() { return "recording"; }
        public String getContentType() { return "application/octet-stream"; }
        public boolean isEmpty() { return size==0; }
        public long getSize() { return size; }
        public byte[] getBytes() throws IOException { return Files.readAllBytes(path); }
        public InputStream getInputStream() throws IOException { return Files.newInputStream(path); }
        public void transferTo(File dest) throws IOException { Files.copy(path,dest.toPath(),StandardCopyOption.REPLACE_EXISTING); }
    }
}
