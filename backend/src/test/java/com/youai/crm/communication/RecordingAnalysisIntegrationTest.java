package com.youai.crm.communication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@SpringBootTest(properties="spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:recording_analysis;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}")
@AutoConfigureMockMvc
class RecordingAnalysisIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CallRecordRepository calls;
    @Autowired CallRecordingRepository recordings;
    @MockitoBean RecordingAiClient ai;
    @MockitoBean QwenTranscriptionClient qwen;
    @MockitoBean LocalTranscriptionClient local;
    @Autowired RecordingTranscriptionRepository transcriptions;

    @Test
    void dualTranscriptionsStaySeparateAndRequireCloudConsent() throws Exception {
        Long id=calls.findAll().getFirst().getId();
        recordings.saveAndFlush(new CallRecording(id,"audio/wav",new byte[]{1,2,3}));
        when(local.configured()).thenReturn(true);
        when(qwen.configured()).thenReturn(true);
        when(local.transcribe(any(),anyString())).thenReturn("本地结果");
        when(qwen.transcribe(any(),anyString())).thenReturn("云端结果");
        String base="/api/calls/"+id+"/recording/transcriptions/";
        try {
            mvc.perform(post(base+"cloud").with(user("admin").roles("ADMIN")))
                    .andExpect(status().isBadRequest());
            verify(qwen,never()).transcribe(any(),anyString());
            for(String provider:java.util.List.of("local","cloud")) {
                mvc.perform(post(base+provider).with(user("admin").roles("ADMIN"))
                        .contentType("application/json").content("{\"cloudConsent\":true}"))
                        .andExpect(status().isAccepted());
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
                while(!"COMPLETED".equals(transcriptions.findById(id+":"+provider).orElseThrow().status)
                        && System.nanoTime()<deadline) Thread.sleep(25);
                mvc.perform(get(base+provider).with(user("admin").roles("ADMIN")))
                        .andExpect(jsonPath("$.status").value("COMPLETED"))
                        .andExpect(jsonPath("$.transcript").value(provider.equals("local")?"本地结果":"云端结果"));
            }
            mvc.perform(get(base+"local")).andExpect(status().isUnauthorized());
            recordings.saveAndFlush(new CallRecording(id,"audio/wav",new byte[]{4,5,6}));
            for(String provider:java.util.List.of("local","cloud"))
                mvc.perform(get(base+provider).with(user("admin").roles("ADMIN")))
                        .andExpect(jsonPath("$.status").value("PENDING"))
                        .andExpect(jsonPath("$.transcript").isEmpty());
        } finally {
            transcriptions.deleteById(id+":local"); transcriptions.deleteById(id+":cloud"); recordings.deleteById(id);
        }
    }

    @Test
    void localModePersistsTranscriptWithoutCallingCloud() throws Exception {
        Long id = calls.findAll().getFirst().getId();
        recordings.saveAndFlush(new CallRecording(id, "audio/wav", new byte[]{1,2,3}));
        when(local.configured()).thenReturn(true);
        when(qwen.configured()).thenReturn(true);
        when(local.transcribe(any(), anyString())).thenReturn("[0s–3s] 本地原文");
        try {
            mvc.perform(post("/api/calls/{id}/recording/analysis", id).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isAccepted());
            awaitStatus(id, "COMPLETED");
            assertEquals("[0s–3s] 本地原文", recordings.findById(id).orElseThrow().getTranscript());
            verify(qwen, never()).transcribe(any(), anyString());
            verify(ai, never()).analyze(any(), anyString());
        } finally { recordings.deleteById(id); }
    }

    @Test
    void qwenTranscriptionPersistsWithoutCallingSummaryProvider() throws Exception {
        Long id = calls.findAll().getFirst().getId();
        recordings.saveAndFlush(new CallRecording(id, "audio/wav", new byte[]{1,2,3}));
        when(qwen.configured()).thenReturn(true);
        when(qwen.transcribe(any(), anyString())).thenReturn("千问原文");
        try {
            mvc.perform(post("/api/calls/{id}/recording/analysis", id).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isAccepted());
            awaitStatus(id, "COMPLETED");
            assertEquals("千问原文", recordings.findById(id).orElseThrow().getTranscript());
            assertNull(recordings.findById(id).orElseThrow().getSummary());
            verify(ai, never()).analyze(any(), anyString());
        } finally { recordings.deleteById(id); }
    }

    @Test
    void completesPersistsAndRejectsUnauthenticatedReads() throws Exception {
        Long id = calls.findAll().getFirst().getId();
        recordings.saveAndFlush(new CallRecording(id, "audio/wav", new byte[]{1,2,3}));
        when(ai.configured()).thenReturn(true);
        when(ai.analyze(any(), anyString())).thenReturn(new RecordingAiClient.Result("原文", "客户希望周五回访"));
        try {
            mvc.perform(post("/api/calls/{id}/recording/analysis", id).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isAccepted());
            awaitStatus(id, "COMPLETED");
            mvc.perform(get("/api/calls/{id}/recording/analysis", id).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.summary").value("客户希望周五回访"))
                    .andExpect(jsonPath("$.transcript").value("原文"));
            mvc.perform(get("/api/calls/{id}/recording/analysis", id)).andExpect(status().isUnauthorized());
            var reviewResponse = mvc.perform(get("/api/calls/{id}/recording/review", id).with(user("admin").roles("ADMIN")))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            var json = new com.fasterxml.jackson.databind.ObjectMapper();
            var review = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(reviewResponse);
            review.put("text", "人工核对：周五下午联系");
            mvc.perform(put("/api/calls/{id}/recording/review", id).with(user("admin").roles("ADMIN"))
                    .contentType("application/json").content(review.toString())).andExpect(status().isNoContent());
            mvc.perform(get("/api/calls/{id}/recording/review", id).with(user("admin").roles("ADMIN")))
                    .andExpect(jsonPath("$.text").value("人工核对：周五下午联系"))
                    .andExpect(jsonPath("$.reviewer").value("admin"));
            // An old open editor cannot silently overwrite a newer confirmation.
            mvc.perform(put("/api/calls/{id}/recording/review", id).with(user("admin").roles("ADMIN"))
                    .contentType("application/json").content(review.toString())).andExpect(status().isConflict());
            assertEquals("客户希望周五回访", recordings.findById(id).orElseThrow().getSummary());
        } finally { recordings.deleteById(id); }
    }

    @Test
    void replacementCannotReceiveOldAnalysisAndDuplicateStartIsRejected() throws Exception {
        Long id = calls.findAll().getFirst().getId();
        recordings.saveAndFlush(new CallRecording(id, "audio/wav", new byte[]{1}));
        when(ai.configured()).thenReturn(true);
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var returned = new CountDownLatch(1);
        when(ai.analyze(any(), anyString())).thenAnswer(invocation -> {
            entered.countDown(); release.await(5, TimeUnit.SECONDS); returned.countDown();
            return new RecordingAiClient.Result("旧原文", "旧总结");
        });
        try {
            mvc.perform(post("/api/calls/{id}/recording/analysis", id).with(user("admin").roles("ADMIN"))).andExpect(status().isAccepted());
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            mvc.perform(post("/api/calls/{id}/recording/analysis", id).with(user("admin").roles("ADMIN"))).andExpect(status().isConflict());
            var old = recordings.findById(id).orElseThrow();
            recordings.saveAndFlush(new CallRecording(id, "audio/wav", new byte[]{2}));
            release.countDown(); assertTrue(returned.await(5, TimeUnit.SECONDS));
            // Directly exercise the same database completion guard deterministically.
            assertEquals(0, recordings.finish(id, old.getRevision(), old.getAnalysisStartedAt(), "COMPLETED", "旧原文", "旧总结", null));
            var current = recordings.findById(id).orElseThrow();
            assertEquals("PENDING", current.getAnalysisStatus()); assertNull(current.getSummary());
        } finally { release.countDown(); recordings.deleteById(id); }
    }

    @Test
    void failedAnalysisCanBeRetriedWithoutExposingProviderException() throws Exception {
        Long id = calls.findAll().getFirst().getId();
        recordings.saveAndFlush(new CallRecording(id, "audio/wav", new byte[]{1}));
        when(ai.configured()).thenReturn(true);
        when(ai.analyze(any(), anyString())).thenThrow(new IllegalStateException("private-provider-detail"))
                .thenReturn(new RecordingAiClient.Result("转写", "重试成功"));
        try {
            mvc.perform(post("/api/calls/{id}/recording/analysis", id).with(user("admin").roles("ADMIN"))).andExpect(status().isAccepted());
            awaitStatus(id, "FAILED");
            assertFalse(recordings.findById(id).orElseThrow().getAnalysisError().contains("private-provider-detail"));
            mvc.perform(post("/api/calls/{id}/recording/analysis", id).with(user("admin").roles("ADMIN"))).andExpect(status().isAccepted());
            awaitStatus(id, "COMPLETED");
            assertEquals("重试成功", recordings.findById(id).orElseThrow().getSummary());
        } finally { recordings.deleteById(id); }
    }

    @Test
    void missingConfigurationDoesNotStartOrCallProvider() throws Exception {
        Long id = calls.findAll().getFirst().getId();
        when(ai.configured()).thenReturn(false);
        mvc.perform(post("/api/calls/{id}/recording/analysis", id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isServiceUnavailable());
        verify(ai, never()).analyze(any(), anyString());
    }

    private void awaitStatus(Long id, String status) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (status.equals(recordings.findById(id).orElseThrow().getAnalysisStatus())) return;
            Thread.sleep(20);
        }
        fail("analysis did not reach " + status);
    }
}
