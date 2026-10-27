package com.youai.crm.communication;
import com.fasterxml.jackson.databind.*;
import com.youai.crm.customer.*;
import com.youai.crm.account.*;
import com.youai.crm.knowledge.*;
import com.youai.crm.task.*;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"server.address=127.0.0.1","spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:customer_review;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}","recording.queue.enabled=false","recording.review.enabled=false","recording.deepseek.automatic-backfill=false"})
@AutoConfigureMockMvc
class CustomerReviewIntegrationTest {
    @org.springframework.boot.test.web.server.LocalServerPort int port;
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired CustomerReviewService service;
    @Autowired CustomerReviewRepository reviews;@Autowired CustomerRepository customers;@Autowired CrmUserRepository users;
    @Autowired CallRecordRepository calls;@Autowired CustomerTranscriptSegmentRepository segments;@Autowired CallIntelligenceRepository analyses;
    @Autowired KnowledgeRepository articles;@Autowired KnowledgeSegmentSourceRepository links;@Autowired FollowUpTaskRepository tasks;
    @MockitoBean CustomerReviewClient model;
    Customer customer;List<CustomerTranscriptSegment> sources=new ArrayList<>();List<Long> taskIds=new ArrayList<>();String stage;
    @BeforeEach void setup()throws Exception{
        customer=new Customer();customer.setCustomerNo("RV"+UUID.randomUUID().toString().substring(0,12));customer.setName("复盘测试客户");customer.setPhone("139"+String.format("%08d",new java.security.SecureRandom().nextInt(100000000)));customer.setCompany("婚恋服务");customer.setSource("测试");customer.setOwner("林夕");customer.setLevel("普通客户");stage="初步沟通";customer.setOwnerId(users.findByUsernameIgnoreCase("linxi").orElseThrow().getId());customer.setStage("初步沟通");customer=customers.saveAndFlush(customer);
        add("客户：我担心费用。销售：下次问清楚需求。",LocalDateTime.now().minusDays(1));
        add("销售：您最看重什么？客户：我希望先了解服务再到店。",LocalDateTime.now());
        when(model.configured()).thenReturn(true);
        var normalizer=new CustomerReviewClient(json,"unused","mock","http://127.0.0.1");
        when(model.normalize(any(),any())).thenAnswer(inv->normalizer.normalize(inv.getArgument(0),inv.getArgument(1)));
        when(model.generate(any(),nullable(JsonNode.class))).thenAnswer(inv->{var input=(JsonNode)inv.getArgument(0);var out=json.createObjectNode().put("summary","客户担心费用，下一次先确认需求和服务，再邀约到店。");
            out.putArray("customerAnalysis").addObject().put("text","客户对费用有顾虑").put("kind","FACT").putArray("evidence").addObject().put("sourceId","call:"+sources.getFirst().id).put("quote","我担心费用");
            out.putArray("nextActions").addObject().put("text","建议询问服务期望并说明费用范围").put("kind","SUGGESTION").putArray("evidence");
            if("已成交".equals(input.path("customerStage").asText()))out.putObject("caseDraft").put("title","先明确服务需求再回应费用顾虑").put("content","客户A关注费用，销售A先了解服务需求。系统记录服务签约，具体转折仍需核对。");return out;});
    }
    void add(String text,LocalDateTime time){
        var c=new CallRecord();c.setCustomerNo(customer.getCustomerNo());c.setCustomerName(customer.getName());c.setAgent("林夕");c.setOwner("林夕");c.setPhone("13800000000");c.setDirection("呼出");c.setStatus("已接通");c.setStartedAt(time);c=calls.saveAndFlush(c);
        var s=new CustomerTranscriptSegment();s.callId=c.getId();s.customerNo=customer.getCustomerNo();s.customerName=customer.getName();s.salesName="林夕";s.recordingSource="UPLOAD";s.provider="cloud";s.recordingRevision=UUID.randomUUID().toString();s.transcript=text;s.sourceTime=time;s.durationSeconds=90;s.addedAt=LocalDateTime.now();s=segments.saveAndFlush(s);sources.add(s);
        var i=new CallIntelligence();i.segmentId=s.id;i.customerNo=s.customerNo;i.state="COMPLETED";i.processingConsent=true;analyses.saveAndFlush(i);
    }
    String path(){return "/api/customers/"+customer.getCustomerNo()+"/review";}
    CustomerReview complete()throws Exception{mvc.perform(post(path()).with(user("linxi").roles("SALES"))).andExpect(status().isOk());var r=reviews.findFirstByCustomerNoOrderByIdDesc(customer.getCustomerNo()).orElseThrow();service.process(r.id);return reviews.findById(r.id).orElseThrow();}
    void won(){customer=customers.findById(customer.getId()).orElseThrow();customer.setStage("已成交");customers.saveAndFlush(customer);}
    @AfterEach void cleanup(){
        for(var a:articles.findAll())if(("customer-success:"+customer.getCustomerNo()).equals(a.sourceKey)){links.deleteAll(links.findAll().stream().filter(l->a.id.equals(l.articleId)).toList());articles.delete(a);}
        reviews.deleteAll(reviews.findByCustomerNoOrderByIdDesc(customer.getCustomerNo(),org.springframework.data.domain.PageRequest.of(0,100)));
        taskIds.forEach(tasks::deleteById);for(var s:sources){analyses.deleteById(s.id);segments.deleteById(s.id);calls.deleteById(s.callId);}sources.clear();
        customer=customers.findById(customer.getId()).orElseThrow();customers.deleteById(customer.getId());
    }
    @Test void cumulativeSnapshotIsIdempotentAndVersionsKeepHistory()throws Exception{
        var first=complete();assertEquals("COMPLETED",first.state);assertEquals(2,json.readTree(first.sourcesJson).size());
        mvc.perform(post(path()).with(user("linxi").roles("SALES"))).andExpect(status().isOk());assertEquals(first.id,reviews.findFirstByCustomerNoOrderByIdDesc(customer.getCustomerNo()).orElseThrow().id);
        add("客户：下周可以到店。",LocalDateTime.now().plusDays(1));var second=complete();assertNotEquals(first.id,second.id);assertEquals(3,json.readTree(second.sourcesJson).size());assertEquals(first.resultJson,reviews.findById(first.id).orElseThrow().resultJson);
        verify(model).generate(any(),isNull());verify(model).generate(any(),argThat(v->v!=null&&v.path("summary").asText().contains("费用")));
    }
    @Test void nonOwnerCannotReadGenerateOrPublishAndTransferRevokesAccess()throws Exception{
        var r=complete();mvc.perform(get(path()).with(user("chenchen").roles("SALES"))).andExpect(status().isForbidden());
        mvc.perform(post(path()).with(user("chenchen").roles("SALES"))).andExpect(status().isForbidden());
        customer=customers.findById(customer.getId()).orElseThrow();customer.setOwnerId(users.findByUsernameIgnoreCase("chenchen").orElseThrow().getId());customers.saveAndFlush(customer);
        mvc.perform(get(path()+"/"+r.id).with(user("linxi").roles("SALES"))).andExpect(status().isForbidden());mvc.perform(get(path()).with(user("chenchen").roles("SALES"))).andExpect(status().isOk());
    }
    @Test void failedModelDoesNotBlockFollowupAndCanRetry()throws Exception{
        doThrow(new IllegalStateException("provider-secret-must-not-leak")).when(model).generate(any(),nullable(JsonNode.class));var r=complete();service.process(r.id);service.process(r.id);assertEquals("FAILED",reviews.findById(r.id).orElseThrow().state);
        mvc.perform(get(path()).with(user("linxi").roles("SALES"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("provider-secret"))));
        mvc.perform(post(path()).with(user("linxi").roles("SALES"))).andExpect(status().isOk());assertEquals("QUEUED",reviews.findById(r.id).orElseThrow().state);
    }
    @Test void noConsentPreventsSendingAnySourceToModel()throws Exception{
        var i=analyses.findById(sources.getFirst().id).orElseThrow();i.processingConsent=false;analyses.saveAndFlush(i);
        mvc.perform(post(path()).with(user("linxi").roles("SALES"))).andExpect(status().isBadRequest());verify(model,never()).generate(any(),any());
    }
    @Test void changingSourceDuringGenerationCannotSaveStaleResult()throws Exception{
        doAnswer(inv->{won();return json.createObjectNode().put("summary","旧版本不应成为最新总结");}).when(model).generate(any(),nullable(JsonNode.class));var r=complete();assertEquals("SUPERSEDED",r.state);assertNull(r.resultJson);
    }
    @Test void expiredLeaseRecoversAndConcurrentWorkerDoesNotDuplicateModelCall()throws Exception{
        service.enqueue(customer.getCustomerNo(),false);var r=reviews.findFirstByCustomerNoOrderByIdDesc(customer.getCustomerNo()).orElseThrow();r.state="PROCESSING";r.token="old-process";r.leaseUntil=LocalDateTime.now().minusMinutes(1);reviews.saveAndFlush(r);
        var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        doAnswer(inv->{entered.countDown();assertTrue(release.await(10,java.util.concurrent.TimeUnit.SECONDS));return json.createObjectNode().put("summary","服务重启后恢复的复盘");}).when(model).generate(any(),nullable(JsonNode.class));
        var future=java.util.concurrent.CompletableFuture.runAsync(()->service.process(r.id));assertTrue(entered.await(10,java.util.concurrent.TimeUnit.SECONDS));
        try{service.process(r.id);}finally{release.countDown();}future.get(10,java.util.concurrent.TimeUnit.SECONDS);
        assertEquals("COMPLETED",reviews.findById(r.id).orElseThrow().state);verify(model,times(1)).generate(any(),nullable(JsonNode.class));
    }
    @Test void confirmedManualFollowupTriggersNewSummaryWithoutNewRecording()throws Exception{
        var old=complete();var t=new FollowUpTask();t.setCustomerId(customer.getCustomerNo());t.setCustomerName(customer.getName());t.setOwner("林夕");t.setTitle("已核对客户希望先了解服务，再确定到店时间");t.setFollowedAt(LocalDateTime.now());t.setDueAt(LocalDateTime.now());t.setTaskType("电话跟进");t.setPriority("普通");t.setStatus("done");t.setCompleted(true);t=tasks.saveAndFlush(t);taskIds.add(t.getId());
        var next=complete();assertNotEquals(old.id,next.id);assertEquals(3,json.readTree(next.sourcesJson).size());
    }
    @Test void revertingCustomerStageReusesMatchingCompletedRevision()throws Exception{
        var before=complete();won();complete();customer=customers.findById(customer.getId()).orElseThrow();customer.setStage("初步沟通");customers.saveAndFlush(customer);
        mvc.perform(get(path()).with(user("linxi").roles("SALES"))).andExpect(status().isOk()).andExpect(jsonPath("$.latest.id").value(before.id)).andExpect(jsonPath("$.completed.id").value(before.id)).andExpect(jsonPath("$.outdated").value(false));
    }
    String publishBody(CustomerReview r,boolean consent,String text)throws Exception{return json.writeValueAsString(Map.of("version",r.version,"title","婚恋服务成交复盘","content",text,"sharingConfirmed",consent));}
    @Test void publishRequiresWonAndConfirmationThenSharesOnlyEditedCaseIdempotently()throws Exception{
        var before=complete();mvc.perform(post(path()+"/"+before.id+"/publish").with(user("linxi").roles("SALES")).contentType("application/json").content(publishBody(before,true,"客户A的脱敏案例"))).andExpect(status().isConflict());
        won();var r=complete();assertTrue(json.readTree(r.resultJson).has("caseDraft"));
        mvc.perform(post(path()+"/"+r.id+"/publish").with(user("linxi").roles("SALES")).contentType("application/json").content(publishBody(r,false,"客户A的脱敏案例"))).andExpect(status().isBadRequest());
        mvc.perform(post(path()+"/"+r.id+"/publish").with(user("chenchen").roles("SALES")).contentType("application/json").content(publishBody(r,true,"客户A的脱敏案例"))).andExpect(status().isForbidden());
        mvc.perform(post(path()+"/"+r.id+"/publish").with(user("linxi").roles("SALES")).contentType("application/json").content(publishBody(r,true,customer.getName()+"的案例"))).andExpect(status().isBadRequest());
        var response=mvc.perform(post(path()+"/"+r.id+"/publish").with(user("linxi").roles("SALES")).contentType("application/json").content(publishBody(r,true,"客户A的脱敏案例"))).andExpect(status().isOk()).andReturn();var articleId=json.readTree(response.getResponse().getContentAsString()).path("articleId").asLong();
        mvc.perform(post(path()+"/"+r.id+"/publish").with(user("linxi").roles("SALES")).contentType("application/json").content(publishBody(r,true,"客户A的脱敏案例"))).andExpect(status().isOk()).andExpect(jsonPath("$.articleId").value(articleId));
        assertEquals(2,links.sources(articleId).size());mvc.perform(get("/api/knowledge/"+articleId).with(user("chenchen").roles("SALES"))).andExpect(status().isOk()).andExpect(jsonPath("$.content").value("客户A的脱敏案例")).andExpect(jsonPath("$.sourceSegments").doesNotExist()).andExpect(jsonPath("$.customerNo").doesNotExist());
    }
    @Test void fabricatedEvidenceIsRemovedAndOneCallCannotProveImprovement()throws Exception{
        var input=json.createObjectNode();input.putArray("sources").addObject().put("id","call:1").put("type","CALL").put("callId",1).put("content","我担心费用");
        var raw=json.createObjectNode().put("summary","草稿");raw.putArray("customerAnalysis").addObject().put("text","虚构事实").put("kind","FACT").putArray("evidence").addObject().put("sourceId","call:1").put("quote","并不存在的原话");
        raw.putArray("progress").addObject().put("text","有改善待验证").put("kind","OBSERVED_CHANGE").putArray("evidence").addObject().put("sourceId","call:1").put("quote","我担心费用");
        var normalized=new CustomerReviewClient(json,"","mock","http://127.0.0.1").normalize(raw,input);assertTrue(normalized.path("customerAnalysis").isEmpty());assertEquals("PENDING_VERIFICATION",normalized.path("progress").get(0).path("kind").asText());assertFalse(normalized.has("caseDraft"));
    }
    @Test @org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable(named="RUN_CUSTOMER_REVIEW_BROWSER",matches="true")
    void browserCanReadRevisionsVerifyEvidenceAndReviewBeforePublication()throws Exception{
        complete();won();complete();
        var root=java.nio.file.Path.of(System.getProperty("user.dir")).toAbsolutePath();if(root.getFileName().toString().equals("backend"))root=root.getParent();
        var builder=new ProcessBuilder("node",root.resolve("scripts/test-recording-e2e.cjs").toString(),Integer.toString(port),"customer-review");builder.environment().put("PIPELINE_CUSTOMER_ID",customer.getCustomerNo());
        var log=root.resolve(".tmp_customer_review_browser.log");var process=builder.directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean finished=process.waitFor(90,java.util.concurrent.TimeUnit.SECONDS);if(!finished)process.destroyForcibly();assertTrue(finished,"Review browser timed out");assertEquals(0,process.exitValue(),java.nio.file.Files.readString(log));
    }
}
