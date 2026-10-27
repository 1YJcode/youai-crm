package com.youai.crm.communication;

import com.youai.crm.knowledge.*;
import jakarta.persistence.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class RecordingKnowledgeService {
    private final KnowledgeRepository articles;
    private final CallRecordRepository calls;
    private final EntityManager entities;
    private final CustomerTranscriptService transcripts;
    public RecordingKnowledgeService(KnowledgeRepository articles, CallRecordRepository calls, EntityManager entities,CustomerTranscriptService transcripts) {
        this.articles=articles; this.calls=calls; this.entities=entities;
        this.transcripts=transcripts;
    }
    @Transactional
    public Long capture(Long callId, String provider, String revision, LocalDateTime started,
                        String text, LocalDateTime completed, String actor, boolean analyze) {
        var audio=entities.find(CallRecording.class,callId,LockModeType.PESSIMISTIC_WRITE);
        var row=entities.find(RecordingTranscription.class,callId+":"+provider,LockModeType.PESSIMISTIC_WRITE);
        if(audio==null || !audio.getRevision().equals(revision) || row==null || !revision.equals(row.revision)
                || !java.util.Objects.equals(started,row.started) || !"COMPLETED".equals(row.status)) return null;
        var call=calls.findById(callId).orElseThrow();
        String key=callId+":"+provider+":"+revision+":"+started;
        var a=articles.findBySourceKey(key).orElseGet(KnowledgeArticle::new);
        if(a.id!=null) {transcripts.append(call,audio,provider,started,text,a.transcribedAt,a.id);return a.id;}
        a.sourceKey=key; a.sourceCallId=callId; a.recordingRevision=revision;
        a.recordingSource=call.getRecordingSource(); a.transcriptionProvider=provider;
        a.customerNo=call.getCustomerNo(); a.customerName=call.getCustomerName();
        a.salesUserId=call.getAgentUserId(); a.salesName=call.getAgent();
        a.callStartedAt=call.getStartedAt(); a.recordingUploadedAt=audio.getUploadedAt();
        a.transcribedAt=completed; a.aiStatus=analyze?"PROCESSING":"NOT_CONFIGURED";
        a.category="通话案例";
        a.title=call.getCustomerName()+" · "+call.getAgent()+" · "+call.getStartedAt();
        a.content="客户："+a.customerName+"（"+a.customerNo+"）\n销售："+a.salesName+"\n\n转写原文\n"+text;
        a.createdBy=actor; a.updatedBy=actor;
        articles.saveAndFlush(a);
        transcripts.append(call,audio,provider,started,text,completed,a.id);
        return a.id;
    }
    @Transactional
    public void captureLegacy(Long callId,String revision,LocalDateTime started,String text) {
        var call=calls.lockById(callId).orElse(null);if(call==null)return;
        var audio=entities.find(CallRecording.class,callId,LockModeType.PESSIMISTIC_WRITE);
        if(audio==null || !revision.equals(audio.getRevision()) || !"COMPLETED".equals(audio.getAnalysisStatus())
                || !java.util.Objects.equals(started,audio.getAnalysisStartedAt()))return;
        transcripts.append(call,audio,"legacy",started,text,LocalDateTime.now(),null);
    }
    @Transactional
    public void finish(Long id, String summary) {
        var a=articles.findById(id).orElse(null);
        if(a==null) return;
        a.aiStatus=summary==null?"FAILED":"COMPLETED";
        if(summary!=null) { a.content="AI 分析（待人工核对）\n"+summary+"\n\n"+a.content; a.analyzedAt=LocalDateTime.now(); }
        articles.saveAndFlush(a);
    }
}
