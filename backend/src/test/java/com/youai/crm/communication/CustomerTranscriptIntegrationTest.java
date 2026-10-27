package com.youai.crm.communication;

import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.customer.*;
import com.youai.crm.knowledge.KnowledgeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@SpringBootTest(properties={"spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:customer_transcript;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}","recording.queue.enabled=false"})
@AutoConfigureMockMvc @Transactional
class CustomerTranscriptIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CustomerRepository customers;
    @Autowired CrmUserRepository users;
    @Autowired CallRecordRepository calls;
    @Autowired CallRecordingRepository recordings;
    @Autowired RecordingTranscriptionRepository results;
    @Autowired RecordingKnowledgeService knowledge;
    @Autowired CustomerTranscriptRepository archives;
    @Autowired CustomerTranscriptSegmentRepository segments;
    @Autowired RecordingArchiveRepository audioArchives;
    @Autowired KnowledgeRepository articles;
    @Autowired javax.sql.DataSource datasource;
    @Autowired jakarta.persistence.EntityManager entities;
    Customer customer;
    @BeforeEach void setup(){segments.deleteAll();archives.deleteAll();results.deleteAll();audioArchives.deleteAll();customer=customers.findAll().getFirst();customer.setOwnerId(users.findByUsernameIgnoreCase("linxi").orElseThrow().getId());customers.saveAndFlush(customer);}
    CallRecord call(String sales,int duration,String source){
        var c=new CallRecord();c.setCustomerNo(customer.getCustomerNo());c.setCustomerName(customer.getName());c.setPhone(customer.getPhone());
        c.setOwner("林夕");c.setAgent(sales);c.setAgentUserId(users.findByUsernameIgnoreCase(sales).orElseThrow().getId());
        c.setDirection("外部通话");c.setStatus("已接通");c.setRecordingSource(source);c.setDurationSeconds(duration);
        c.setStartedAt(LocalDateTime.of(2026,10,20,10,0));calls.saveAndFlush(c);
        recordings.saveAndFlush(new CallRecording(c.getId(),"audio/wav",RecordingFixtures.wav()));return c;
    }
    void transcribe(CallRecord call,String provider,String text){
        var audio=recordings.findById(call.getId()).orElseThrow();var row=new RecordingTranscription(call.getId()+":"+provider,audio.getRevision());
        row.status="COMPLETED";row.started=LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);row.transcript=text;results.saveAndFlush(row);
        knowledge.capture(call.getId(),provider,audio.getRevision(),row.started,text,LocalDateTime.now(),"linxi",false);
    }
    String url(){return "/api/customers/"+customer.getCustomerNo()+"/transcript";}
    @Test void twoCallsAppendToOneCustomerArchiveWithImmutableSalesSourceTimeAndDuration()throws Exception{
        var a=call("linxi",42,"SYSTEM");transcribe(a,"cloud","第一次沟通");
        var b=call("chenchen",75,"UPLOAD");transcribe(b,"cloud","后续沟通");
        assertEquals(1,archives.count());var list=segments.findByCustomerNo(customer.getCustomerNo(),org.springframework.data.domain.Pageable.unpaged()).getContent();
        assertEquals(2,list.size());var first=list.stream().filter(s->s.callId.equals(a.getId())).findFirst().orElseThrow();
        var next=list.stream().filter(s->s.callId.equals(b.getId())).findFirst().orElseThrow();
        assertEquals("linxi",first.salesName);assertEquals(42,first.durationSeconds);assertEquals(a.getStartedAt(),first.callStartedAt);
        assertEquals("chenchen",next.salesName);assertEquals(75,next.durationSeconds);assertNull(next.callStartedAt);assertEquals(next.recordingUploadedAt,next.sourceTime);
        customer.setOwnerId(users.findByUsernameIgnoreCase("chenchen").orElseThrow().getId());customers.saveAndFlush(customer);
        mvc.perform(get(url()).with(user("linxi").roles("SALES"))).andExpect(status().isForbidden());
        mvc.perform(get(url()).with(user("chenchen").roles("SALES"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSegments").value(2)).andExpect(jsonPath("$.totalDurationSeconds").value(117))
                .andExpect(header().string("Cache-Control","no-store"));
        assertEquals("linxi",segments.findById(first.id).orElseThrow().salesName);
    }
    @Test void retryOrAlternativeProviderDoesNotDuplicateOrOverwriteAndNewRecordingVersionAppends(){
        var c=call("linxi",42,"UPLOAD");transcribe(c,"cloud","保留的第一份结果");var original=recordings.findById(c.getId()).orElseThrow().getRevision();
        transcribe(c,"cloud","重试结果");transcribe(c,"local","本地结果");assertEquals(1,segments.count());
        assertEquals("保留的第一份结果",segments.findByCallIdAndRecordingRevision(c.getId(),original).orElseThrow().transcript);
        recordings.saveAndFlush(new CallRecording(c.getId(),"audio/wav",RecordingFixtures.wav()));c.setDurationSeconds(64);calls.saveAndFlush(c);
        transcribe(c,"cloud","新录音结果");assertEquals(2,segments.count());assertEquals(1,archives.count());
        assertEquals(42,segments.findByCallIdAndRecordingRevision(c.getId(),original).orElseThrow().durationSeconds);
    }
    @Test void multipleSegmentsCanFormOneCompanyCaseWhileSourcesRemainPrivate()throws Exception{
        transcribe(call("linxi",42,"UPLOAD"),"cloud","private-archive-alpha");transcribe(call("chenchen",75,"UPLOAD"),"cloud","private-archive-beta");
        var ids=segments.findAll().stream().map(s->s.id).toList();String body=json.writeValueAsString(java.util.Map.of("segmentIds",ids));
        mvc.perform(post(url()+"/case").with(user("chenchen").roles("SALES")).contentType("application/json").content(body)).andExpect(status().isForbidden());
        var selection=json.readTree(mvc.perform(post(url()+"/case").with(user("linxi").roles("SALES")).contentType("application/json").content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());long id=selection.path("id").asLong();
        var source=json.readTree(mvc.perform(get("/api/knowledge/"+id).with(user("linxi").roles("SALES")))
                .andExpect(jsonPath("$.sourceSegments.length()").value(2)).andReturn().getResponse().getContentAsString());
        assertTrue(source.path("content").asText().contains("private-archive-alpha"));assertTrue(source.path("content").asText().contains("private-archive-beta"));
        var publish=json.createObjectNode().put("title","公司学习案例").put("category","公司案例").put("content","只发布整理后的经验")
                .put("version",source.path("version").asLong()).put("sharingConfirmed",true);
        var shared=json.readTree(mvc.perform(post("/api/knowledge/"+id+"/company-case").with(user("linxi").roles("SALES")).contentType("application/json").content(publish.toString()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sourceSegments.length()").value(2)).andReturn().getResponse().getContentAsString());
        mvc.perform(get("/api/knowledge/"+shared.path("id").asLong()).with(user("chenchen").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sourceSegments").doesNotExist()).andExpect(jsonPath("$.sourceCallId").doesNotExist());
        mvc.perform(get(url()).with(user("chenchen").roles("SALES"))).andExpect(status().isForbidden());
    }
    @Test void backfillCopiesExistingAndHistoricalTranscriptsOnceWithoutInventingHistoricalDuration()throws Exception{
        var c=call("linxi",52,"UPLOAD");var audio=recordings.findById(c.getId()).orElseThrow();
        for(String provider:java.util.List.of("local","cloud")){var row=new RecordingTranscription(c.getId()+":"+provider,audio.getRevision());row.status="COMPLETED";row.transcript=provider+" existing";results.saveAndFlush(row);}
        var old=new RecordingArchive();old.revision=java.util.UUID.randomUUID().toString();old.callId=c.getId();old.mediaType="audio/wav";
        old.storageBackend="file";old.storageKey="recordings/"+c.getId()+"/"+old.revision;old.audioSize=76;old.audioSha256="0".repeat(64);
        old.uploadedAt=LocalDateTime.of(2026,10,1,9,0);old.archivedAt=LocalDateTime.now();old.snapshot="{\"cloud\":{\"status\":\"COMPLETED\",\"transcript\":\"historical text\"}}";audioArchives.saveAndFlush(old);
        entities.flush();var connection=org.springframework.jdbc.datasource.DataSourceUtils.getConnection(datasource);
        var migration=new db.migration.V42__BackfillCustomerTranscripts();migration.backfill(connection);migration.backfill(connection);entities.clear();
        assertEquals(1,archives.count());assertEquals(2,segments.count());
        var current=segments.findByCallIdAndRecordingRevision(c.getId(),audio.getRevision()).orElseThrow();assertEquals("cloud existing",current.transcript);assertEquals(52,current.durationSeconds);
        var history=segments.findByCallIdAndRecordingRevision(c.getId(),old.revision).orElseThrow();assertEquals("historical text",history.transcript);assertNull(history.durationSeconds);assertNull(history.transcribedAt);
    }
}
