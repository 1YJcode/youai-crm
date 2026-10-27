package com.youai.crm.communication;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.TestingAuthenticationToken;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:storage_queue;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}",
        "recording.queue.poll-ms=600000","recording.storage.directory=./target/storage-queue-audio"})
@AutoConfigureMockMvc
class RecordingStorageQueueIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CallRecordRepository calls;
    @Autowired CallRecordingRepository recordings;
    @Autowired RecordingTranscriptionRepository results;
    @Autowired RecordingJobRepository jobs;
    @Autowired RecordingArchiveRepository archives;
    @Autowired RecordingStorage storage;
    @Autowired RecordingQueue queue;
    @MockitoBean LocalTranscriptionClient local;
    @MockitoBean QwenTranscriptionClient cloud;
    @MockitoBean RecordingAiClient ai;
    Long callId;
    final TestingAuthenticationToken admin=new TestingAuthenticationToken("admin","unused","ROLE_ADMIN");
    @BeforeEach void setup() {
        callId=calls.findAll().getFirst().getId(); jobs.deleteAll(); results.deleteAll(); archives.deleteAll(); recordings.deleteAll();
        when(local.configured()).thenReturn(true);
    }
    void upload() throws Exception {
        mvc.perform(multipart("/api/calls/"+callId+"/recording")
                .file(new MockMultipartFile("file","test.wav","audio/wav",RecordingFixtures.wav()))
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }
    RecordingJob awaitJob(String state) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        RecordingJob job;
        do {
            job=jobs.findAll().getFirst();
            if(state.equals(job.state)) return job;
            Thread.sleep(20);
        } while(System.nanoTime()<deadline);
        fail("Expected "+state+" but got "+job.state); return job;
    }
    @Test void newUploadStoresOnlyMetadataAndReplacementKeepsAudioAndTranscript() throws Exception {
        upload(); var old=recordings.findById(callId).orElseThrow();
        assertNull(old.getAudio()); assertNotNull(old.getStorageKey()); assertEquals("file",old.getStorageBackend());
        assertArrayEquals(RecordingFixtures.wav(),storage.read(old));
        var text=new RecordingTranscription(callId+":local",old.getRevision());
        text.status="COMPLETED"; text.transcript="原始转写内容"; results.saveAndFlush(text);
        upload();
        var archive=archives.findById(old.getRevision()).orElseThrow();
        assertTrue(archive.snapshot.contains("原始转写内容"));
        assertArrayEquals(RecordingFixtures.wav(),storage.read(archive.recording()));
        mvc.perform(get("/api/calls/"+callId+"/recording").param("revision",old.getRevision()).with(user("admin").roles("ADMIN")))
                .andExpect(content().bytes(RecordingFixtures.wav()));
        mvc.perform(get("/api/calls/"+callId+"/recording/history").with(user("outsider").roles("SALES")))
                .andExpect(status().isForbidden());
    }
    @Test void migrationVerifiesBytesAndPreservesRecordingRevisionAndText() throws Exception {
        var legacy=recordings.saveAndFlush(new CallRecording(callId,"audio/wav",RecordingFixtures.wav()));
        var text=new RecordingTranscription(callId+":cloud",legacy.getRevision()); text.transcript="保留文字"; text.status="COMPLETED"; results.saveAndFlush(text);
        mvc.perform(post("/api/recording-storage/migrate").with(user("linxi").roles("SALES"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/recording-storage/migrate").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.migrated").value(1));
        var moved=recordings.findById(callId).orElseThrow(); assertNull(moved.getAudio());
        assertEquals(legacy.getRevision(),moved.getRevision()); assertArrayEquals(RecordingFixtures.wav(),storage.read(moved));
        assertEquals("保留文字",results.findById(callId+":cloud").orElseThrow().transcript);
        mvc.perform(post("/api/recording-storage/migrate").with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$.migrated").value(0));
    }
    @Test void queuedJobSurvivesExpiredWorkerLeaseAndRejectsDuplicateSubmission() throws Exception {
        upload(); var audio=recordings.findById(callId).orElseThrow();
        when(local.transcribe(any(),anyString())).thenReturn("恢复完成");
        queue.enqueue(callId,audio.getRevision(),"local",admin);
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->queue.enqueue(callId,audio.getRevision(),"local",admin));
        var job=jobs.findAll().getFirst(); job.state="RUNNING"; job.token="lost-worker";
        job.attempts=1; job.leaseUntil=LocalDateTime.now().minusMinutes(1); jobs.saveAndFlush(job);
        queue.dispatch(); var finished=awaitJob("COMPLETED");
        assertEquals(2,finished.attempts); assertEquals("恢复完成",results.findById(callId+":local").orElseThrow().transcript);
        assertEquals(0,jobs.finish(job.id,"lost-worker","FAILED",LocalDateTime.now()));
    }
    @Test void transientFailureRetriesWithoutLosingQueueEntry() throws Exception {
        upload(); when(local.transcribe(any(),anyString())).thenThrow(new java.io.IOException("temporary")).thenReturn("重试完成");
        queue.enqueue(callId,recordings.findRevision(callId).orElseThrow(),"local",admin);
        queue.dispatch(); var job=awaitJob("QUEUED");
        // Wait for first attempt to finish, rather than reading the original queued state.
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(job.attempts==0 && System.nanoTime()<deadline) { Thread.sleep(20); job=jobs.findById(job.id).orElseThrow(); }
        job=awaitJob("QUEUED"); assertEquals(1,job.attempts);
        job.availableAt=LocalDateTime.now().minusSeconds(1); jobs.saveAndFlush(job);
        queue.dispatch(); assertEquals(2,awaitJob("COMPLETED").attempts);
    }
    @Test void replacementCancelsQueuedJobAndMissingActorFailsWithoutExternalCall() throws Exception {
        upload(); queue.enqueue(callId,recordings.findRevision(callId).orElseThrow(),"local",admin);
        upload(); queue.dispatch(); awaitJob("CANCELLED"); verify(local,never()).transcribe(any(),anyString());
        jobs.deleteAll(); queue.enqueue(callId,recordings.findRevision(callId).orElseThrow(),"local",admin);
        var job=jobs.findAll().getFirst(); job.actor="deleted-account"; jobs.saveAndFlush(job);
        queue.dispatch(); awaitJob("FAILED"); verify(local,never()).transcribe(any(),anyString());
    }
}
