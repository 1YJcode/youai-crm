package com.youai.crm.communication;

import java.time.LocalDateTime;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.youai.crm.customer.*;
import com.youai.crm.knowledge.*;
import com.youai.crm.common.NotFoundException;

@Service
public class CustomerTranscriptService {
    @org.springframework.beans.factory.annotation.Autowired private CallFollowupWorkRepository followupWork;
    private final CustomerRepository customers;
    private final CustomerAccessPolicy access;
    private final CustomerTranscriptRepository archives;
    private final CustomerTranscriptSegmentRepository segments;
    private final KnowledgeRepository articles;
    private final KnowledgeSegmentSourceRepository sources;
    private final CallIntelligenceRepository intelligence;
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    public CustomerTranscriptService(CustomerRepository customers,CustomerAccessPolicy access,CustomerTranscriptRepository archives,
            CustomerTranscriptSegmentRepository segments,KnowledgeRepository articles,KnowledgeSegmentSourceRepository sources,CallIntelligenceRepository intelligence,com.fasterxml.jackson.databind.ObjectMapper json) {
        this.customers=customers;this.access=access;this.archives=archives;this.segments=segments;this.articles=articles;this.sources=sources;
        this.intelligence=intelligence;this.json=json;
    }
    public record SegmentView(Long id,Long callId,String recordingRevision,String provider,String transcript,
            String customerNo,String customerName,Long salesUserId,String salesName,String recordingSource,
            LocalDateTime sourceTime,String timeKind,LocalDateTime callStartedAt,LocalDateTime recordingUploadedAt,
            Integer durationSeconds,LocalDateTime transcribedAt,LocalDateTime addedAt) {}
    public record ArchiveView(String customerNo,String customerName,LocalDateTime createdAt,long totalSegments,
            long totalDurationSeconds,long unknownDurationCount,Page<SegmentView> segments) {}
    public static SegmentView view(CustomerTranscriptSegment s,boolean text) {
        return new SegmentView(s.id,s.callId,s.recordingRevision,s.provider,text?s.transcript:null,s.customerNo,s.customerName,
                s.salesUserId,s.salesName,s.recordingSource,s.sourceTime,"SYSTEM".equals(s.recordingSource)&&s.callStartedAt!=null?"CALL":"UPLOAD",
                s.callStartedAt,s.recordingUploadedAt,s.durationSeconds,s.transcribedAt,s.addedAt);
    }
    @Transactional
    public void append(CallRecord call,CallRecording audio,String provider,LocalDateTime started,String text,LocalDateTime completed,Long sourceArticleId) {
        if(text==null || text.isBlank()) return;
        // A customer row exists before the archive; serialize first creation across separate calls/workers.
        if(customers.lockByCustomerNo(call.getCustomerNo()).isEmpty()) return;
        if(segments.findByCallIdAndRecordingRevision(call.getId(),audio.getRevision()).isPresent()) return;
        if(!archives.existsById(call.getCustomerNo())) archives.saveAndFlush(new CustomerTranscript(call.getCustomerNo()));
        var s=new CustomerTranscriptSegment();s.customerNo=call.getCustomerNo();s.callId=call.getId();s.recordingRevision=audio.getRevision();
        s.provider=provider;s.transcript=text;s.customerName=call.getCustomerName();s.salesUserId=call.getAgentUserId();s.salesName=call.getAgent();
        s.recordingSource=call.getRecordingSource();s.callStartedAt="SYSTEM".equals(s.recordingSource)?call.getStartedAt():null;
        s.recordingUploadedAt=audio.getUploadedAt();s.sourceTime=s.callStartedAt!=null?s.callStartedAt:s.recordingUploadedAt;
        s.durationSeconds=call.getDurationSeconds()>0?call.getDurationSeconds():null;s.transcriptionStartedAt=started;
        s.transcribedAt=completed;s.addedAt=LocalDateTime.now();s.sourceArticleId=sourceArticleId;segments.saveAndFlush(s);
        var work=followupWork.findById(call.getId()).orElse(null);
        if(work!=null&&work.followUpId!=null&&work.sourceSegmentId==null){
            work.sourceSegmentId=s.id;
            if(sourceArticleId!=null)articles.lockById(sourceArticleId).filter(a->!a.companyCase&&s.customerNo.equals(a.customerNo)).ifPresent(a->{
                a.content+="\n\n销售已确认跟进（手动填写）\n通话编号："+s.callId+"\n"+work.confirmedContent;articles.save(a);
            });
        }
    }
    @Transactional(readOnly=true)
    public ArchiveView get(String customerNo,int page,Authentication auth) {
        access.requireAccess(customerNo,auth);
        var customer=customers.findByCustomerNo(customerNo).orElseThrow(()->new NotFoundException("未找到客户"));
        var archive=archives.findById(customerNo).orElse(null);var totals=segments.totals(customerNo);
        return new ArchiveView(customerNo,customer.getName(),archive==null?null:archive.createdAt,totals.getSegments(),totals.getDuration(),totals.getUnknownDuration(),
                segments.findByCustomerNo(customerNo,PageRequest.of(Math.max(0,page),20,Sort.by("sourceTime").and(Sort.by("id")))).map(s->view(s,true)));
    }
    @Transactional(readOnly=true) public SegmentView source(String customerNo,Long id,Authentication auth){
        access.requireAccess(customerNo,auth);var s=segments.findById(id).filter(v->customerNo.equals(v.customerNo)).orElseThrow(()->new NotFoundException("未找到来源转写"));return view(s,true);
    }
    @Transactional
    public Long selectionCase(String customerNo,List<Long> ids,Authentication auth) {
        access.requireAccess(customerNo,auth);
        customers.lockByCustomerNo(customerNo).orElseThrow(()->new NotFoundException("未找到客户"));
        var selected=segments.findAllById(new TreeSet<>(ids));
        if(selected.size()!=new HashSet<>(ids).size() || selected.stream().anyMatch(s->!customerNo.equals(s.customerNo)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"请选择此客户转写档案中的有效段落");
        selected.sort(Comparator.comparing((CustomerTranscriptSegment s)->s.sourceTime).thenComparing(s->s.id));
        Map<Long,CallIntelligence> analysisVersions=new HashMap<>();for(var i:intelligence.findAllById(ids))analysisVersions.put(i.segmentId,i);
        String versionKey=new TreeSet<>(ids).stream().map(id->{var work=followupWork.findById(segments.findById(id).orElseThrow().callId).orElse(null);return id+":"+(analysisVersions.containsKey(id)?analysisVersions.get(id).version:"none")+":"+(work==null?"none":work.followUpId);}).collect(java.util.stream.Collectors.joining(","));
        String key="customer-transcript:"+customerNo+":"+RecordingStorage.sha256(versionKey.getBytes(StandardCharsets.UTF_8));
        var existing=articles.findBySourceKey(key);if(existing.isPresent())return existing.get().id;
        var first=selected.getFirst();var a=new KnowledgeArticle();a.sourceKey=key;a.sourceCallId=first.callId;a.recordingRevision=first.recordingRevision;
        a.customerNo=customerNo;a.customerName=first.customerName;a.salesUserId=first.salesUserId;a.salesName=first.salesName;
        a.recordingSource=first.recordingSource;a.transcriptionProvider=first.provider;a.callStartedAt=first.callStartedAt;a.recordingUploadedAt=first.recordingUploadedAt;
        a.title="客户转写选段 · "+first.customerName;a.category="客户私有转写选段";a.aiStatus="NOT_CONFIGURED";
        StringBuilder content=new StringBuilder();
        for(var s:selected) {
            content.append("【来源段落 ").append(s.id).append("】\n客户：").append(s.customerName).append("（").append(s.customerNo).append("）\n销售：")
                    .append(s.salesName).append("\n").append(s.callStartedAt==null?"上传时间：":"通话时间：").append(s.sourceTime)
                    .append("\n录音时长：").append(s.durationSeconds==null?"未记录":s.durationSeconds+"秒")
                    .append("\n通话编号：").append(s.callId).append("\n\n").append(s.transcript).append("\n\n");
            var i=analysisVersions.get(s.id);
            var work=followupWork.findById(s.callId).orElse(null);
            if(work!=null&&work.confirmedContent!=null&&(i==null||i.confirmedContent==null))content.append("\n销售已确认跟进\n").append(work.confirmedContent).append("\n\n");
            if(i!=null){
                if(i.analysisJson!=null)try{
                    var analysis=json.readTree(i.analysisJson);content.append("AI分析（待核对）\n").append(analysis.path("summary").asText()).append('\n');
                    for(String category:List.of("customerAnalysis","salesCoaching","reminders"))for(var finding:analysis.path(category))
                        content.append('[').append(finding.path("kind").asText()).append("] ").append(finding.path("text").asText()).append("\n依据：").append(finding.path("evidence").asText()).append('\n');
                }catch(java.io.IOException e){throw new IllegalStateException("分析数据无法读取");}
                if(i.confirmedContent!=null)content.append("\n销售已确认跟进\n模板：").append(i.confirmedTemplate).append("\n").append(i.confirmedContent).append("\n\n");
            }
            if(content.length()>1000000)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"选段内容过长，请减少选择数量");
        }
        a.content=content.toString().trim();a.createdBy=auth.getName();a.updatedBy=auth.getName();articles.saveAndFlush(a);
        for(var s:selected)sources.save(new KnowledgeSegmentSource(a.id,s.id));sources.flush();return a.id;
    }
    public List<SegmentView> sources(Long articleId){return sources.sources(articleId).stream().map(s->view(s,false)).toList();}
    @Transactional public void copySources(Long from,Long to){for(var s:sources.sources(from))sources.save(new KnowledgeSegmentSource(to,s.id));sources.flush();}
}
