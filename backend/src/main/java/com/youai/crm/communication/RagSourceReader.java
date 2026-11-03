package com.youai.crm.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youai.crm.knowledge.RagKnowledgeService.Material;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Shared deterministic rendering for authorized reads and internal indexing. No HTTP endpoint. */
@Service
public class RagSourceReader {
    private final CallIntelligenceRepository intelligence;
    private final CallFollowupWorkRepository work;
    private final ObjectMapper json;
    public RagSourceReader(CallIntelligenceRepository intelligence, CallFollowupWorkRepository work, ObjectMapper json) {
        this.intelligence=intelligence; this.work=work; this.json=json;
    }
    public Material render(CustomerTranscriptSegment source) {
        var v=CustomerTranscriptService.view(source,true);
        var i=intelligence.findById(v.id()).orElse(null);
        var content=new StringBuilder("通话来源："+v.sourceTime()+"\n销售："+v.salesName()+"\n\n"+v.transcript());
        String title="客户通话 · "+v.sourceTime();
        try {
            if(i!=null && i.analysisJson!=null) {
                var analysis=json.readTree(i.analysisJson);
                title=analysis.path("title").asText(title);
                content.append("\n\nAI摘要（待核对）：").append(analysis.path("summary").asText());
            }
            if(i!=null && i.confirmedContent!=null) content.append("\n\n销售已确认跟进：").append(i.confirmedContent);
            else work.findById(v.callId()).filter(w->w.confirmedContent!=null).ifPresent(w->content.append("\n\n销售已确认跟进：").append(w.confirmedContent));
            String text=content.toString();
            String version=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((v.recordingRevision()+title+text).getBytes(StandardCharsets.UTF_8)));
            return new Material("segment:"+v.id(),version,title,text,null,v.id(),v.customerNo());
        } catch(Exception e) { throw new IllegalStateException("索引来源暂不可读取",e); }
    }
}
