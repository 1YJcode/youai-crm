package com.youai.crm.communication;
import com.fasterxml.jackson.databind.*;
import com.youai.crm.customer.*;
import com.youai.crm.account.*;
import com.youai.crm.task.FollowUpTaskRepository;
import com.youai.crm.knowledge.KnowledgeRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import java.time.LocalDateTime;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties={"recording.deepseek.automatic-backfill=false","spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:call_intelligence;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}","recording.queue.poll-ms=600000","recording.storage.directory=./target/call-intelligence-audio"})
@AutoConfigureMockMvc
class CallIntelligenceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired FollowUpTemplates templates;
    @Autowired RecordingQueue queue;
    @Autowired RecordingJobRepository jobs;
    @Autowired CustomerTranscriptSegmentRepository segments;
    @Autowired CallIntelligenceRepository intelligence;
    @Autowired CallIntelligenceService service;
    @Autowired CustomerRepository customers;
    @Autowired CrmUserRepository users;
    @Autowired CallRecordRepository calls;
    @Autowired FollowUpTaskRepository tasks;
    @Autowired KnowledgeRepository articles;
    @Autowired CallFollowupWorkRepository work;
    @Autowired CallFollowupWorkService workService;
    @Autowired SystemNotificationRepository notifications;
    @MockitoBean QwenTranscriptionClient cloud;
    @MockitoBean DeepSeekAnalysisClient deepseek;
    Customer customer;
    @BeforeEach void setup()throws Exception{
        work.deleteAll();notifications.findAll().stream().filter(n->n.getNotificationKind()!=null).forEach(notifications::delete);
        jobs.deleteAll();for(var old:intelligence.findAll())if(old.followUpId!=null)tasks.deleteById(old.followUpId);intelligence.deleteAll();segments.deleteAll();customer=customers.findAll().getFirst();customer.setOwnerId(users.findByUsernameIgnoreCase("linxi").orElseThrow().getId());customers.saveAndFlush(customer);
        when(cloud.configured()).thenReturn(true);when(cloud.transcribe(any(),anyString())).thenReturn("客户：已经加微信，周五下午再联系我。");
        when(deepseek.configured()).thenReturn(true);when(deepseek.model()).thenReturn("deepseek-flash");
        var raw=json.readTree("{\"summary\":\"客户约定周五联系\",\"recommendedTemplate\":\"一通\",\"recommendationReason\":\"首次沟通\",\"customerAnalysis\":[{\"text\":\"已加微信\",\"evidence\":\"已经加微信\"}],\"templates\":[{\"name\":\"一通\",\"fields\":[{\"label\":\"加微信：\",\"value\":\"已加微信\",\"evidence\":\"已经加微信\"}]}]}");
        for(var t:templates.all()){boolean present=false;for(var node:raw.path("templates"))if(t.name().equals(node.path("name").asText()))present=true;if(!present)((com.fasterxml.jackson.databind.node.ArrayNode)raw.path("templates")).addObject().put("name",t.name()).putArray("fields");}
        ((com.fasterxml.jackson.databind.node.ObjectNode)raw).put("title","约定周五继续联系").put("titleEvidence","周五下午再联系我");
        var normalized=new DeepSeekAnalysisClient(json,templates,"unused","deepseek-flash","http://127.0.0.1").normalize(raw,"客户：已经加微信，周五下午再联系我。");
        when(deepseek.analyze(anyString(),nullable(String.class))).thenReturn(normalized);
    }
    Long upload()throws Exception{
        var response=mvc.perform(multipart("/api/call-uploads").file(new MockMultipartFile("file","test.wav","audio/wav",RecordingFixtures.wav()))
                .param("customerId",customer.getCustomerNo()).param("autoProcess","true").param("processingConsent","true").with(user("linxi").roles("SALES")))
                .andExpect(status().isCreated()).andReturn().getResponse();
        return json.readTree(response.getContentAsString()).path("id").asLong();
    }
    CallIntelligence await(String state)throws Exception{
        long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        do{var rows=intelligence.findAll();if(!rows.isEmpty()&&state.equals(rows.getFirst().state))return rows.getFirst();queue.dispatch();Thread.sleep(25);}while(System.nanoTime()<deadline);
        fail("Expected state "+state+"; got "+intelligence.findAll().stream().map(i->i.state+":"+i.error).toList());return null;
    }
    @Test void automaticUploadAnalyzesAndConfirmationCreatesExactlyOneFollowupWithPrivateKnowledge()throws Exception{
        long before=tasks.count();upload();var i=await("COMPLETED");assertEquals(1,segments.count());verify(cloud,times(1)).transcribe(any(),anyString());verify(deepseek,times(1)).analyze(anyString(),nullable(String.class));
        String path="/api/transcript-segments/"+i.segmentId+"/intelligence";
        var view=json.readTree(mvc.perform(get(path).with(user("linxi").roles("SALES"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        var body=json.createObjectNode().put("version",view.path("version").asLong()).put("template","一通").put("content","销售核对：已加微信，周五下午联系。");
        var saved=json.readTree(mvc.perform(post(path+"/confirm").with(user("linxi").roles("SALES")).contentType("application/json").content(body.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post(path+"/confirm").with(user("linxi").roles("SALES")).contentType("application/json").content(body.toString())).andExpect(status().isOk()).andExpect(jsonPath("$.followUp.id").value(saved.path("followUp").path("id").asLong()));
        assertEquals(before+1,tasks.count());mvc.perform(get("/api/customers/"+customer.getCustomerNo()+"/follow-ups").with(user("linxi").roles("SALES"))).andExpect(content().string(org.hamcrest.Matchers.containsString("销售核对")));
        mvc.perform(get("/api/call-followup-work/by-followup/"+saved.path("followUp").path("id").asLong()).with(user("linxi").roles("SALES"))).andExpect(status().isOk()).andExpect(jsonPath("$.generationMethod").value("AI_CONFIRMED")).andExpect(jsonPath("$.submittedByName").value("林夕")).andExpect(jsonPath("$.segmentId").value(i.segmentId));
        mvc.perform(get("/api/call-followup-work/by-followup/"+saved.path("followUp").path("id").asLong()).with(user("chenchen").roles("SALES"))).andExpect(status().isForbidden());
        mvc.perform(get(path).with(user("chenchen").roles("SALES"))).andExpect(status().isForbidden());
        var segment=segments.findById(i.segmentId).orElseThrow();assertTrue(articles.findById(segment.sourceArticleId).orElseThrow().content.contains("销售已确认跟进"));
        customer=customers.findById(customer.getId()).orElseThrow();customer.setCollaboratorIds(java.util.Set.of(users.findByUsernameIgnoreCase("chenchen").orElseThrow().getId()));customers.saveAndFlush(customer);
        mvc.perform(get("/api/customers/"+customer.getCustomerNo()+"/follow-ups").with(user("chenchen").roles("SALES")))
                .andExpect(result->{assertTrue(java.util.Set.of(200,403).contains(result.getResponse().getStatus()));assertFalse(result.getResponse().getContentAsString().contains("销售核对"));});
        assertEquals(0,articles.findAll().stream().filter(a->a.companyCase&&a.customerNo!=null&&a.customerNo.equals(customer.getCustomerNo())).count());
        mvc.perform(post(path).with(user("linxi").roles("SALES")).contentType("application/json").content("{\"processingConsent\":true}")).andExpect(status().isConflict());
    }
    @Test void missingKeyKeepsCompletedTranscriptionAndResumesAnalysisAfterConfiguration()throws Exception{
        when(deepseek.configured()).thenReturn(false);upload();var i=await("WAITING_CONFIG");verify(deepseek,never()).analyze(anyString(),nullable(String.class));assertEquals(1,segments.count());
        when(deepseek.configured()).thenReturn(true);service.resumeConfigured();await("COMPLETED");verify(cloud,times(1)).transcribe(any(),anyString());
    }
    @Test void automaticProcessingRequiresConsentAndDoesNotLeaveAnEmptyCall()throws Exception{
        long before=calls.count();mvc.perform(multipart("/api/call-uploads").file(new MockMultipartFile("file","test.wav","audio/wav",RecordingFixtures.wav()))
                .param("customerId",customer.getCustomerNo()).param("autoProcess","true").param("processingConsent","false").with(user("linxi").roles("SALES"))).andExpect(status().isBadRequest());
        assertEquals(before,calls.count());assertEquals(0,jobs.count());
    }
    @Test void retryingAnalysisDoesNotRetranscribeAudio()throws Exception{
        var normalized=deepseek.analyze("x",null);clearInvocations(deepseek);
        when(deepseek.analyze(anyString(),nullable(String.class))).thenThrow(new java.io.IOException("temporary")).thenReturn(normalized);
        upload();await("QUEUED");long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while(System.nanoTime()<deadline){var row=intelligence.findAll().getFirst();var job=jobs.findById(row.activeJobId).orElseThrow();if(job.attempts>0&&"QUEUED".equals(job.state)){job.availableAt=LocalDateTime.now().minusSeconds(1);jobs.saveAndFlush(job);break;}queue.dispatch();Thread.sleep(25);}
        await("COMPLETED");verify(cloud,times(1)).transcribe(any(),anyString());verify(deepseek,times(2)).analyze(anyString(),nullable(String.class));
    }
    @Test void customerTransferPreventsOldActorFromSendingTextForAnalysis()throws Exception{
        when(deepseek.configured()).thenReturn(false);upload();var i=await("WAITING_CONFIG");customer=customers.findById(customer.getId()).orElseThrow();customer.setOwnerId(users.findByUsernameIgnoreCase("chenchen").orElseThrow().getId());customers.saveAndFlush(customer);
        when(deepseek.configured()).thenReturn(true);service.resumeConfigured();await("FAILED");verify(deepseek,never()).analyze(anyString(),nullable(String.class));
        mvc.perform(get("/api/transcript-segments/"+i.segmentId+"/intelligence").with(user("linxi").roles("SALES"))).andExpect(status().isForbidden());
    }
    @Test void perCallManualSubmissionIsAtomicAndIdempotentEvenWithoutAnalysis()throws Exception{
        when(deepseek.configured()).thenReturn(false);long first=upload();await("WAITING_CONFIG");long second=upload();
        assertEquals(2,work.count());var original=work.findById(first).orElseThrow();var reminderAt=original.remindAt;workService.register(first);
        assertEquals(reminderAt,work.findById(first).orElseThrow().remindAt);assertEquals(2,work.count());
        long before=tasks.count();var follow=json.createObjectNode().put("title","手动确认本次沟通").put("customer",customer.getName()).put("customerId",customer.getCustomerNo()).put("owner","林夕").put("dueAt",LocalDateTime.now().toString()).put("type","电话跟进").put("completed",true);
        var body=json.createObjectNode().set("followUp",follow);
        String url="/api/call-followup-work/"+first+"/submit";
        mvc.perform(post(url).with(user("linxi").roles("SALES")).contentType("application/json").content(body.toString())).andExpect(status().isOk());
        mvc.perform(post(url).with(user("linxi").roles("SALES")).contentType("application/json").content(body.toString())).andExpect(status().isOk());
        assertEquals(before+1,tasks.count());assertNotNull(work.findById(first).orElseThrow().followUpId);assertNull(work.findById(second).orElseThrow().followUpId);
        mvc.perform(get("/api/call-followup-work").with(user("chenchen").roles("SALES"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/call-followup-work/"+second+"/submit").with(user("chenchen").roles("SALES")).contentType("application/json").content(body.toString())).andExpect(status().isForbidden());
    }
    @Test void remindersPersistDeduplicateEscalateAndTransferWithCustomer()throws Exception{
        when(deepseek.configured()).thenReturn(false);long id=upload();await("WAITING_CONFIG");var w=work.findById(id).orElseThrow();
        workService.scanAt(w.remindAt.minusSeconds(1));assertEquals(0,notifications.count());
        workService.scanAt(w.remindAt);workService.scanAt(w.remindAt.plusMinutes(1));assertEquals(1,notifications.count());
        mvc.perform(patch("/api/notifications/read").with(user("linxi").roles("SALES")).contentType("application/json").content(json.createObjectNode().put("read",true).set("notificationIds",json.valueToTree(java.util.List.of(notifications.findAll().getFirst().getNotificationKey()))).toString())).andExpect(status().isNoContent());
        assertNull(work.findById(id).orElseThrow().followUpId);
        workService.scanAt(w.escalateAt);assertTrue(notifications.findAll().stream().anyMatch(n->"CALL_OVERDUE".equals(n.getNotificationKind())));
        long count=notifications.count();workService.scanAt(w.escalateAt.plusHours(1));assertEquals(count,notifications.count());
        customer=customers.findById(customer.getId()).orElseThrow();customer.setOwnerId(users.findByUsernameIgnoreCase("chenchen").orElseThrow().getId());customers.saveAndFlush(customer);
        workService.scanAt(w.escalateAt.plusHours(2));
        mvc.perform(get("/api/system-notifications").with(user("linxi").roles("SALES"))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/system-notifications").with(user("chenchen").roles("SALES"))).andExpect(status().isOk()).andExpect(jsonPath("$[0].callId").value(id));
    }
    @Test void dailySummaryIsServerDeduplicatedAndSideEffectsRollback()throws Exception{
        when(deepseek.configured()).thenReturn(false);long id=upload();await("WAITING_CONFIG");var w=work.findById(id).orElseThrow();w.remindAt=LocalDateTime.now().minusMinutes(1);work.saveAndFlush(w);
        for(int n=0;n<2;n++)mvc.perform(post("/api/call-followup-work/daily-summary").with(user("linxi").roles("SALES"))).andExpect(status().isOk()).andExpect(jsonPath("$.pending").value(1));
        mvc.perform(post("/api/call-followup-work/daily-summary").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.pending").value(0));
        assertEquals(1,notifications.findAll().stream().filter(n->"CALL_SUMMARY".equals(n.getNotificationKind())).count());
        long before=tasks.count();var follow=json.createObjectNode().put("title","不可部分保存").put("customer",customer.getName()).put("customerId",customer.getCustomerNo()).put("owner","林夕").put("dueAt",LocalDateTime.now().toString()).put("type","电话跟进").put("completed",true);
        var body=json.createObjectNode();body.set("followUp",follow);body.set("nextTask",follow.deepCopy().put("completed",true));
        mvc.perform(post("/api/call-followup-work/"+id+"/submit").with(user("linxi").roles("SALES")).contentType("application/json").content(body.toString())).andExpect(status().isConflict());
        assertEquals(before,tasks.count());assertNull(work.findById(id).orElseThrow().followUpId);
    }
    @Test void replacementAndConcurrentSubmissionsKeepOneCallOneFollowup()throws Exception{
        when(deepseek.configured()).thenReturn(false);long id=upload();await("WAITING_CONFIG");var due=work.findById(id).orElseThrow().remindAt;
        mvc.perform(multipart("/api/calls/"+id+"/recording").file(new MockMultipartFile("file","replacement.wav","audio/wav",RecordingFixtures.wav())).with(user("linxi").roles("SALES"))).andExpect(status().isOk());
        assertEquals(1,work.count());assertEquals(due,work.findById(id).orElseThrow().remindAt);
        long before=tasks.count();var follow=json.createObjectNode().put("title","并发确认仅保存一次").put("customer",customer.getName()).put("customerId",customer.getCustomerNo()).put("owner","林夕").put("dueAt",LocalDateTime.now().toString()).put("type","电话跟进").put("completed",true);
        var body=json.createObjectNode().set("followUp",follow).toString();
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
            var ready=new java.util.concurrent.CountDownLatch(2);var start=new java.util.concurrent.CountDownLatch(1);var results=new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
            for(int n=0;n<2;n++)results.add(pool.submit(()->{ready.countDown();start.await();return mvc.perform(post("/api/call-followup-work/"+id+"/submit").with(user("linxi").roles("SALES")).contentType("application/json").content(body)).andReturn().getResponse().getStatus();}));
            assertTrue(ready.await(5,java.util.concurrent.TimeUnit.SECONDS));start.countDown();for(var result:results)assertEquals(200,result.get(10,java.util.concurrent.TimeUnit.SECONDS));
        }
        assertEquals(before+1,tasks.count());long notices=notifications.count();workService.scanAt(due.plusDays(2));assertEquals(notices,notifications.count());
    }
    @Test void staleAiDraftCannotSaveAndConcurrentReminderScansDoNotDuplicate()throws Exception{
        long id=upload();var i=await("COMPLETED");var follow=json.createObjectNode().put("title","过期草稿不应保存").put("customer",customer.getName()).put("customerId",customer.getCustomerNo()).put("owner","林夕").put("dueAt",LocalDateTime.now().toString()).put("type","电话跟进").put("completed",true);
        var body=json.createObjectNode().put("segmentId",i.segmentId).put("analysisVersion",i.version+10).put("template","一通");body.set("followUp",follow);long before=tasks.count();
        mvc.perform(post("/api/call-followup-work/"+id+"/submit").with(user("linxi").roles("SALES")).contentType("application/json").content(body.toString())).andExpect(status().isConflict());assertEquals(before,tasks.count());assertNull(work.findById(id).orElseThrow().followUpId);
        var due=work.findById(id).orElseThrow().remindAt;
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){
            var first=pool.submit(()->workService.scanAt(due));var second=pool.submit(()->workService.scanAt(due));first.get(10,java.util.concurrent.TimeUnit.SECONDS);second.get(10,java.util.concurrent.TimeUnit.SECONDS);
        }assertEquals(1,notifications.findAll().stream().filter(n->"CALL_FOLLOWUP".equals(n.getNotificationKind())).count());
    }
    @Test void historicalTranscriptCanBeExplicitlySelectedWithoutAutomaticBackfill()throws Exception{
        long callId=upload();var i=await("COMPLETED");work.deleteById(callId);
        mvc.perform(get("/api/call-followup-work/sources").param("customerNo",customer.getCustomerNo()).with(user("linxi").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].segmentId").value(i.segmentId)).andExpect(jsonPath("$[0].intelligence.analysis.recommendedTemplate").value("一通"));
        assertEquals(0,work.count());assertEquals(0,notifications.count());
        mvc.perform(get("/api/call-followup-work/sources").param("customerNo",customer.getCustomerNo()).with(user("chenchen").roles("SALES"))).andExpect(status().isForbidden());
        for(int n=0;n<2;n++)mvc.perform(post("/api/call-followup-work/from-transcript/"+i.segmentId).with(user("linxi").roles("SALES"))).andExpect(status().isOk());
        assertEquals(1,work.count());assertTrue(work.findById(callId).orElseThrow().remindAt.isAfter(LocalDateTime.now().plusMinutes(29)));
    }
    @Test void oldUnconfirmedSourcesAutomaticallyReceiveAllTemplatesWithoutHistoricalReminderBackfill()throws Exception{
        when(deepseek.configured()).thenReturn(false);long callId=upload();var row=await("WAITING_CONFIG");work.deleteById(callId);row.promptVersion="call-followup-v1";row.state="WAITING_CONSENT";intelligence.saveAndFlush(row);
        when(deepseek.configured()).thenReturn(true);org.springframework.test.util.ReflectionTestUtils.setField(service,"automaticBackfill",true);
        try{service.fillAutomaticDrafts();var completed=await("COMPLETED");assertEquals(templates.all().size(),json.readTree(completed.analysisJson).path("templates").size());assertEquals("约定周五继续联系",json.readTree(completed.analysisJson).path("title").asText());assertEquals(0,work.count());service.fillAutomaticDrafts();verify(deepseek,times(1)).analyze(anyString(),nullable(String.class));}
        finally{org.springframework.test.util.ReflectionTestUtils.setField(service,"automaticBackfill",false);}
    }
    @Test void automaticHistoricalAnalysisUsesEnabledAdminWhenCustomerHasNoOwner()throws Exception{
        when(deepseek.configured()).thenReturn(false);long id=upload();var row=await("WAITING_CONFIG");row.promptVersion="call-followup-v1";row.state="WAITING_CONSENT";intelligence.saveAndFlush(row);
        customer=customers.findById(customer.getId()).orElseThrow();customer.setOwnerId(null);customers.saveAndFlush(customer);
        when(deepseek.configured()).thenReturn(true);org.springframework.test.util.ReflectionTestUtils.setField(service,"automaticBackfill",true);
        try{service.fillAutomaticDrafts();var completed=await("COMPLETED");assertEquals(users.findByUsernameIgnoreCase("admin").orElseThrow().getId(),completed.requestedUserId);assertNull(work.findById(id).orElseThrow().followUpId);}
        finally{org.springframework.test.util.ReflectionTestUtils.setField(service,"automaticBackfill",false);}
    }
}
