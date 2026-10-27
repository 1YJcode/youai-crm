package com.youai.crm.communication;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.util.UUID;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:storage_s3;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}","recording.queue.enabled=false"})
@AutoConfigureMockMvc @TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named="S3_TEST_ENDPOINT",matches=".+")
class RecordingS3IntegrationTest {
    static final String BUCKET="recording-test-"+UUID.randomUUID();
    @DynamicPropertySource static void config(DynamicPropertyRegistry r) {
        r.add("recording.storage.backend",()->"s3"); r.add("recording.storage.s3.bucket",()->BUCKET);
        r.add("recording.storage.s3.endpoint",()->System.getenv("S3_TEST_ENDPOINT"));
        r.add("recording.storage.s3.access-key",()->"storage-test-user");
        r.add("recording.storage.s3.secret-key",()->"storage-test-password-20261025");
        r.add("recording.storage.s3.path-style",()->true);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CallRecordRepository calls;
    @Autowired CallRecordingRepository recordings;
    @Autowired RecordingUploadRepository uploads;
    @Autowired RecordingStorage storage;
    S3Client client;
    final HttpClient http=HttpClient.newHttpClient();
    @BeforeAll void bucket() {
        client=S3Client.builder().region(Region.US_EAST_1).endpointOverride(URI.create(System.getenv("S3_TEST_ENDPOINT")))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("storage-test-user","storage-test-password-20261025")))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build();
        client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
    }
    @AfterAll void close() { client.close(); }
    JsonNode prepare(Long callId,String customerId) throws Exception {
        var request=json.createObjectNode(); if(callId!=null) request.put("callId",callId); else request.put("customerId",customerId);
        request.put("size",RecordingFixtures.wav().length); request.put("note","direct upload test");
        return json.readTree(mvc.perform(post("/api/recording-uploads").with(user("admin").roles("ADMIN"))
                .contentType("application/json").content(request.toString())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }
    void putFile(JsonNode ticket,byte[] audio) throws Exception {
        var result=http.send(HttpRequest.newBuilder(URI.create(ticket.path("url").asText()))
                .header("Content-Type","application/octet-stream").PUT(HttpRequest.BodyPublishers.ofByteArray(audio)).build(),HttpResponse.BodyHandlers.discarding());
        assertEquals(200,result.statusCode());
    }
    @Test void directUploadChecksPermissionsAndCannotOverwriteValidatedObject() throws Exception {
        Long id=calls.findAll().getFirst().getId(); var ticket=prepare(id,null); assertTrue(ticket.path("direct").asBoolean());
        putFile(ticket,RecordingFixtures.wav());
        String complete="/api/recording-uploads/"+ticket.path("id").asText()+"/complete";
        mvc.perform(post(complete).with(user("linxi").roles("SALES"))).andExpect(status().isForbidden());
        mvc.perform(post(complete).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        var audio=recordings.findById(id).orElseThrow(); assertNull(audio.getAudio()); assertEquals("s3",audio.getStorageBackend());
        assertArrayEquals(RecordingFixtures.wav(),storage.read(audio));
        byte[] changed=RecordingFixtures.wav(); changed[changed.length-1]=42; putFile(ticket,changed);
        assertArrayEquals(RecordingFixtures.wav(),storage.read(audio));
        var playback=json.readTree(mvc.perform(get("/api/calls/"+id+"/recording/playback").with(user("admin").roles("ADMIN")))
                .andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse().getContentAsString());
        assertTrue(playback.path("url").isNull());
        mvc.perform(get("/api/calls/"+id+"/recording").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().bytes(RecordingFixtures.wav()));
        mvc.perform(get("/api/calls/"+id+"/recording")).andExpect(status().isUnauthorized());
        assertEquals(403,http.send(HttpRequest.newBuilder(URI.create(System.getenv("S3_TEST_ENDPOINT")+"/"+BUCKET+"/"+audio.getStorageKey())).GET().build(),HttpResponse.BodyHandlers.discarding()).statusCode());
        mvc.perform(post(complete).with(user("admin").roles("ADMIN"))).andExpect(status().isConflict());
    }
    @Test void newCallIsCreatedOnlyAfterValidatedUploadAndCorruptFileIsRejected() throws Exception {
        var existing=calls.findAll().getFirst();
        long count=calls.count(); var bad=prepare(null,existing.getCustomerNo()); byte[] invalid=new byte[RecordingFixtures.wav().length]; putFile(bad,invalid);
        mvc.perform(post("/api/recording-uploads/"+bad.path("id").asText()+"/complete").with(user("admin").roles("ADMIN")))
                .andExpect(status().isUnsupportedMediaType());
        assertEquals(count,calls.count());
        var good=prepare(null,existing.getCustomerNo()); putFile(good,RecordingFixtures.wav());
        var saved=json.readTree(mvc.perform(post("/api/recording-uploads/"+good.path("id").asText()+"/complete").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertTrue(saved.path("id").asLong()>0); assertEquals(count+1,calls.count());
        assertNotNull(recordings.findById(saved.path("id").asLong()).orElseThrow().getStorageKey());
    }
}
