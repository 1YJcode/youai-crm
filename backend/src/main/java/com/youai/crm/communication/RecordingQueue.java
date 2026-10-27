package com.youai.crm.communication;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import jakarta.persistence.*;
import com.youai.crm.customer.CustomerAccessPolicy;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** SQL-backed queue, conditional claims and renewable leases allow restart recovery. */
@Service
public class RecordingQueue {
    private final RecordingJobRepository jobs;
    private final CallRecordingRepository recordings;
    private final RecordingTranscriptionRepository results;
    private final CallRecordRepository calls;
    private final CustomerAccessPolicy access;
    private final UserDetailsService users;
    private final com.youai.crm.account.AccessPolicy userAccess;
    private final RecordingStorage storage;
    private final LocalTranscriptionClient local;
    private final QwenTranscriptionClient cloud;
    private final RecordingAiClient ai;
    private final RecordingKnowledgeService knowledge;
    private final CallIntelligenceService intelligence;
    private final TransactionTemplate transactions;
    private final EntityManager entities;
    private final ExecutorService workers;
    private final Semaphore slots;
    private final Map<String,String> running=new ConcurrentHashMap<>();
    private final boolean enabled;
    private volatile boolean closing;

    public RecordingQueue(RecordingJobRepository jobs,CallRecordingRepository recordings,
            RecordingTranscriptionRepository results,CallRecordRepository calls,CustomerAccessPolicy access,
            UserDetailsService users,com.youai.crm.account.AccessPolicy userAccess,RecordingStorage storage,LocalTranscriptionClient local,QwenTranscriptionClient cloud,
            RecordingAiClient ai,RecordingKnowledgeService knowledge,CallIntelligenceService intelligence,PlatformTransactionManager manager,EntityManager entities,
            @Value("${recording.queue.concurrency:1}") int concurrency,@Value("${recording.queue.enabled:true}") boolean enabled) {
        this.jobs=jobs; this.recordings=recordings; this.results=results; this.calls=calls; this.access=access;
        this.users=users; this.storage=storage; this.local=local; this.cloud=cloud; this.ai=ai; this.knowledge=knowledge;
        this.intelligence=intelligence;
        this.userAccess=userAccess;
        transactions=new TransactionTemplate(manager); this.entities=entities; this.enabled=enabled;
        int count=Math.max(1,Math.min(4,concurrency)); slots=new Semaphore(count);
        workers=Executors.newFixedThreadPool(count,r -> { var t=new Thread(r,"recording-queue"); t.setDaemon(true); return t; });
    }
    public void enqueue(Long callId,String revision,String provider,Authentication auth) {
        enqueue(callId,revision,provider,auth,false);
    }
    public void enqueue(Long callId,String revision,String provider,Authentication auth,boolean analysisConsent) {
        transactions.executeWithoutResult(status -> {
            // Serialize replacement and enqueue on the same call, even across server instances.
            var call=entities.find(CallRecord.class,callId,LockModeType.PESSIMISTIC_WRITE);
            if(call==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"未找到通话记录");
            access.requireAccess(call.getCustomerNo(),auth);
            var audio=recordings.findById(callId).orElseThrow();
            if(!revision.equals(audio.getRevision())) throw new ResponseStatusException(HttpStatus.CONFLICT,"录音已更新，请刷新");
            String id=callId+":"+provider;
            var row=results.findById(id).orElseGet(() -> new RecordingTranscription(id,revision));
            boolean legacyExpired=row.activeJobId==null && row.started!=null && row.started.isBefore(LocalDateTime.now().minusMinutes(120));
            if(revision.equals(row.revision) && !legacyExpired && ("QUEUED".equals(row.status) || "PROCESSING".equals(row.status)))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"当前方式已在队列中");
            row.revision=revision; row.status="QUEUED"; row.started=null; row.error=null; row.transcript=null; row.requestedBy=auth.getName();
            var job=new RecordingJob(callId,revision,provider,auth.getName(),userAccess.currentUserId(auth));
            job.analysisConsent=analysisConsent;
            row.activeJobId=job.id;
            results.saveAndFlush(row);
            jobs.saveAndFlush(job);
        });
    }
    @Scheduled(fixedDelayString="${recording.queue.poll-ms:2000}")
    public void dispatch() {
        if(!enabled || closing) return;
        for(String id:jobs.available(LocalDateTime.now(),PageRequest.of(0,Math.max(1,slots.availablePermits())))) {
            if(!slots.tryAcquire()) break;
            String token=UUID.randomUUID().toString();
            try {
                if(jobs.claim(id,token,LocalDateTime.now(),LocalDateTime.now().plusMinutes(2))==0) { slots.release(); continue; }
                running.put(id,token);
                workers.execute(() -> { try { process(id,token); } finally { running.remove(id); slots.release(); } });
            } catch(RuntimeException e) { running.remove(id); slots.release(); throw e; }
        }
    }
    @Scheduled(fixedDelay=20000)
    public void heartbeat() {
        running.forEach((id,token) -> jobs.heartbeat(id,token,LocalDateTime.now().plusMinutes(2)));
    }
    private Authentication authentication(RecordingJob job) {
        var user=users.loadUserByUsername(job.actor);
        if(!user.isEnabled() || !user.isAccountNonLocked() || !user.isAccountNonExpired() || !user.isCredentialsNonExpired())
            throw new org.springframework.security.access.AccessDeniedException("账号已停用");
        var authentication=UsernamePasswordAuthenticationToken.authenticated(user,null,user.getAuthorities());
        if(!job.actorUserId.equals(userAccess.currentUserId(authentication)))
            throw new org.springframework.security.access.AccessDeniedException("任务账号已变更");
        return authentication;
    }
    private boolean matches(RecordingJob job,RecordingTranscription row) {
        return row!=null && job.revision.equals(row.revision) && job.id.equals(row.activeJobId)
                && ("QUEUED".equals(row.status) || "PROCESSING".equals(row.status));
    }
    private void process(String id,String token) {
        var job=jobs.findById(id).orElseThrow();
        if("deepseek".equals(job.provider)){intelligence.process(job,token);return;}
        LocalDateTime started=LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        try {
            if(job.attempts>3) throw new IllegalStateException("任务重试次数已用完");
            var auth=authentication(job);
            var call=calls.findById(job.callId).orElseThrow(); access.requireAccess(call.getCustomerNo(),auth);
            boolean valid=Boolean.TRUE.equals(transactions.execute(status -> {
                var locked=entities.find(RecordingJob.class,id,LockModeType.PESSIMISTIC_WRITE);
                if(!token.equals(locked.token) || !"RUNNING".equals(locked.state)) return false;
                var audio=recordings.findById(job.callId).orElse(null);
                var row=entities.find(RecordingTranscription.class,job.callId+":"+job.provider,LockModeType.PESSIMISTIC_WRITE);
                if(audio==null || !job.revision.equals(audio.getRevision()) || !matches(job,row)) return false;
                row.status="PROCESSING"; row.started=started; return true;
            }));
            if(!valid) { jobs.finish(id,token,"CANCELLED",LocalDateTime.now()); return; }
            var audio=recordings.findById(job.callId).orElseThrow();
            if(!job.revision.equals(audio.getRevision())) { jobs.finish(id,token,"CANCELLED",LocalDateTime.now()); return; }
            byte[] bytes=storage.read(audio);
            access.requireAccess(call.getCustomerNo(),authentication(job));
            String text="local".equals(job.provider)?local.transcribe(bytes,audio.getMediaType()):cloud.transcribe(bytes,audio.getMediaType());
            access.requireAccess(call.getCustomerNo(),authentication(job));
            Long article=transactions.execute(status -> {
                var locked=entities.find(RecordingJob.class,id,LockModeType.PESSIMISTIC_WRITE);
                if(!token.equals(locked.token) || !"RUNNING".equals(locked.state)) return null;
                calls.lockById(job.callId).orElseThrow();
                if(!job.revision.equals(recordings.findRevision(job.callId).orElse(null))) {
                    jobs.finish(id,token,"CANCELLED",LocalDateTime.now()); return null;
                }
                if(results.finish(job.callId+":"+job.provider,job.revision,started,"COMPLETED",text,null)==0) {
                    jobs.finish(id,token,"CANCELLED",LocalDateTime.now()); return null;
                }
                var captured=knowledge.capture(job.callId,job.provider,job.revision,started,text,LocalDateTime.now(),job.actor,!job.analysisConsent&&ai.summaryConfigured());
                intelligence.register(job.callId,job.revision,job.actor,job.actorUserId,job.analysisConsent);
                jobs.finish(id,token,"COMPLETED",LocalDateTime.now()); return captured;
            });
            if(article!=null && !job.analysisConsent && ai.summaryConfigured()) {
                try {
                    access.requireAccess(call.getCustomerNo(),authentication(job));
                    knowledge.finish(article,ai.summarize(text));
                }
                catch(Exception ignored) { knowledge.finish(article,null); }
            }
        } catch(Exception e) {
            org.slf4j.LoggerFactory.getLogger(RecordingQueue.class).warn("Recording job {} attempt {} failed ({})",id,job.attempts,e.getClass().getSimpleName());
            if(e instanceof InterruptedException) Thread.currentThread().interrupt();
            boolean retry=job.attempts<3 && !(e instanceof org.springframework.security.access.AccessDeniedException)
                    && !(e instanceof org.springframework.security.core.userdetails.UsernameNotFoundException);
            transactions.executeWithoutResult(status -> {
                var locked=entities.find(RecordingJob.class,id,LockModeType.PESSIMISTIC_WRITE);
                if(!token.equals(locked.token) || !"RUNNING".equals(locked.state)) return;
                var row=entities.find(RecordingTranscription.class,job.callId+":"+job.provider,LockModeType.PESSIMISTIC_WRITE);
                if(matches(job,row)) {
                    row.status=retry?"QUEUED":"FAILED"; row.error=retry?"转写暂时失败，等待自动重试":"转写失败，请检查服务或权限后重试";
                }
                jobs.finish(id,token,retry?"QUEUED":"FAILED",LocalDateTime.now().plusSeconds(30L*Math.min(job.attempts,3)));
            });
        }
    }
    @jakarta.annotation.PreDestroy void shutdown() {
        closing=true; workers.shutdownNow();
        // A hard shutdown leaves leases behind; another process reclaims them after two minutes.
    }
}
