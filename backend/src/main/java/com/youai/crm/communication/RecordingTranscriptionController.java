package com.youai.crm.communication;

import com.youai.crm.common.NotFoundException;
import com.youai.crm.customer.CustomerAccessPolicy;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/calls/{callId}/recording/transcriptions/{provider}")
public class RecordingTranscriptionController {
    private final CallRecordRepository calls;
    private final CallRecordingRepository recordings;
    private final RecordingTranscriptionRepository results;
    private final CustomerAccessPolicy access;
    private final RecordingQueue queue;
    private final RecordingKnowledgeService knowledge;
    private final CustomerTranscriptSegmentRepository segments;

    private final LocalTranscriptionClient local;
    private final QwenTranscriptionClient cloud;
    public RecordingTranscriptionController(CallRecordRepository calls, CallRecordingRepository recordings,
            RecordingTranscriptionRepository results, CustomerAccessPolicy access,
            LocalTranscriptionClient local, QwenTranscriptionClient cloud, RecordingQueue queue, RecordingKnowledgeService knowledge,CustomerTranscriptSegmentRepository segments) {
        this.queue=queue;
        this.knowledge=knowledge;
        this.segments=segments;
        this.calls=calls; this.recordings=recordings; this.results=results; this.access=access; this.local=local; this.cloud=cloud;
    }
    public record View(boolean configured,String status,String transcript,String summary,String error,Long segmentId) {
        public View(boolean configured,String status,String transcript,String summary,String error){this(configured,status,transcript,summary,error,null);}
    }
    public record Consent(boolean cloudConsent,boolean analysisConsent) {}
    private boolean configured(String provider) {
        return switch(provider) { case "local" -> local.configured(); case "cloud" -> cloud.configured();
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"请选择本地或云端转写"); };
    }
    private String authorize(Long id,Authentication auth) {
        var call=calls.findById(id).orElseThrow(() -> new NotFoundException("未找到通话记录"));
        access.requireAccess(call.getCustomerNo(),auth);
        return recordings.findRevision(id).orElse(null);
    }
    @GetMapping
    public View get(@PathVariable Long callId,@PathVariable String provider,Authentication auth,
                    jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control","no-store");
        boolean ready=configured(provider);
        var audio=authorize(callId,auth);
        if(audio==null) return new View(ready,"NO_RECORDING",null,null,null);
        var row=results.findById(callId+":"+provider).orElse(null);
        if(row==null || !row.revision.equals(audio)) return new View(ready,"PENDING",null,null,null);
        boolean expired=row.activeJobId==null && "PROCESSING".equals(row.status) && row.started!=null
                && row.started.isBefore(LocalDateTime.now().minusMinutes(120));
        Long segmentId=segments.findByCallIdAndRecordingRevision(callId,audio).map(s->s.id).orElse(null);
        return new View(ready,expired?"FAILED":row.status,row.transcript,null,expired?"旧任务已中断，请重试":row.error,segmentId);
    }
    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED)
    public View start(@PathVariable Long callId,@PathVariable String provider,
                                   @RequestBody(required=false) Consent consent,Authentication auth) {
        boolean ready=configured(provider);
        var revisionCheck=authorize(callId,auth);
        if(revisionCheck==null) throw new NotFoundException("请先上传录音");
        if(!ready) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"所选转写服务尚未配置");
        if("cloud".equals(provider) && (consent==null || !consent.cloudConsent()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"请确认将录音发送至千问云端");
        queue.enqueue(callId,revisionCheck,provider,auth,consent!=null&&consent.analysisConsent());
        return new View(true,"QUEUED",null,null,null);
    }
    @PostMapping("/case")
    public java.util.Map<String,Long> sourceCase(@PathVariable Long callId,@PathVariable String provider,Authentication auth) {
        configured(provider);
        String revision=authorize(callId,auth);
        var row=results.findById(callId+":"+provider).orElseThrow(()->new NotFoundException("请先完成转写"));
        if(!"COMPLETED".equals(row.status) || !row.revision.equals(revision) || row.transcript==null || row.transcript.isBlank())
            throw new ResponseStatusException(HttpStatus.CONFLICT,"当前录音尚无可整理的转写");
        // Legacy rows do not retain completion time; do not invent one when backfilling a source case.
        Long id=knowledge.capture(callId,provider,revision,row.started,row.transcript,null,auth.getName(),false);
        if(id==null) throw new ResponseStatusException(HttpStatus.CONFLICT,"录音或转写已变化，请刷新");
        return java.util.Map.of("id",id);
    }
}
