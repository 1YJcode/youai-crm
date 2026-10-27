package com.youai.crm.communication;

import com.youai.crm.account.*;
import com.youai.crm.customer.*;
import com.youai.crm.task.*;
import com.youai.crm.common.*;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.*;
import java.util.*;

@Service
public class CallFollowupWorkService {
    private final CallFollowupWorkRepository work;
    private final CallRecordRepository calls;
    private final CustomerRepository customers;
    private final CustomerAccessPolicy access;
    private final AccessPolicy identities;
    private final CrmUserRepository users;
    private final CustomerTranscriptSegmentRepository segments;
    private final CallIntelligenceService analysis;
    private final TaskService tasks;
    private final SystemNotificationRepository notifications;
    private final TransactionTemplate tx;
    private final EntityManager em;
    @org.springframework.beans.factory.annotation.Autowired private com.youai.crm.knowledge.KnowledgeRepository articles;
    @org.springframework.beans.factory.annotation.Autowired private CallIntelligenceRepository intelligence;
    public CallFollowupWorkService(CallFollowupWorkRepository work,CallRecordRepository calls,CustomerRepository customers,
            CustomerAccessPolicy access,AccessPolicy identities,CrmUserRepository users,CustomerTranscriptSegmentRepository segments,
            CallIntelligenceService analysis,TaskService tasks,SystemNotificationRepository notifications,TransactionTemplate tx,EntityManager em){
        this.work=work;this.calls=calls;this.customers=customers;this.access=access;this.identities=identities;this.users=users;
        this.segments=segments;this.analysis=analysis;this.tasks=tasks;this.notifications=notifications;this.tx=tx;this.em=em;
    }
    static LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Shanghai"));}
    /** Call lock also serializes first upload, replacements and follow-up confirmation. No history backfill. */
    @Transactional public void register(Long callId){
        var call=calls.lockById(callId).orElseThrow();
        if(work.existsById(callId)||!"已接通".equals(call.getStatus()))return;
        var w=new CallFollowupWork();w.callId=callId;w.customerNo=call.getCustomerNo();
        w.startedAt="UPLOAD".equals(call.getRecordingSource())?call.getUploadedAt():call.getStartedAt().plusSeconds(call.getDurationSeconds());
        if(w.startedAt==null)w.startedAt=now();w.remindAt=w.startedAt.plusMinutes(30);w.escalateAt=w.startedAt.plusHours(24);work.save(w);
    }
    public record View(Long callId,String customerNo,String customerName,String salesName,LocalDateTime sourceTime,Integer durationSeconds,
            LocalDateTime remindAt,LocalDateTime escalateAt,Long followUpId,Long segmentId,String analysisState,CallIntelligenceService.View intelligence,
            String title,String followupStatus,String recordingRevision,String uploadedByName){}
    /** Read-only historical sources are not reminder work until explicitly selected. */
    @Transactional(readOnly=true) public List<View> sources(String customerNo,Authentication auth){
        access.requireAccess(customerNo,auth);var seen=new HashSet<Long>();var result=new ArrayList<View>();
        for(var s:segments.findByCustomerNo(customerNo,org.springframework.data.domain.PageRequest.of(0,100,org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC,"sourceTime")))){
            if(!seen.add(s.callId))continue;
            var w=work.findById(s.callId).orElse(null);
            if(w==null){w=new CallFollowupWork();w.callId=s.callId;w.customerNo=customerNo;}
            result.add(view(w,auth));
        }return result;
    }
    @Transactional public View useSource(Long segmentId,Authentication auth){
        var s=segments.findById(segmentId).orElseThrow(()->new NotFoundException("未找到来源转写"));
        var c=calls.lockById(s.callId).orElseThrow();access.requireAccess(c.getCustomerNo(),auth);
        var w=work.findById(s.callId).orElse(null);
        if((w!=null&&w.followUpId!=null)||!intelligence.confirmedForCall(s.callId).isEmpty())throw new ConflictException("这次通话已写跟进，请在跟进记录中查看");
        if(w==null){w=new CallFollowupWork();w.callId=s.callId;w.customerNo=s.customerNo;w.startedAt=now();w.remindAt=w.startedAt.plusMinutes(30);w.escalateAt=w.startedAt.plusHours(24);work.saveAndFlush(w);}
        return view(w,auth);
    }
    @Transactional(readOnly=true) public List<View> list(String customerNo,Authentication auth){
        identities.currentUserId(auth);if(customerNo!=null)access.requireAccess(customerNo,auth);
        return work.findByFollowUpIdIsNullOrderByStartedAtDesc().stream()
                .filter(w->(customerNo==null||customerNo.equals(w.customerNo))&&access.canAccess(w.customerNo,auth))
                .map(w->view(w,auth)).toList();
    }
    private View view(CallFollowupWork w,Authentication auth){
        var c=calls.findById(w.callId).orElseThrow();var s=w.followUpId!=null&&w.sourceSegmentId!=null?segments.findById(w.sourceSegmentId).orElse(null):segments.findFirstByCallIdOrderByIdDesc(w.callId).orElse(null);
        var i=s==null?null:analysis.get(s.id,auth);
        Long followUp=w.followUpId;if(followUp==null){var confirmed=intelligence.confirmedForCall(w.callId);if(!confirmed.isEmpty())followUp=confirmed.getFirst();}
        String title=i!=null&&i.analysis()!=null?i.analysis().path("title").asText(""):"";
        String uploader=c.getUploadedByUserId()==null?null:users.findById(c.getUploadedByUserId()).map(CrmUser::getDisplayName).orElse("历史上传人");
        return new View(w.callId,w.customerNo,c.getCustomerName(),c.getAgent(),"UPLOAD".equals(c.getRecordingSource())?c.getUploadedAt():c.getStartedAt(),
                c.getDurationSeconds(),w.remindAt,w.escalateAt,followUp,s==null?null:s.id,i==null?"WAITING_TRANSCRIPT":i.state(),i,
                title,followUp==null?"UNFOLLOWED":"FOLLOWED",s==null?null:s.recordingRevision,uploader);
    }
    public record Submission(@NotNull @Valid TaskRequest followUp,@Valid TaskRequest nextTask,Long reminderTaskId,
            Long segmentId,Long analysisVersion,@Size(max=64) String template){}
    @Transactional public TaskResponse submit(Long callId,Submission request,Authentication auth){
        var call=calls.lockById(callId).orElseThrow(()->new NotFoundException("未找到通话"));access.requireAccess(call.getCustomerNo(),auth);
        var w=work.lockById(callId).orElseThrow(()->new NotFoundException("未找到待写跟进"));
        if(w.followUpId!=null)return tasks.find(w.followUpId,auth);
        var r=request.followUp();if(!w.customerNo.equals(r.customerId())||!Boolean.TRUE.equals(r.completed()))throw new ConflictException("请为本次通话提交已完成的跟进");
        TaskResponse saved;
        if(request.segmentId()!=null){
            var s=segments.findById(request.segmentId()).orElseThrow(()->new NotFoundException("未找到来源转写"));
            if(!callId.equals(s.callId))throw new ConflictException("转写来源与本次通话不一致");
            saved=analysis.confirm(s.id,request.analysisVersion(),request.template(),r.title(),auth).followUp();
            saved=tasks.update(saved.id(),r,auth);
        }else saved=tasks.create(r,auth);
        w.followUpId=saved.id();w.completedAt=now();w.confirmedContent=r.title().trim();w.confirmedBy=identities.currentUserId(auth);w.confirmedByName=identities.currentDisplayName(auth);
        w.sourceSegmentId=request.segmentId()!=null?request.segmentId():segments.findFirstByCallIdOrderByIdDesc(callId).map(s->s.id).orElse(null);
        w.analysisVersion=request.analysisVersion();w.template=request.template();work.saveAndFlush(w);
        if(request.segmentId()==null&&w.sourceSegmentId!=null){
            var s=segments.findById(w.sourceSegmentId).orElseThrow();
            if(s.sourceArticleId!=null)articles.lockById(s.sourceArticleId).filter(a->!a.companyCase&&w.customerNo.equals(a.customerNo)).ifPresent(a->{
                a.content+="\n\n销售已确认跟进（手动填写）\n通话编号："+callId+"\n"+w.confirmedContent;articles.save(a);
            });
        }
        if(request.nextTask()!=null){if(!w.customerNo.equals(request.nextTask().customerId())||Boolean.TRUE.equals(request.nextTask().completed()))throw new ConflictException("下次跟进客户或状态不正确");tasks.create(request.nextTask(),auth);}
        if(request.reminderTaskId()!=null&&!saved.id().equals(request.reminderTaskId())){
            var reminder=tasks.find(request.reminderTaskId(),auth);if(!w.customerNo.equals(reminder.customerId()))throw new ConflictException("关联提醒客户不一致");tasks.updateCompletion(request.reminderTaskId(),true,auth);
        }
        return saved;
    }
    public record Source(Long callId,Long segmentId,String recordingRevision,String title,String customerNo,String salesName,
            String uploadedByName,Long submittedById,String submittedByName,LocalDateTime sourceTime,String timeKind,Integer durationSeconds,
            String template,String generationMethod,LocalDateTime submittedAt,Long analysisVersion){}
    @Transactional(readOnly=true) public Source sourceForTask(Long taskId,Authentication auth){
        var w=work.findByFollowUpId(taskId).orElse(null);if(w==null)return null;access.requireAccess(w.customerNo,auth);
        var c=calls.findById(w.callId).orElseThrow();var s=w.sourceSegmentId==null?null:segments.findById(w.sourceSegmentId).orElse(null);
        var i=s==null?null:intelligence.findById(s.id).orElse(null);String title="";
        if(s!=null){var v=analysis.get(s.id,auth);if(v.analysis()!=null)title=v.analysis().path("title").asText("");}
        String submitted=w.confirmedByName!=null?w.confirmedByName:(i!=null&&i.confirmedByName!=null?i.confirmedByName:(w.confirmedBy==null?null:users.findById(w.confirmedBy).map(CrmUser::getDisplayName).orElse("历史操作人")));
        String uploader=c.getUploadedByUserId()==null?null:users.findById(c.getUploadedByUserId()).map(CrmUser::getDisplayName).orElse("历史上传人");
        return new Source(w.callId,s==null?null:s.id,s==null?null:s.recordingRevision,title,w.customerNo,s==null?c.getAgent():s.salesName,uploader,w.confirmedBy,submitted,
                s==null?("UPLOAD".equals(c.getRecordingSource())?c.getUploadedAt():c.getStartedAt()):s.sourceTime,
                s==null?("UPLOAD".equals(c.getRecordingSource())?"UPLOAD":"CALL"):(s.callStartedAt==null?"UPLOAD":"CALL"),
                s==null?c.getDurationSeconds():s.durationSeconds,w.template,w.analysisVersion==null?"MANUAL":"AI_CONFIRMED",w.completedAt,w.analysisVersion);
    }
    @Scheduled(fixedDelayString="${recording.followup.poll-ms:60000}") public void scan(){scanAt(now());}
    public void scanAt(LocalDateTime time){
        for(var candidate:work.findByFollowUpIdIsNullOrderByStartedAtDesc())tx.executeWithoutResult(status->{
            var w=work.lockById(candidate.callId).orElse(null);if(w==null||w.followUpId!=null)return;
            var customer=customers.findByCustomerNo(w.customerNo).orElse(null);if(customer==null)return;
            if(!time.isBefore(w.remindAt)&&customer.getOwnerId()!=null)users.findById(customer.getOwnerId()).filter(CrmUser::isEnabled).ifPresent(u->notify(u,w,"CALL_FOLLOWUP","本次通话尚未写跟进"));
            if(!time.isBefore(w.escalateAt))for(var u:users.findAllByEnabledTrueOrderByDisplayNameAsc())if(u.getRoles().stream().anyMatch(r->"ADMIN".equals(r.getCode())))notify(u,w,"CALL_OVERDUE","通话超过24小时未写跟进");
        });
    }
    private void notify(CrmUser user,CallFollowupWork w,String kind,String title){
        String key="call-work:"+w.callId+":"+kind+":"+user.getId();if(notifications.findByNotificationKey(key).isPresent())return;
        var n=new SystemNotification();n.setUsername(user.getUsername());n.setNotificationKey(key);n.setTitle(title);n.setContent("请核对并提交本次通话跟进；AI处理中或失败时也可手动填写。");n.setCustomerNo(w.customerNo);n.setCallId(w.callId);n.setNotificationKind(kind);notifications.saveAndFlush(n);
    }
    public record Summary(int pending,boolean created){}
    @Transactional public Summary dailySummary(Authentication auth){
        Long userId=identities.currentUserId(auth);em.find(CrmUser.class,userId,LockModeType.PESSIMISTIC_WRITE);
        var pending=list(null,auth).stream().filter(w->{
            boolean assigned=customers.findByCustomerNo(w.customerNo()).map(c->userId.equals(c.getOwnerId())).orElse(false);
            return !now().isBefore(assigned?w.remindAt():w.escalateAt());
        }).toList();
        String key="call-summary:"+userId+":"+now().toLocalDate();
        boolean created=!pending.isEmpty()&&notifications.findByNotificationKey(key).isEmpty();
        if(created){
            var n=new SystemNotification();n.setUsername(auth.getName());n.setNotificationKey(key);n.setTitle("待写通话跟进汇总");n.setContent("还有 "+pending.size()+" 通电话尚未提交跟进，点击查看待办。");n.setNotificationKind("CALL_SUMMARY");notifications.saveAndFlush(n);
        }return new Summary(pending.size(),created);
    }
    @Transactional(readOnly=true) public boolean completed(Long callId){return work.findById(callId).map(w->w.followUpId!=null).orElse(true);}
}
