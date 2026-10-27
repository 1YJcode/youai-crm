package com.youai.crm.knowledge;

import com.youai.crm.account.AccessPolicy;
import com.youai.crm.customer.CustomerAccessPolicy;
import com.youai.crm.communication.*;
import com.youai.crm.common.ConflictException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class RagKnowledgeService {
    private final KnowledgeRepository articles;private final CustomerTranscriptSegmentRepository segments;
    private final CustomerAccessPolicy access;private final AccessPolicy users;private final CallIntelligenceService intelligence;
    private final CallFollowupWorkRepository work;private final RagNotebookClient rag;private final RagAnswerClient model;
    public RagKnowledgeService(KnowledgeRepository articles,CustomerTranscriptSegmentRepository segments,CustomerAccessPolicy access,
            AccessPolicy users,CallIntelligenceService intelligence,CallFollowupWorkRepository work,RagNotebookClient rag,RagAnswerClient model){
        this.articles=articles;this.segments=segments;this.access=access;this.users=users;this.intelligence=intelligence;this.work=work;this.rag=rag;this.model=model;
    }
    public record Material(String id,String version,String title,String content,Long articleId,Long segmentId,String customerNo){}
    public record Citation(String title,String quote,Long articleId,Long segmentId,String customerNo,String sourceId,String version){}
    public record Answer(String answer,List<Citation> citations,String engine,boolean grounded){}
    public boolean configured(){return rag.configured()&&model.configured();}
    public Material source(String id,String customerNo,String version,Authentication auth){
        var result=materials(customerNo,null,auth).get(id);if(result==null)throw new com.youai.crm.common.NotFoundException("引用资料已不可访问");
        if(!result.version.equals(version))throw new ConflictException("引用资料已更新，请重新查询");return result;
    }
    private boolean shared(KnowledgeArticle a){return a.companyCase||(a.sourceCallId==null&&a.customerNo==null);}
    private String fingerprint(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private LinkedHashMap<String,Material> materials(String customerNo,Long segmentId,Authentication auth){
        users.currentUserId(auth);if(customerNo!=null)access.requireAccess(customerNo,auth);
        var result=new LinkedHashMap<String,Material>();
        articles.findAll((root,q,cb)->cb.or(cb.isTrue(root.get("companyCase")),cb.and(cb.isNull(root.get("sourceCallId")),cb.isNull(root.get("customerNo"))))).forEach(a->{
            result.put("article:"+a.id,new Material("article:"+a.id,String.valueOf(a.version),a.title,a.content,a.id,null,null));
        });
        if(customerNo!=null){
            var selected=segmentId==null?segments.findByCustomerNo(customerNo,PageRequest.of(0,100,Sort.by(Sort.Direction.DESC,"sourceTime"))).getContent():segments.findById(segmentId).map(List::of).orElse(List.of());
            for(var s:selected){
                var v=CustomerTranscriptService.view(s,true);if(!customerNo.equals(v.customerNo()))throw new org.springframework.security.access.AccessDeniedException("通话来源不属于当前客户");
                var i=intelligence.get(v.id(),auth);var content=new StringBuilder("通话来源："+v.sourceTime()+"\n销售："+v.salesName()+"\n\n"+v.transcript());
                String title="客户通话 · "+v.sourceTime();if(i.analysis()!=null){title=i.analysis().path("title").asText(title);content.append("\n\nAI摘要（待核对）：").append(i.analysis().path("summary").asText());}
                if(i.confirmedContent()!=null)content.append("\n\n销售已确认跟进：").append(i.confirmedContent());
                else work.findById(v.callId()).filter(w->w.confirmedContent!=null).ifPresent(w->content.append("\n\n销售已确认跟进：").append(w.confirmedContent));
                String text=content.toString();result.put("segment:"+v.id(),new Material("segment:"+v.id(),fingerprint(v.recordingRevision()+text),title,text,null,v.id(),customerNo));
            }
        }else if(segmentId!=null)throw new IllegalArgumentException("请选择客户后查询通话资料");
        if(result.size()>500)throw new IllegalArgumentException("资料超过单次检索范围，请缩小查询范围");return result;
    }
    private List<Map<String,Object>> scopes(Collection<Material> materials,String customerNo){
        var company=new ArrayList<Map<String,Object>>();var personal=new ArrayList<Map<String,Object>>();
        for(var m:materials){var d=Map.<String,Object>of("id",m.id,"version",m.version,"title",m.title,"content",m.content);if(m.customerNo==null)company.add(d);else personal.add(d);}
        var result=new ArrayList<Map<String,Object>>();result.add(Map.of("id","company","documents",company));if(customerNo!=null)result.add(Map.of("id","customer:"+customerNo,"documents",personal));return result;
    }
    public Answer ask(String question,String customerNo,Long segmentId,boolean coaching,Authentication auth)throws Exception{
        var snapshot=materials(customerNo,segmentId,auth);
        if(snapshot.isEmpty()){
            if(rag.configured())try{rag.search(question,scopes(snapshot.values(),customerNo));}catch(Exception ignored){}
            return new Answer("暂无可检索的知识。请先发布公司案例、添加通用知识，或选择有转写的客户。",List.of(),"RAG-Notebook",false);
        }
        var matches=rag.search(question,scopes(snapshot.values(),customerNo));
        var fresh=materials(customerNo,segmentId,auth);var evidence=new ArrayList<Map<String,Object>>();
        for(var match:matches){
            var material=fresh.get(match.path("id").asText());var original=snapshot.get(match.path("id").asText());
            String text=match.path("content").asText();
            if(material==null||original==null||!material.version.equals(original.version)||!material.version.equals(match.path("version").asText())||text.isBlank()||text.length()>2000||!material.content.contains(text))continue;
            evidence.add(Map.of("id",material.id,"title",material.title,"content",text,"chunk",match.path("chunk").asInt()));
        }
        if(evidence.isEmpty())return new Answer("没有找到可核验的相关资料，暂不能给出有来源的回答。",List.of(),"RAG-Notebook",false);
        var response=model.answer(question,evidence,coaching);var after=materials(customerNo,segmentId,auth);var citations=new ArrayList<Citation>();
        for(var citation:response.path("citations")){
            String id=citation.path("id").asText(),quote=citation.path("quote").asText();int chunk=citation.path("chunk").asInt(-1);
            var material=after.get(id);var old=fresh.get(id);if(material==null||old==null||!material.version.equals(old.version))throw new ConflictException("引用资料已变化，请重新查询");
            if(quote.isBlank()||quote.length()>600)continue;
            if(evidence.stream().noneMatch(e->id.equals(e.get("id"))&&Objects.equals(chunk,e.get("chunk"))&&((String)e.get("content")).contains(quote)))continue;
            citations.add(new Citation(material.title,quote,material.articleId,material.segmentId,material.customerNo,material.id,material.version));
        }
        String answer=response.path("answer").asText("");
        if(citations.isEmpty()||answer.isBlank())return new Answer("模型未提供可核验的引用依据，请换一种问法或补充知识资料。",List.of(),"RAG-Notebook",false);
        return new Answer(answer.substring(0,Math.min(12000,answer.length())),citations,"RAG-Notebook",true);
    }
}
