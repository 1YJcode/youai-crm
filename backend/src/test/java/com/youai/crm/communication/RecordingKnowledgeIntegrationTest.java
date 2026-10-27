package com.youai.crm.communication;

import com.youai.crm.knowledge.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:recording_knowledge;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}") @AutoConfigureMockMvc
class RecordingKnowledgeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CallRecordRepository calls;
    @Autowired CallRecordingRepository recordings;
    @Autowired RecordingTranscriptionRepository results;
    @Autowired KnowledgeRepository articles;
    @Autowired RecordingKnowledgeService knowledge;
    @MockitoBean RecordingAiClient ai;
    @MockitoBean LocalTranscriptionClient local;
    @MockitoBean QwenTranscriptionClient cloud;

    @Test void capturesAnalyzesSearchesAndProtectsSource() throws Exception {
        var call=calls.findAll().getFirst(); Long id=call.getId();
        var audio=recordings.saveAndFlush(new CallRecording(id,"audio/wav",new byte[]{1,2,3}));
        when(local.configured()).thenReturn(true);
        when(local.transcribe(any(),anyString())).thenReturn("unique-case-original");
        when(ai.summaryConfigured()).thenReturn(true);
        when(ai.summarize("unique-case-original")).thenReturn("unique-case-analysis");
        KnowledgeArticle article=null;
        try {
            mvc.perform(post("/api/calls/"+id+"/recording/transcriptions/local").with(user("admin").roles("ADMIN")))
                .andExpect(status().isAccepted());
            long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            while(System.nanoTime()<deadline) {
                article=articles.findAll().stream().filter(a->id.equals(a.sourceCallId) && "COMPLETED".equals(a.aiStatus)).findFirst().orElse(null);
                if(article!=null) break;
                Thread.sleep(30);
            }
            assertNotNull(article); assertNotNull(article.transcribedAt); assertNotNull(article.analyzedAt);
            assertEquals(call.getAgent(),article.salesName); assertEquals(call.getCustomerNo(),article.customerNo);
            assertEquals(audio.getRevision(),article.recordingRevision); assertTrue(article.content.contains("unique-case-analysis"));
            mvc.perform(get("/api/knowledge?keyword=unique-case-original").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
            mvc.perform(get("/api/knowledge/"+article.id).with(user("outsider").roles("SALES"))).andExpect(status().isForbidden());
            var row=results.findById(id+":local").orElseThrow();
            assertEquals(article.id,knowledge.capture(id,"local",audio.getRevision(),row.started,row.transcript,LocalDateTime.now(),"admin",true));
            recordings.saveAndFlush(new CallRecording(id,"audio/wav",new byte[]{4,5}));
            assertNull(knowledge.capture(id,"local",audio.getRevision(),row.started,row.transcript,LocalDateTime.now(),"admin",true));
            mvc.perform(get("/api/calls/"+id+"/recording").param("revision",audio.getRevision()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isConflict());
        } finally {
            articles.findAll().stream().filter(a->id.equals(a.sourceCallId)).forEach(articles::delete);
            results.deleteById(id+":local"); recordings.deleteById(id);
        }
    }
}
