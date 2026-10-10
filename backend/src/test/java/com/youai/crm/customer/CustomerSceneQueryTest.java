package com.youai.crm.customer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.communication.CallRecord;
import com.youai.crm.communication.CallRecordRepository;
import com.youai.crm.task.FollowUpTask;
import com.youai.crm.task.FollowUpTaskRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerSceneQueryTest {
    private static final AtomicLong NUMBERS = new AtomicLong(70_000_000);
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CustomerRepository customers;
    @Autowired CrmUserRepository users;
    @MockitoSpyBean FollowUpTaskRepository tasks;
    @MockitoSpyBean CallRecordRepository calls;

    @Test
    void freshlyAssignedCustomerIsUnfollowedEvenWithPoolDeadlineTimestamp() throws Exception {
        String keyword = "scene-assigned-" + NUMBERS.incrementAndGet();
        String body = json.createObjectNode().put("name", keyword).put("phone", phone())
                .put("source", "测试").put("owner", "白板").toString();
        String customerNo = json.readTree(mvc.perform(post("/api/customers")
                        .with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.lastContactAt").isNotEmpty())
                .andExpect(jsonPath("$.followUpCount").value(0)).andReturn().getResponse().getContentAsString())
                .get("id").asText();
        String owner = "user:" + users.findByUsernameIgnoreCase("linxi").orElseThrow().getId();
        mvc.perform(patch("/api/customers/" + customerNo + "/assignment")
                        .with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(json.createObjectNode().put("owner", owner).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lastContactAt").isNotEmpty())
                .andExpect(jsonPath("$.followUpCount").value(0));
        customer("chenchen", keyword, 1);
        mvc.perform(get("/api/customers").with(user("linxi").roles("SALES"))
                        .param("keyword", keyword).param("scene", "new-unfollowed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(customerNo))
                .andExpect(jsonPath("$.content[0].followUpCount").value(0));
    }

    @Test
    void realTasksAndCallsExcludeCustomersFromBothUnfollowedScenes() throws Exception {
        String keyword = "scene-real-" + NUMBERS.incrementAndGet();
        Customer uncontacted = customer("linxi", keyword, 2);
        Customer tasked = customer("linxi", keyword, 2);
        Customer called = customer("linxi", keyword, 2);
        task(tasked, tasked.getCustomerNo());
        call(called);
        for (String scene : List.of("new-unfollowed", "duplicate-unfollowed")) {
            mvc.perform(get("/api/customers").with(user("linxi").roles("SALES"))
                            .param("keyword", keyword).param("scene", scene))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(uncontacted.getCustomerNo()));
        }
        for (Customer followed : List.of(tasked, called)) {
            mvc.perform(get("/api/customers/" + followed.getCustomerNo()).with(user("linxi").roles("SALES")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.followUpCount").value(1));
        }
        uncontacted.setRegistrationCount(1);
        customers.saveAndFlush(uncontacted);
        mvc.perform(get("/api/customers").with(user("linxi").roles("SALES"))
                        .param("keyword", keyword).param("scene", "duplicate-unfollowed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void usesStableReferencesAndAllHistoryWithoutGuessingLegacyCustomerNames() throws Exception {
        String keyword = "scene-same-name-" + NUMBERS.incrementAndGet();
        Customer followed = customer("linxi", keyword, 1);
        Customer sameName = customer("linxi", keyword, 1);
        task(followed, followed.getCustomerNo());
        task(sameName, null);
        CallRecord historical = call(followed);
        historical.setStartedAt(LocalDateTime.now().minusDays(90));
        calls.saveAndFlush(historical);
        followed.setLastAllocationAt(LocalDateTime.now());
        customers.saveAndFlush(followed);
        mvc.perform(get("/api/customers").with(user("linxi").roles("SALES"))
                        .param("keyword", keyword).param("scene", "new-unfollowed"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(sameName.getCustomerNo()))
                .andExpect(jsonPath("$.content[0].followUpCount").value(0));
        mvc.perform(get("/api/customers/" + followed.getCustomerNo()).with(user("linxi").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.followUpCount").value(2));
    }

    @Test
    void paginatesSceneInDatabaseAndAggregatesFollowUpCountsOncePerPage() throws Exception {
        String keyword = "scene-page-" + NUMBERS.incrementAndGet();
        for (int index = 0; index < 4; index++) customer("linxi", keyword, 1);
        Customer followed = customer("linxi", keyword, 1);
        task(followed, followed.getCustomerNo());
        task(followed, followed.getCustomerNo());
        call(followed);
        clearInvocations(tasks, calls);
        JsonNode page = json.readTree(mvc.perform(get("/api/customers").with(user("linxi").roles("SALES"))
                        .param("keyword", keyword).param("size", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content[0].followUpCount").value(3))
                .andReturn().getResponse().getContentAsString());
        assertEquals(3, page.get("content").size());
        verify(tasks, times(1)).countByCustomerNos(anyCollection());
        verify(calls, times(1)).countByCustomerNos(anyCollection());
        mvc.perform(get("/api/customers").with(user("linxi").roles("SALES"))
                        .param("keyword", keyword).param("scene", "new-unfollowed")
                        .param("page", "1").param("size", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void poolFollowUpRangeUsesTasksAndCallsBeforePagination() throws Exception {
        String keyword = "pool-range-" + NUMBERS.incrementAndGet();
        Customer zero = customer("linxi", keyword, 1);
        Customer one = customer("linxi", keyword, 1);
        Customer two = customer("linxi", keyword, 1);
        task(one, one.getCustomerNo());
        task(two, two.getCustomerNo());
        call(two);
        for (Customer item : List.of(zero, one, two)) {
            item.setOwner("公海");
            item.setOwnerId(null);
            customers.saveAndFlush(item);
        }
        mvc.perform(get("/api/customers/pool").with(user("admin").roles("ADMIN"))
                        .param("keyword", keyword).param("followUpMin", "1").param("followUpMax", "2")
                        .param("size", "1").param("page", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].id").value(one.getCustomerNo()))
                .andExpect(jsonPath("$.content[0].followUpCount").value(1));
        mvc.perform(get("/api/customers/pool").with(user("admin").roles("ADMIN"))
                        .param("keyword", keyword).param("followUpMax", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(zero.getCustomerNo()));
        mvc.perform(get("/api/customers/pool").with(user("admin").roles("ADMIN"))
                        .param("keyword", keyword).param("followUpMin", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(two.getCustomerNo()));
        mvc.perform(get("/api/customers/pool").with(user("admin").roles("ADMIN"))
                        .param("keyword", keyword).param("followUpMin", "2").param("followUpMax", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/customers/pool").with(user("admin").roles("ADMIN"))
                        .param("keyword", keyword))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
        for (String invalid : List.of("-1", "1.5", "abc", "99999999999999999999999")) {
            mvc.perform(get("/api/customers/pool").with(user("admin").roles("ADMIN"))
                            .param("followUpMin", invalid))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/customers/pool").with(user("admin").roles("ADMIN"))
                        .param("followUpMin", "2").param("followUpMax", "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tagAnyCombinesUntaggedAndMultipleTagsWithoutDuplicatePages() throws Exception {
        for (boolean pool : List.of(false, true)) {
            String keyword = "tag-any-" + NUMBERS.incrementAndGet();
            Customer empty = customer("linxi", keyword, 1);
            Customer important = customer("linxi", keyword, 1);
            Customer ordinary = customer("linxi", keyword, 1);
            Customer both = customer("linxi", keyword, 1);
            Customer other = customer("linxi", keyword, 1);
            important.setTags(List.of("重点客户"));
            ordinary.setTags(List.of("普通客户"));
            both.setTags(List.of("重点客户", "普通客户"));
            other.setTags(List.of("其他"));
            for (Customer item : List.of(empty, important, ordinary, both, other)) {
                if (pool) { item.setOwner("公海"); item.setOwnerId(null); }
                customers.saveAndFlush(item);
            }
            String endpoint = pool ? "/api/customers/pool" : "/api/customers";
            mvc.perform(get(endpoint).with(user("admin").roles("ADMIN"))
                            .param("keyword", keyword).param("tagMatch", "any")
                            .param("tag", "重点客户,普通客户").param("size", "2").param("page", "1"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.content.length()").value(1));
            mvc.perform(get(endpoint).with(user("admin").roles("ADMIN"))
                            .param("keyword", keyword).param("tagMatch", "any")
                            .param("tag", "重点客户").param("noTag", "true"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
            mvc.perform(get(endpoint).with(user("admin").roles("ADMIN"))
                            .param("keyword", keyword).param("tagMatch", "any").param("noTag", "true"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].id").value(empty.getCustomerNo()));
            mvc.perform(get(endpoint).with(user("admin").roles("ADMIN"))
                            .param("keyword", keyword).param("tagMatch", "any")
                            .param("tag", "重点客户,普通客户").param("noTag", "true"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(4));
            mvc.perform(get(endpoint).with(user("admin").roles("ADMIN"))
                            .param("keyword", keyword).param("tagMatch", "any"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(5));
            mvc.perform(get(endpoint).with(user("admin").roles("ADMIN"))
                            .param("keyword", keyword).param("tag", "重点客户"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
            mvc.perform(get(endpoint).with(user("admin").roles("ADMIN"))
                            .param("keyword", keyword).param("tag", "重点客户").param("noTag", "true"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    private Customer customer(String username, String name, int registrationCount) {
        var owner = users.findByUsernameIgnoreCase(username).orElseThrow();
        Customer customer = new Customer();
        customer.setCustomerNo("scene-" + NUMBERS.incrementAndGet());
        customer.setName(name);
        customer.setPhone(phone());
        customer.setCompany("测试");
        customer.setSource("测试");
        customer.setOwner(owner.getDisplayName());
        customer.setOwnerId(owner.getId());
        customer.setStage("初步沟通");
        customer.setLevel("普通客户");
        customer.setRegistrationCount(registrationCount);
        customer.setLastContactAt(LocalDateTime.now());
        return customers.saveAndFlush(customer);
    }

    private FollowUpTask task(Customer customer, String customerNo) {
        FollowUpTask task = new FollowUpTask();
        task.setTitle("真实跟进记录");
        task.setCustomerName(customer.getName());
        task.setCustomerId(customerNo);
        task.setOwner(customer.getOwner());
        task.setDueAt(LocalDateTime.now().plusDays(1));
        task.setFollowedAt(LocalDateTime.now());
        task.setTaskType("电话跟进");
        task.setStatus("upcoming");
        task.setPriority("普通");
        return tasks.saveAndFlush(task);
    }

    private CallRecord call(Customer customer) {
        CallRecord call = new CallRecord();
        call.setCustomerNo(customer.getCustomerNo());
        call.setCustomerName(customer.getName());
        call.setPhone(customer.getPhone());
        call.setOwner(customer.getOwner());
        call.setAgent(customer.getOwner());
        call.setDirection("呼出");
        call.setStatus("未接通");
        return calls.saveAndFlush(call);
    }

    private String phone() {
        return "139" + NUMBERS.incrementAndGet();
    }
}
