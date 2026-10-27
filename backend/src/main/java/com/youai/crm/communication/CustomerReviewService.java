package com.youai.crm.communication;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.youai.crm.customer.*;
import com.youai.crm.knowledge.*;
import com.youai.crm.task.*;
import com.youai.crm.common.*;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class CustomerReviewService {
    private final CustomerRepository customers;private final CustomerAccessPolicy access;
    private final CustomerTranscriptSegmentRepository segments;private final CallIntelligenceRepository analyses;
    private final CustomerReviewRepository reviews;private final FollowUpTaskRepository tasks;
    private final KnowledgeRepository articles;private final KnowledgeSegmentSourceRepository links;
    private final CustomerReviewClient model;private final ObjectMapper json;private final TransactionTemplate tx;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"customer-review");t.setDaemon(true);return t;});
    private final AtomicBoolean running=new AtomicBoolean();
    @Value("${recording.review.enabled:${recording.queue.enabled:true}}") private boolean enabled;
    public CustomerReviewService(CustomerRepository customers,CustomerAccessPolicy access,CustomerTranscriptSegmentRepository segments,
            CallIntelligenceRepository analyses,CustomerReviewRepository reviews,FollowUpTaskRepository tasks,
            KnowledgeRepository articles,KnowledgeSegmentSourceRepository links,CustomerReviewClient model,ObjectMapper json,PlatformTransactionManager manager){
        this.customers=customers;this.access=access;this.segments=segments;this.analyses=analyses;this.reviews=reviews;this.tasks=tasks;
        this.articles=articles;this.links=links;this.model=model;this.json=json;tx=new TransactionTemplate(manager);
    }
    record Snapshot(ObjectNode input,String fingerprint,ArrayNode sources){}
    private Snapshot snapshot(String no){
        var customer=customers.findByCustomerNo(no).orElseThrow(()->new NotFoundException("未找到客户"));
        var input=json.createObjectNode().put("promptVersion",CustomerReviewClient.VERSION).put("customerStage",customer.getStage());
        var sourceList=input.putArray("sources");var metadata=json.createArrayNode();var ai=input.putArray("priorCallAdvice");
        var selected=segments.reviewSources(no,PageRequest.of(0,101));
        if(selected.size()>100)throw new IllegalArgumentException("通话超过100次，请先分阶段整理复盘，本次未忽略任何通话");
        for(var s:selected){
            var a=analyses.findById(s.id).orElse(null);
            if(a==null||!a.processingConsent)throw new IllegalArgumentException("部分录音尚未授权AI处理，请先完成授权");
            String sourceId="call:"+s.id;
            sourceList.addObject().put("id",sourceId).put("type","CALL").put("callId",s.callId).put("segmentId",s.id)
                .put("sourceTime",String.valueOf(s.sourceTime)).put("timeKind",s.callStartedAt==null?"UPLOAD":"CALL").put("salesName",s.salesName).put("durationSeconds",s.durationSeconds).put("content",s.transcript);
            metadata.addObject().put("id",sourceId).put("type","CALL").put("callId",s.callId).put("segmentId",s.id)
                .put("sourceTime",String.valueOf(s.sourceTime)).put("timeKind",s.callStartedAt==null?"UPLOAD":"CALL").put("salesName",s.salesName).put("durationSeconds",s.durationSeconds).put("recordingRevision",s.recordingRevision);
            if(a.analysisJson!=null)try{var detail=json.readTree(a.analysisJson);var advice=ai.addObject().put("sourceId",sourceId).put("completedAt",String.valueOf(a.completedAt));
                advice.set("salesCoaching",detail.path("salesCoaching"));advice.set("reminders",detail.path("reminders"));}catch(Exception e){throw new IllegalStateException("分析数据无法读取");}
        }
        for(var t:tasks.findByCustomerIdAndFollowedAtIsNotNullOrderByFollowedAtAscIdAsc(no)){
            String id="followup:"+t.getId();sourceList.addObject().put("id",id).put("type","FOLLOWUP").put("sourceTime",String.valueOf(t.getFollowedAt())).put("salesName",t.getOwner()).put("content",t.getTitle());
            metadata.addObject().put("id",id).put("type","FOLLOWUP").put("sourceTime",String.valueOf(t.getFollowedAt())).put("salesName",t.getOwner());
        }
        if(selected.isEmpty())throw new IllegalArgumentException("请先上传录音并完成转写");
        String serialized=input.toString();if(serialized.length()>120000)throw new IllegalArgumentException("累计资料超过本次复盘容量，请先分阶段整理，本次未截断资料");
        try{return new Snapshot(input,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(serialized.getBytes(StandardCharsets.UTF_8))),metadata);}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public Map<String,Object> get(String no,Authentication auth){
        access.requireAccess(no,auth);var customer=customers.findByCustomerNo(no).orElseThrow(()->new NotFoundException("未找到客户"));
        var result=new LinkedHashMap<String,Object>();result.put("customerNo",no);result.put("customerStage",customer.getStage());result.put("configured",model.configured());
        var history=reviews.findByCustomerNoOrderByIdDesc(no,PageRequest.of(0,20));result.put("history",history.stream().map(r->view(r,false)).toList());
        var latest=history.isEmpty()?null:history.getFirst();
        try{var fingerprint=snapshot(no).fingerprint;var matching=reviews.findByCustomerNoAndFingerprint(no,fingerprint).orElse(null);result.put("outdated",matching==null||"SUPERSEDED".equals(matching.state));if(matching!=null)latest=matching;}
        catch(IllegalArgumentException e){result.put("blockedReason",e.getMessage());result.put("outdated",true);}
        result.put("latest",latest==null?null:view(latest,true));
        var completed=latest!=null&&"COMPLETED".equals(latest.state)?latest:reviews.findFirstByCustomerNoAndStateOrderByIdDesc(no,"COMPLETED").orElse(null);result.put("completed",completed==null?null:view(completed,true));
        access.requireAccess(no,auth);return result;
    }
    public Map<String,Object> revision(String no,Long id,Authentication auth){access.requireAccess(no,auth);var r=reviews.findById(id).filter(v->no.equals(v.customerNo)).orElseThrow(()->new NotFoundException("未找到复盘版本"));return view(r,true);}
    private Map<String,Object> view(CustomerReview r,boolean details){
        var out=new LinkedHashMap<String,Object>();out.put("id",r.id);out.put("state",r.state);out.put("version",r.version);out.put("createdAt",r.createdAt);out.put("completedAt",r.completedAt);
        out.put("customerStage",r.customerStage);out.put("error",r.error);out.put("publishedArticleId",r.publishedArticleId);
        if(details)try{out.put("sources",json.readTree(r.sourcesJson));out.put("analysis",r.resultJson==null?null:json.readTree(r.resultJson));}catch(Exception e){throw new IllegalStateException("复盘资料无法读取");}return out;
    }
    public void request(String no,Authentication auth){access.requireAccess(no,auth);enqueue(no,true);}
    void enqueue(String no,boolean retry){
        tx.executeWithoutResult(status->{
            customers.lockByCustomerNo(no).orElseThrow(()->new NotFoundException("未找到客户"));var s=snapshot(no);
            var old=reviews.findByCustomerNoAndFingerprint(no,s.fingerprint).orElse(null);
            if(old!=null){if("SUPERSEDED".equals(old.state)||(retry&&"FAILED".equals(old.state))){old.state="QUEUED";old.attempts=0;old.error=null;}return;}
            var r=new CustomerReview();r.customerNo=no;r.fingerprint=s.fingerprint;r.customerStage=s.input.path("customerStage").asText();r.sourcesJson=s.sources.toString();r.state="QUEUED";r.createdAt=LocalDateTime.now();reviews.saveAndFlush(r);
        });
    }
    @Scheduled(initialDelayString="${recording.review.initial-ms:45000}",fixedDelayString="${recording.review.scan-ms:30000}")
    public void scheduled(){
        if(!enabled||!model.configured()||!running.compareAndSet(false,true))return;
        worker.submit(()->{try{
            for(String no:segments.reviewCustomers())try{enqueue(no,false);}catch(IllegalArgumentException ignored){}
            for(Long id:reviews.pending(PageRequest.of(0,5)))process(id);
        }catch(Exception e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Customer review scan failed ({})",e.getClass().getSimpleName());}finally{running.set(false);}});
    }
    @PreDestroy public void stop(){worker.shutdownNow();}
    public void process(Long id){
        String token=UUID.randomUUID().toString();
        var claimed=tx.execute(status->{var r=reviews.lockById(id).orElse(null);
            if(r==null||!("QUEUED".equals(r.state)||("PROCESSING".equals(r.state)&&r.leaseUntil.isBefore(LocalDateTime.now()))))return null;
            if(r.attempts>=3){r.state="FAILED";r.error="复盘重试已用完，可手动重试";return null;}
            r.state="PROCESSING";r.token=token;r.attempts++;r.leaseUntil=LocalDateTime.now().plusMinutes(10);return r;
        });if(claimed==null)return;
        try{
            var s=snapshot(claimed.customerNo);if(!s.fingerprint.equals(claimed.fingerprint)){finishStale(id,token);return;}
            var previous=reviews.findFirstByCustomerNoAndStateOrderByIdDesc(claimed.customerNo,"COMPLETED").orElse(null);
            var prior=previous==null?null:json.readTree(previous.resultJson);var raw=model.generate(s.input,prior);var result=model.normalize(raw,s.input);
            tx.executeWithoutResult(status->{
                customers.lockByCustomerNo(claimed.customerNo).orElseThrow();var r=reviews.lockById(id).orElseThrow();if(!token.equals(r.token))return;
                if(!snapshot(r.customerNo).fingerprint.equals(r.fingerprint)){r.state="SUPERSEDED";return;}
                r.resultJson=result.toString();r.state="COMPLETED";r.error=null;r.completedAt=LocalDateTime.now();r.token=null;
            });
        }catch(Exception e){
            if(e instanceof InterruptedException)Thread.currentThread().interrupt();
            tx.executeWithoutResult(status->{var r=reviews.lockById(id).orElseThrow();if(!token.equals(r.token))return;
                r.state=r.attempts<3?"QUEUED":"FAILED";r.error="复盘暂时失败，请检查模型配置或稍后重试";r.token=null;});
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Customer review failed ({})",e.getClass().getSimpleName());
        }
    }
    private void finishStale(Long id,String token){tx.executeWithoutResult(status->{var r=reviews.lockById(id).orElseThrow();if(token.equals(r.token)){r.state="SUPERSEDED";r.token=null;}});}
    public Long publish(String no,Long id,Long version,String title,String content,Authentication auth){
        access.requireAccess(no,auth);
        return tx.execute(status->{
            var customer=customers.lockByCustomerNo(no).orElseThrow(()->new NotFoundException("未找到客户"));access.requireAccess(no,auth);
            if(!"已成交".equals(customer.getStage()))throw new ConflictException("客户确认已成交后才能发布成功案例");
            var r=reviews.lockById(id).filter(v->no.equals(v.customerNo)).orElseThrow(()->new NotFoundException("未找到复盘版本"));
            if(r.publishedArticleId!=null)return r.publishedArticleId;
            var existing=articles.findBySourceKey("customer-success:"+no).orElse(null);if(existing!=null)return existing.id;
            if(!Objects.equals(version,r.version)||!"COMPLETED".equals(r.state)||!"已成交".equals(r.customerStage)||!snapshot(no).fingerprint.equals(r.fingerprint))throw new ConflictException("复盘已更新，请核对最新成交复盘后发布");
            // Require explicit manual review and block obvious direct identifiers even in edited drafts.
            String body=title+"\n"+content;for(String value:Arrays.asList(customer.getName(),customer.getNickname(),customer.getPhone(),customer.getWechat(),customer.getIdCard(),no))if(value!=null&&value.length()>=2&&body.contains(value))throw new IllegalArgumentException("案例仍含客户姓名、联系方式或编号，请先脱敏");
            if(body.matches("(?s).*1[3-9][0-9]{9}.*")||body.matches("(?s).*[0-9]{17}[0-9Xx].*"))throw new IllegalArgumentException("案例仍含疑似电话或证件号码，请先脱敏");
            var sourceIds=new ArrayList<Long>();try{for(var s:json.readTree(r.sourcesJson))if("CALL".equals(s.path("type").asText()))sourceIds.add(s.path("segmentId").asLong());}catch(Exception e){throw new IllegalStateException(e);}
            var first=segments.findById(sourceIds.getFirst()).orElseThrow();var a=new KnowledgeArticle();a.companyCase=true;a.sourceKey="customer-success:"+no;
            a.sourceCallId=first.callId;a.customerNo=no;a.title=title.trim();a.category="婚恋成交复盘";a.content=content.trim();a.createdBy=auth.getName();a.updatedBy=auth.getName();articles.saveAndFlush(a);
            sourceIds.forEach(s->links.save(new KnowledgeSegmentSource(a.id,s)));links.flush();r.publishedArticleId=a.id;return a.id;
        });
    }
}
