package com.youai.crm.communication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerCallIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CallRecordRepository calls;
    @Autowired com.youai.crm.customer.CustomerRepository customers;
    @Autowired com.youai.crm.account.CrmUserRepository users;
    @Autowired ObjectMapper json;
    @Autowired CallService callService;
    @Autowired jakarta.persistence.EntityManagerFactory emf;

    @Test void ownershipMappingUsesOneScalarQueryForOneHundredRecords() {
        var customer = customers.findAll().getFirst();
        var sales = users.findByUsernameIgnoreCase("linxi").orElseThrow();
        customer.setOwnerId(null);
        customers.saveAndFlush(customer);
        var principal = new com.youai.crm.account.CrmPrincipal(sales);
        var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        var records = java.util.stream.IntStream.range(0, 100).mapToObj(i -> {
            var record = new CallRecord();
            record.setCustomerNo(i % 2 == 0 ? customer.getCustomerNo() : "MISSING-CUSTOMER");
            record.setAgentUserId(sales.getId());
            record.setPhone("private phone"); record.setNote("private note");
            return record;
        }).toList();
        var statistics = emf.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean previous = statistics.isStatisticsEnabled();
        try {
            statistics.setStatisticsEnabled(true);
            statistics.clear();
            var result = callService.responses(new org.springframework.data.domain.PageImpl<>(records), authentication);
            assertEquals(1, statistics.getPrepareStatementCount(), "ownership lookup must not grow per record");
            assertEquals(100, result.getNumberOfElements());
            result.forEach(record -> {
                assertEquals("TRANSFERRED", record.customerOwnershipStatus());
                assertFalse(record.canAccessRecording());
                assertEquals("", record.phone()); assertEquals("", record.note());
            });
        } finally { statistics.setStatisticsEnabled(previous); }
    }

    @Test void uploadIsSharedAndPreservesAgentAfterTransfer() throws Exception {
        var customer = customers.findAll().getFirst();
        var sales = users.findByUsernameIgnoreCase("linxi").orElseThrow();
        var admin = users.findByUsernameIgnoreCase("admin").orElseThrow();
        customer.setOwnerId(sales.getId()); customers.saveAndFlush(customer);
        var result = mvc.perform(multipart("/api/call-uploads")
                .file(new MockMultipartFile("file", "test.wav", "audio/wav", RecordingFixtures.wav()))
                .param("customerId", customer.getCustomerNo())
                .with(user("linxi").roles("SALES")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.recordingSource").value("UPLOAD"))
                .andExpect(jsonPath("$.agentUserId").value(sales.getId()))
                .andExpect(jsonPath("$.uploadedAt").isNotEmpty())
                .andExpect(jsonPath("$.durationSeconds").value(1)).andReturn();
        long id = json.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        mvc.perform(get("/api/customers/{id}/calls", customer.getCustomerNo()).with(user("linxi").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[?(@.id == " + id + ")].agent").value(sales.getDisplayName()));
        customer.setOwnerId(admin.getId()); customers.saveAndFlush(customer);
        mvc.perform(get("/api/customers/{id}/calls", customer.getCustomerNo()).with(user("linxi").roles("SALES")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/calls/{id}/recording", id).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().bytes(RecordingFixtures.wav()));
        mvc.perform(get("/api/calls").param("keyword", customer.getCustomerNo()).with(user("linxi").roles("SALES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].customerOwnershipStatus").value("TRANSFERRED"))
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].canAccessRecording").value(false))
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].phone").value(""))
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].note").value(""));
        mvc.perform(get("/api/calls/{id}/recording", id).with(user("linxi").roles("SALES")))
                .andExpect(status().isForbidden());
        var nextSales = users.findByUsernameIgnoreCase("chenchen").orElseThrow();
        customer.setOwnerId(nextSales.getId()); customers.saveAndFlush(customer);
        mvc.perform(get("/api/customers/{id}/calls", customer.getCustomerNo()).with(user("chenchen").roles("SALES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].canAccessRecording").value(true));
        mvc.perform(get("/api/calls/{id}/recording", id).with(user("chenchen").roles("SALES")))
                .andExpect(status().isOk()).andExpect(content().bytes(RecordingFixtures.wav()));
        customer.setOwnerId(admin.getId()); customers.saveAndFlush(customer);
        mvc.perform(get("/api/calls").param("keyword", customer.getCustomerNo()).with(user("chenchen").roles("SALES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")]").isEmpty());
        customer.setOwnerId(null); customers.saveAndFlush(customer);
        mvc.perform(get("/api/calls").param("keyword", customer.getCustomerNo()).with(user("linxi").roles("SALES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].customerOwnershipStatus").value("TRANSFERRED"));
        customer.setOwnerId(sales.getId()); customers.saveAndFlush(customer);
        mvc.perform(get("/api/calls").param("keyword", customer.getCustomerNo()).with(user("linxi").roles("SALES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].customerOwnershipStatus").value("FOLLOWING"))
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].canAccessRecording").value(true));
        mvc.perform(get("/api/calls/{id}/recording", id).with(user("linxi").roles("SALES")))
                .andExpect(status().isOk());
        assertEquals(sales.getId(), calls.findById(id).orElseThrow().getAgentUserId());
        mvc.perform(get("/api/calls").param("keyword", customer.getCustomerNo()).param("agent", sales.getDisplayName()).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[?(@.id == " + id + ")].recordingSource").value("UPLOAD"));
    }
    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void corruptUploadDoesNotLeaveEmptyCall() throws Exception {
        long before = calls.count();
        mvc.perform(multipart("/api/call-uploads")
                .file(new MockMultipartFile("file", "bad.wav", "audio/wav", new byte[]{1,2,3}))
                .param("customerId", customers.findAll().getFirst().getCustomerNo())
                .param("startedAt", "2026-01-01T10:00:00").with(user("admin").roles("ADMIN")))
                .andExpect(status().isUnsupportedMediaType());
        assertEquals(before, calls.count());
    }
    @Test void suppliedAgentTimeAndDurationCannotOverrideSystemValues() throws Exception {
        var customer = customers.findAll().getFirst();
        customer.setOwnerId(users.findByUsernameIgnoreCase("linxi").orElseThrow().getId()); customers.saveAndFlush(customer);
        mvc.perform(multipart("/api/call-uploads")
                .file(new MockMultipartFile("file", "test.wav", "audio/wav", RecordingFixtures.wav()))
                .param("customerId", customer.getCustomerNo()).param("startedAt", "2026-01-01T10:00:00")
                .param("agentUserId", users.findByUsernameIgnoreCase("admin").orElseThrow().getId().toString())
                .param("durationSeconds", "9999")
                .with(user("linxi").roles("SALES")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.agentUserId").value(customer.getOwnerId()))
                .andExpect(jsonPath("$.durationSeconds").value(1))
                .andExpect(jsonPath("$.startedAt").value(org.hamcrest.Matchers.startsWith(java.time.LocalDate.now().toString())));
    }
}
