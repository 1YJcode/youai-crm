package com.youai.crm.communication;

import com.fasterxml.jackson.databind.*;
import com.youai.crm.account.*;
import com.youai.crm.customer.CustomerAccessPolicy;
import com.youai.crm.knowledge.*;
import com.youai.crm.task.*;
import com.youai.crm.common.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;

@Service
public class CallIntelligenceService {
    private final CustomerTranscriptSegmentRepository segments;
    private final CallIntelligenceRepository intelligence;
    private final RecordingJobRepository jobs;
    private final DeepSeekAnalysisClient deepseek;
    private final FollowUpTemplates templates;
    private final CustomerAccessPolicy access;
    private final AccessPolicy users;
    private final UserDetailsService identities;
    private final FollowUpTaskRepository tasks;
    private final KnowledgeRepository articles;
    private final EntityManager entities;
    private final ObjectMapper json;
    private final TransactionTemplate tx;
    @org.springframework.beans.factory.annotation.Autowired private CrmUserRepository accounts;
    @org.springframework.beans.factory.annotation.Autowired private com.youai.crm.customer.CustomerRepository customers;
    @org.springframework.beans.factory.annotation.Value("${recording.deepseek.automatic-backfill:true}") private boolean automaticBackfill;
    public CallIntelligenceService(CustomerTranscriptSegmentRepository segments,CallIntelligenceRepository intelligence,RecordingJobRepository jobs,
            DeepSeekAnalysisClient deepseek,FollowUpTemplates templates,CustomerAccessPolicy access,AccessPolicy users,UserDetailsService identities,
            FollowUpTaskRepository tasks,KnowledgeRepository articles,EntityManager entities,ObjectMapper json,PlatformTransactionManager manager){
        this.segments=segments;this.intelligence=intelligence;this.jobs=jobs;this.deepseek=deepseek;this.templates=templates;this.access=access;this.users=users;
        this.identities=identities;this.tasks=tasks;this.articles=articles;this.entities=entities;this.json=json;tx=new TransactionTemplate(manager);
    }
    public record View(Long segmentId,String state,boolean configured,String model,JsonNode analysis,Long version,
            String error,String confirmedTemplate,String confirmedContent,String confirmedByName,LocalDateTime confirmedAt,TaskResponse followUp){}
    private CustomerTranscriptSegment authorize(Long id,Authentication auth){var s=segments.findById(id).orElseThrow(()->new NotFoundException("未找到来源转写"));access.requireAccess(s.customerNo,auth);return s;}
    public View get(Long id,Authentication auth){authorize(id,auth);return view(id,intelligence.findById(id).orElse(null));}
    public View latest(String customerNo,Authentication auth){access.requireAccess(customerNo,auth);var s=segments.findFirstByCustomerNoOrderBySourceTimeDescIdDesc(customerNo).orElseThrow(()->new NotFoundException("此客户尚无已完成的转写"));return get(s.id,auth);}
    private View view(Long id,CallIntelligence i){
        JsonNode analysis=null;try{if(i!=null&&i.analysisJson!=null)analysis=json.readTree(i.analysisJson);}catch(Exception e){throw new IllegalStateException("分析数据无法读取");}
        TaskResponse task=i==null||i.followUpId==null?null:tasks.findById(i.followUpId).map(TaskResponse::from).orElse(null);
        return new View(id,i==null?"NOT_REQUESTED":i.state,deepseek.configured(),i==null?deepseek.model():i.model,analysis,i==null?null:i.version,
                i==null?null:i.error,i==null?null:i.confirmedTemplate,i==null?null:i.confirmedContent,i==null?null:i.confirmedByName,i==null?null:i.confirmedAt,task);
    }
    public void register(Long callId,String revision,String actor,Long actorId,boolean consent){
        tx.executeWithoutResult(status->{
            var s=segments.findByCallIdAndRecordingRevision(callId,revision).orElse(null);if(s==null)return;
            entities.find(CustomerTranscriptSegment.class,s.id,LockModeType.PESSIMISTIC_WRITE);
            if(intelligence.existsById(s.id))return;
            var i=new CallIntelligence();i.segmentId=s.id;i.customerNo=s.customerNo;i.requestedBy=actor;i.requestedUserId=actorId;
            i.processingConsent=consent;i.requestedAt=LocalDateTime.now();i.model=deepseek.model();i.promptVersion=DeepSeekAnalysisClient.PROMPT_VERSION;
            i.state=consent?"WAITING_CONFIG":"WAITING_CONSENT";intelligence.saveAndFlush(i);
            if(consent&&deepseek.configured())enqueue(i,s);
        });
    }
    public View request(Long id,String preferredTemplate,Authentication auth){
        authorize(id,auth);
        if(preferredTemplate!=null&&!templates.contains(preferredTemplate))throw new IllegalArgumentException("请选择系统已有模板");
        tx.executeWithoutResult(status->{
            var s=entities.find(CustomerTranscriptSegment.class,id,LockModeType.PESSIMISTIC_WRITE);access.requireAccess(s.customerNo,auth);
            var i=intelligence.findById(id).orElseGet(CallIntelligence::new);
            if(i.confirmedAt!=null)throw new ConflictException("已确认的跟进不会被重新分析覆盖");
            if("QUEUED".equals(i.state)||"PROCESSING".equals(i.state))throw new ConflictException("当前分析已在队列中");
            i.segmentId=id;i.customerNo=s.customerNo;i.requestedBy=auth.getName();i.requestedUserId=users.currentUserId(auth);i.processingConsent=true;
            i.preferredTemplate=preferredTemplate;i.requestedAt=LocalDateTime.now();i.model=deepseek.model();i.promptVersion=DeepSeekAnalysisClient.PROMPT_VERSION;i.error=null;i.state="WAITING_CONFIG";
            intelligence.saveAndFlush(i);if(deepseek.configured())enqueue(i,s);
        });return get(id,auth);
    }
    private void enqueue(CallIntelligence i,CustomerTranscriptSegment s){
        var job=new RecordingJob(s.callId,s.recordingRevision,"deepseek",i.requestedBy,i.requestedUserId);job.analysisConsent=true;
        jobs.saveAndFlush(job);i.activeJobId=job.id;i.state="QUEUED";i.error=null;intelligence.saveAndFlush(i);
    }
    @Scheduled(fixedDelayString="${recording.deepseek.resume-ms:30000}")
    public void resumeConfigured(){
        if(!deepseek.configured())return;
        for(Long id:intelligence.waiting(PageRequest.of(0,20)))tx.executeWithoutResult(status->{
            var i=intelligence.lockById(id).orElse(null);if(i!=null&&"WAITING_CONFIG".equals(i.state)&&i.processingConsent){var s=segments.findById(id).orElse(null);if(s!=null)enqueue(i,s);}
        });
    }
    /** Upgrade only unconfirmed latest sources; never creates historical reminder work. */
    @Scheduled(initialDelayString="${recording.deepseek.backfill-initial-ms:15000}",fixedDelayString="${recording.deepseek.backfill-ms:30000}")
    public void fillAutomaticDrafts(){
        if(!automaticBackfill||!deepseek.configured())return;
        for(Long id:segments.needingAutomaticDrafts(DeepSeekAnalysisClient.PROMPT_VERSION,PageRequest.of(0,20))){
            var source=segments.findById(id).orElse(null);if(source==null)continue;
            var owner=customers.findByCustomerNo(source.customerNo).map(c->c.getOwnerId()).flatMap(accounts::findById).filter(CrmUser::isEnabled).orElse(null);
            if(owner==null)owner=accounts.findAllByEnabledTrueOrderByDisplayNameAsc().stream().filter(u->u.getRoles().stream().anyMatch(r->"ADMIN".equals(r.getCode()))).min(java.util.Comparator.comparing(CrmUser::getId)).orElse(null);
            if(owner==null)continue;
            var principal=identities.loadUserByUsername(owner.getUsername());
            var auth=UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());
            try{request(id,null,auth);}catch(ConflictException|org.springframework.security.access.AccessDeniedException ignored){}
        }
    }
    private Authentication actor(RecordingJob job){
        var principal=identities.loadUserByUsername(job.actor);
        if(!principal.isEnabled()||!principal.isAccountNonLocked())throw new org.springframework.security.access.AccessDeniedException("账号不可用");
        var auth=UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities());
        if(!job.actorUserId.equals(users.currentUserId(auth)))throw new org.springframework.security.access.AccessDeniedException("任务账号已变化");return auth;
    }
    public void process(RecordingJob job,String token){
        try{
            var snapshot=intelligence.findByActiveJobId(job.id).orElse(null);
            if(snapshot==null){jobs.finish(job.id,token,"CANCELLED",LocalDateTime.now());return;}
            var s=authorize(snapshot.segmentId,actor(job));
            boolean claimed=Boolean.TRUE.equals(tx.execute(status->{
                var j=entities.find(RecordingJob.class,job.id,LockModeType.PESSIMISTIC_WRITE);var i=intelligence.lockById(s.id).orElseThrow();
                if(!token.equals(j.token)||!"RUNNING".equals(j.state)||!job.id.equals(i.activeJobId)||i.confirmedAt!=null)return false;
                if(!deepseek.configured()){i.state="WAITING_CONFIG";i.activeJobId=null;jobs.finish(job.id,token,"CANCELLED",LocalDateTime.now());return false;}
                i.state="PROCESSING";return true;
            }));
            if(!claimed)return;if(job.attempts>3)throw new IllegalStateException("分析重试次数已用完");
            var result=deepseek.analyze(s.transcript,snapshot.preferredTemplate);access.requireAccess(s.customerNo,actor(job));
            String serialized=json.writeValueAsString(result);
            tx.executeWithoutResult(status->{
                var j=entities.find(RecordingJob.class,job.id,LockModeType.PESSIMISTIC_WRITE);var i=intelligence.lockById(s.id).orElseThrow();
                if(!token.equals(j.token)||!"RUNNING".equals(j.state)||!job.id.equals(i.activeJobId))return;
                i.analysisJson=serialized;i.state="COMPLETED";i.error=null;i.completedAt=LocalDateTime.now();i.model=deepseek.model();
                appendKnowledge(s,"DeepSeek 通话分析（AI草稿，待销售核对）\n"+render(result));
                jobs.finish(job.id,token,"COMPLETED",LocalDateTime.now());
            });
        }catch(Exception e){
            if(e instanceof InterruptedException)Thread.currentThread().interrupt();
            boolean retry=job.attempts<3&&!(e instanceof org.springframework.security.access.AccessDeniedException)&&!(e instanceof org.springframework.security.core.userdetails.UsernameNotFoundException);
            tx.executeWithoutResult(status->{
                var j=entities.find(RecordingJob.class,job.id,LockModeType.PESSIMISTIC_WRITE);var i=intelligence.findByActiveJobId(job.id).orElse(null);
                if(!token.equals(j.token)||!"RUNNING".equals(j.state)||i==null)return;
                i.state=retry?"QUEUED":"FAILED";i.error=retry?"分析暂时失败，等待自动重试":"分析失败，请检查DeepSeek配置或客户权限后重试";intelligence.save(i);
                jobs.finish(job.id,token,retry?"QUEUED":"FAILED",LocalDateTime.now().plusSeconds(30L*Math.min(job.attempts,3)));
            });
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Call analysis failed ({})",e.getClass().getSimpleName());
        }
    }
    public View confirm(Long id,Long version,String template,String content,Authentication auth){
        authorize(id,auth);if(!templates.contains(template))throw new IllegalArgumentException("请选择系统已有模板");
        tx.executeWithoutResult(status->{
            var source=segments.findById(id).orElseThrow();
            entities.find(CallRecord.class,source.callId,LockModeType.PESSIMISTIC_WRITE);
            var work=entities.find(CallFollowupWork.class,source.callId,LockModeType.PESSIMISTIC_WRITE);
            var s=entities.find(CustomerTranscriptSegment.class,id,LockModeType.PESSIMISTIC_WRITE);access.requireAccess(s.customerNo,auth);
            var i=intelligence.lockById(id).orElseThrow(()->new ConflictException("请先完成AI分析"));
            if(i.followUpId!=null){if(template.equals(i.confirmedTemplate)&&content.trim().equals(i.confirmedContent))return;throw new ConflictException("此来源已保存跟进，请在跟进记录中查看");}
            if(work!=null&&work.followUpId!=null)throw new ConflictException("本次通话已写跟进，请在跟进记录中查看");
            if(!Objects.equals(version,i.version)||!"COMPLETED".equals(i.state)||i.analysisJson==null)throw new ConflictException("分析草稿已变化，请重新打开核对");
            LocalDateTime now=LocalDateTime.now();var task=new FollowUpTask();task.setTitle(content.trim());task.setCustomerId(s.customerNo);task.setCustomerName(s.customerName);
            task.setOwner(users.currentDisplayName(auth));task.setTaskType("电话跟进");task.setFollowedAt(now);task.setDueAt(now);task.setCompleted(true);task.setStatus("done");task.setPriority("普通");tasks.saveAndFlush(task);
            i.confirmedTemplate=template;i.confirmedContent=content.trim();i.confirmedBy=users.currentUserId(auth);i.confirmedByName=users.currentDisplayName(auth);i.confirmedAt=now;i.followUpId=task.getId();
            if(work!=null){work.followUpId=task.getId();work.completedAt=now;work.sourceSegmentId=s.id;work.analysisVersion=version;work.template=template;work.confirmedContent=content.trim();work.confirmedBy=i.confirmedBy;work.confirmedByName=i.confirmedByName;}
            appendKnowledge(s,"销售已确认跟进\n模板："+template+"\n确认人："+i.confirmedByName+"\n确认时间："+now+"\n"+content.trim());
        });return get(id,auth);
    }
    private void appendKnowledge(CustomerTranscriptSegment s,String text){
        if(s.sourceArticleId==null)return;
        var a=articles.lockById(s.sourceArticleId).orElse(null);
        if(a!=null&&!a.companyCase&&s.customerNo.equals(a.customerNo)){a.content=a.content+"\n\n"+text;a.aiStatus="COMPLETED";a.analyzedAt=LocalDateTime.now();articles.save(a);}
    }
    private String render(JsonNode result){
        StringBuilder out=new StringBuilder(result.path("summary").asText());
        for(String key:List.of("customerAnalysis","salesCoaching","reminders")){
            out.append("\n\n").append(switch(key){case "customerAnalysis"->"客户分析";case "salesCoaching"->"销售改进建议";default->"提醒";});
            for(var item:result.path(key))out.append("\n[").append(item.path("kind").asText()).append("] ").append(item.path("text").asText()).append(item.path("evidence").asText().isBlank()?"":"\n依据："+item.path("evidence").asText());
        }
        return out.toString();
    }
}
