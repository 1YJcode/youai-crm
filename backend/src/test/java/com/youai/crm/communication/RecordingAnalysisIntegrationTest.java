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

@SpringBootTest
@AutoConfigureMockMvc
class RecordingAnalysisIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CallRecordRepository calls;
    @Autowired CallRecordingRepository recordings;
    @MockitoBean RecordingAiClient ai;

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
