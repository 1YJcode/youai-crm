package com.youai.crm.communication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CallRecordingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CallRecordRepository calls;
    @Autowired jakarta.persistence.EntityManager entities;
    @Autowired com.youai.crm.customer.CustomerRepository customers;
    @Autowired com.youai.crm.account.CrmUserRepository users;

    @Test
    void transferRevokesRecordingAnalysisAndReviewAccessForPreviousOwner() throws Exception {
        var call = calls.findAll().getFirst();
        var customer = customers.findByCustomerNo(call.getCustomerNo()).orElseThrow();
        customer.setOwnerId(users.findByUsernameIgnoreCase("linxi").orElseThrow().getId());
        customers.saveAndFlush(customer);
        mvc.perform(multipart("/api/calls/{id}/recording", call.getId())
                .file(new MockMultipartFile("file", "test.wav", "audio/wav", RecordingFixtures.wav()))
                .with(user("linxi").roles("SALES"))).andExpect(status().isOk());
        mvc.perform(get("/api/calls/{id}/recording", call.getId()).with(user("linxi").roles("SALES")))
                .andExpect(status().isOk());
        customer.setOwnerId(users.findByUsernameIgnoreCase("admin").orElseThrow().getId());
        customers.saveAndFlush(customer);
        for (String suffix : new String[]{"", "/analysis", "/review"}) {
            mvc.perform(get("/api/calls/" + call.getId() + "/recording" + suffix).with(user("linxi").roles("SALES")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/calls/{id}/recording/analysis", call.getId()).with(user("linxi").roles("SALES")))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/calls/{id}/recording", call.getId())
                .file(new MockMultipartFile("file", "test.wav", "audio/wav", RecordingFixtures.wav()))
                .with(user("linxi").roles("SALES"))).andExpect(status().isForbidden());
    }

    @Test
    void multipartPersistsAndAuthenticatedDownloadReturnsOriginalBytes() throws Exception {
        Long id = calls.findAll().getFirst().getId();
        byte[] audio = RecordingFixtures.wav();
        mvc.perform(multipart("/api/calls/{id}/recording", id)
                .file(new MockMultipartFile("file", "test.wav", "audio/wav", audio))
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(audio.length));
        entities.flush(); entities.clear();
        mvc.perform(get("/api/calls/{id}/recording", id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().bytes(audio))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/calls/{id}/recording", id)).andExpect(status().isUnauthorized());
    }

    @Test
    void unsupportedFileReturnsUsefulError() throws Exception {
        Long id = calls.findAll().getFirst().getId();
        mvc.perform(multipart("/api/calls/{id}/recording", id)
                .file(new MockMultipartFile("file", "test.mp3", "audio/mpeg", "not audio".getBytes()))
                .with(user("admin").roles("ADMIN")))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("REQUEST_REJECTED"));
    }
}
