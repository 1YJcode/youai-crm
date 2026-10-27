package com.youai.crm.communication;

import com.youai.crm.knowledge.*;
import com.youai.crm.customer.*;
import com.youai.crm.account.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:company_cases;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}","recording.queue.enabled=false"})
@AutoConfigureMockMvc @Transactional
class CompanyCasePrivacyIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CallRecordRepository calls;
    @Autowired CallRecordingRepository recordings;
    @Autowired RecordingTranscriptionRepository transcriptions;
    @Autowired KnowledgeRepository articles;
    @Autowired RecordingKnowledgeService knowledge;
    @Autowired CustomerRepository customers;
    @Autowired CrmUserRepository users;
    Customer customer;
    CallRecord call;
    KnowledgeArticle source;
    String other;
    String recordingBase;
    @BeforeEach void setup() {
        call=calls.findAll().getFirst();
        customer=customers.findByCustomerNo(call.getCustomerNo()).orElseThrow();
        customer.setOwnerId(users.findByUsernameIgnoreCase("linxi").orElseThrow().getId()); customers.saveAndFlush(customer);
        other=users.findAllByEnabledTrueOrderByDisplayNameAsc().stream()
                .filter(u->!"admin".equals(u.getUsername()) && !"linxi".equals(u.getUsername()))
                .filter(u->u.getRoles().stream().noneMatch(r->"ADMIN".equals(r.getCode()))).findFirst().orElseThrow().getUsername();
        var audio=recordings.saveAndFlush(new CallRecording(call.getId(),"audio/wav",RecordingFixtures.wav()));
        var row=new RecordingTranscription(call.getId()+":cloud",audio.getRevision());
        row.started=LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        row.status="COMPLETED";row.transcript="private-secret-needle 客户原始转写";transcriptions.saveAndFlush(row);
        Long id=knowledge.capture(call.getId(),"cloud",audio.getRevision(),row.started,row.transcript,LocalDateTime.now(),"linxi",false);
        source=articles.findById(id).orElseThrow(); source.category="私有专用分类";articles.saveAndFlush(source);
        recordingBase="/api/calls/"+call.getId()+"/recording";
    }
    ObjectNode request() {
        return json.createObjectNode().put("title","需求澄清案例").put("category","公司共享学习")
                .put("content","public-learning-needle：先澄清需求，再明确跟进时间。")
                .put("version",source.version).put("sharingConfirmed",true);
    }
    long publish() throws Exception {
        return json.readTree(mvc.perform(post("/api/knowledge/"+source.id+"/company-case").with(user("linxi").roles("SALES"))
                .contentType("application/json").content(request().toString())).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).path("id").asLong();
    }
    @Test void onlyCurrentOwnerAndAdminCanReadRawAudioTranscriptionAndPrivateCase() throws Exception {
        for(String suffix:new String[]{"","/playback","/history","/analysis","/review","/transcriptions/local","/transcriptions/cloud"}) {
            mvc.perform(get(recordingBase+suffix).with(user(other).roles("SALES"))).andExpect(status().isForbidden());
            mvc.perform(get(recordingBase+suffix).with(user("linxi").roles("SALES"))).andExpect(status().is(suffix.equals("/review")?409:200));
            mvc.perform(get(recordingBase+suffix).with(user("admin").roles("ADMIN"))).andExpect(status().is(suffix.equals("/review")?409:200));
        }
        mvc.perform(get("/api/knowledge/"+source.id).with(user(other).roles("SALES"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/knowledge").param("keyword","private-secret-needle").with(user(other).roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/knowledge/categories").with(user(other).roles("SALES")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("私有专用分类"))));
        mvc.perform(post(recordingBase+"/transcriptions/cloud/case").with(user(other).roles("SALES"))).andExpect(status().isForbidden());
    }
    @Test void companyCopyIsSharedWithoutExposingPrivateSourceFieldsOrGrantingSourceAccess() throws Exception {
        long shared=publish(); assertNotEquals(source.id,shared);
        var publicView=json.readTree(mvc.perform(get("/api/knowledge/"+shared).with(user(other).roles("SALES")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.companyCase").value(true)).andExpect(jsonPath("$.canAccessSource").value(false))
                .andReturn().getResponse().getContentAsString());
        assertEquals(request().path("content").asText(),publicView.path("content").asText());
        for(String key:new String[]{"sourceCallId","sourceArticleId","sourceKey","derivedFromId","customerNo","customerName","salesUserId","salesName","recordingRevision","recordingSource","transcriptionProvider","callStartedAt","recordingUploadedAt","transcribedAt"})
            assertFalse(publicView.has(key),"Leaked private field: "+key);
        assertFalse(publicView.toString().contains("private-secret-needle"));
        mvc.perform(get("/api/knowledge").param("keyword","public-learning-needle").with(user(other).roles("SALES")))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/knowledge/"+source.id).with(user(other).roles("SALES"))).andExpect(status().isForbidden());
        mvc.perform(get(recordingBase).with(user(other).roles("SALES"))).andExpect(status().isForbidden());
        assertTrue(articles.findById(source.id).orElseThrow().content.contains("private-secret-needle"));
        assertEquals("private-secret-needle 客户原始转写",transcriptions.findById(call.getId()+":cloud").orElseThrow().transcript);
        mvc.perform(get("/api/knowledge/"+source.id).with(user("linxi").roles("SALES")))
                .andExpect(jsonPath("$.companyCaseId").value(shared));
    }
    @Test void transferringCustomerRevokesFormerOwnerButKeepsPublishedCaseAvailable() throws Exception {
        long shared=publish();
        customer.setOwnerId(users.findByUsernameIgnoreCase(other).orElseThrow().getId());customers.saveAndFlush(customer);
        for(String suffix:new String[]{"","/playback","/history","/analysis","/review","/transcriptions/cloud"}) {
            mvc.perform(get(recordingBase+suffix).with(user("linxi").roles("SALES"))).andExpect(status().isForbidden());
            mvc.perform(get(recordingBase+suffix).with(user(other).roles("SALES"))).andExpect(status().is(suffix.equals("/review")?409:200));
        }
        mvc.perform(get("/api/knowledge/"+source.id).with(user("linxi").roles("SALES"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/knowledge/"+shared).with(user("linxi").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sourceCallId").doesNotExist());
        mvc.perform(get("/api/knowledge/"+source.id).with(user(other).roles("SALES"))).andExpect(status().isOk());
        mvc.perform(post("/api/knowledge/"+source.id+"/company-case").with(user("linxi").roles("SALES"))
                .contentType("application/json").content(request().toString())).andExpect(status().isForbidden());
    }
    @Test void publishingRequiresSourcePermissionConfirmationAndCurrentVersionAndPreventsDuplicates() throws Exception {
        mvc.perform(post("/api/knowledge/"+source.id+"/company-case").with(user(other).roles("SALES"))
                .contentType("application/json").content(request().toString())).andExpect(status().isForbidden());
        mvc.perform(post("/api/knowledge/"+source.id+"/company-case").with(user("linxi").roles("SALES"))
                .contentType("application/json").content(request().put("sharingConfirmed",false).toString())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/knowledge/"+source.id+"/company-case").with(user("linxi").roles("SALES"))
                .contentType("application/json").content(request().put("version",source.version+100).toString())).andExpect(status().isConflict());
        publish();
        mvc.perform(post("/api/knowledge/"+source.id+"/company-case").with(user("admin").roles("ADMIN"))
                .contentType("application/json").content(request().toString())).andExpect(status().isConflict());
    }
    @Test void completedLegacyTranscriptionCanCreatePrivateCaseWithoutRetranscribing() throws Exception {
        articles.delete(source);articles.flush();
        var first=json.readTree(mvc.perform(post(recordingBase+"/transcriptions/cloud/case").with(user("linxi").roles("SALES")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post(recordingBase+"/transcriptions/cloud/case").with(user("linxi").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(first.path("id").asLong()));
        mvc.perform(get("/api/knowledge/"+first.path("id").asLong()).with(user(other).roles("SALES"))).andExpect(status().isForbidden());
    }
}
