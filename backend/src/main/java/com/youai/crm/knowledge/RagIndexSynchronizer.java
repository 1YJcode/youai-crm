package com.youai.crm.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.youai.crm.communication.*;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Reconciliation is the durable queue: authoritative rows + persisted remote catalog.
 * Retries after failures/restarts require no user query and cannot lose an update event.
 */
@Service
public class RagIndexSynchronizer {
    private final KnowledgeRepository articles;
    private final CustomerTranscriptSegmentRepository segments;
    private final RagSourceReader reader;
    private final RagNotebookClient client;
    private final AtomicBoolean running=new AtomicBoolean();
    private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"rag-index-sync");t.setDaemon(true);return t;});
    @Value("${rag.notebook.sync.enabled:true}") private boolean enabled;
    private volatile Status status=new Status("WAITING",null,0,0,0);
    public record Status(String state,Instant completedAt,int updated,int deleted,int failed){}
    public RagIndexSynchronizer(KnowledgeRepository articles,CustomerTranscriptSegmentRepository segments,RagSourceReader reader,RagNotebookClient client){
        this.articles=articles;this.segments=segments;this.reader=reader;this.client=client;
    }
    public Status status(){return status;}
    @Scheduled(initialDelayString="${rag.notebook.sync.initial-ms:5000}",fixedDelayString="${rag.notebook.sync.poll-ms:15000}")
    public void schedule(){
        if(enabled&&client.configured()&&running.compareAndSet(false,true))executor.execute(()->{
            try{reconcile();}finally{running.set(false);}
        });
    }
    private String key(String scope,String id){return scope+"\n"+id;}
    private boolean shared(KnowledgeArticle a){return a.isPublished()&&(a.companyCase||(a.sourceCallId==null&&a.customerNo==null));}
    private boolean exists(JsonNode row){
        String id=row.path("id").asText(),scope=row.path("scope").asText();
        try{
            if(id.startsWith("article:"))return "company".equals(scope)&&articles.findById(Long.parseLong(id.substring(8))).filter(this::shared).isPresent();
            if(id.startsWith("segment:"))return segments.findById(Long.parseLong(id.substring(8)))
                .map(s->("customer:"+CustomerTranscriptService.view(s,false).customerNo()).equals(scope)).orElse(false);
            return false;
        }catch(NumberFormatException e){return false;}
    }
    private boolean publish(RagKnowledgeService.Material m,Map<String,JsonNode> remote)throws Exception{
        String scope=m.customerNo()==null?"company":"customer:"+m.customerNo();
        var old=remote.get(key(scope,m.id()));
        if(old!=null&&m.version().equals(old.path("version").asText()))return false;
        client.sync(scope,m.id(),old==null?"":old.path("token").asText(),
            Map.of("id",m.id(),"version",m.version(),"title",m.title(),"content",m.content()));
        return true;
    }
    public synchronized void reconcile(){
        int updated=0,deleted=0,failed=0;
        status=new Status("RUNNING",status.completedAt(),0,0,0);
        try{
            var remote=new LinkedHashMap<String,JsonNode>();
            for(var row:client.catalog())remote.put(key(row.path("scope").asText(),row.path("id").asText()),row);
            // Deletions run first, even if an unrelated source fails to render or embed.
            for(var row:remote.values())try{
                if(!exists(row)){client.sync(row.path("scope").asText(),row.path("id").asText(),row.path("token").asText(),null);deleted++;}
            }catch(Exception e){failed++;}
            long cursor=0;
            while(true){
                var page=articles.findByIdGreaterThanOrderByIdAsc(cursor,PageRequest.of(0,100));if(page.isEmpty())break;
                for(var a:page){cursor=a.id;if(shared(a))try{
                    if(publish(new RagKnowledgeService.Material("article:"+a.id,String.valueOf(a.version),a.title,a.content,a.id,null,null),remote))updated++;
                }catch(Exception e){failed++;}}
                if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            }
            cursor=0;
            while(true){
                var page=segments.findByIdGreaterThanOrderByIdAsc(cursor,PageRequest.of(0,100));if(page.isEmpty())break;
                for(var s:page){cursor=CustomerTranscriptService.view(s,false).id();try{if(publish(reader.render(s),remote))updated++;}catch(Exception e){failed++;}}
                if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            }
            client.cleanup();
        }catch(Exception e){failed++;if(e instanceof InterruptedException)Thread.currentThread().interrupt();}
        status=new Status(failed==0?"READY":"RETRY_PENDING",Instant.now(),updated,deleted,failed);
        if(failed>0)org.slf4j.LoggerFactory.getLogger(getClass()).warn("RAG synchronization pending retry: {} operations",failed);
    }
    @PreDestroy public void close(){executor.shutdownNow();}
}
