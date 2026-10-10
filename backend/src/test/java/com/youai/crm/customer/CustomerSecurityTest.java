package com.youai.crm.customer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.youai.crm.account.CrmUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerSecurityTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CustomerRepository customers;
    @Autowired CrmUserRepository users;

    @Test
    void onlyAdministratorsCanImportCustomers() throws Exception {
        employee("import_sales", "导入权限员工");
        String body = json.createObjectNode().put("name", "导入权限客户").put("phone", "13800997654")
                .put("owner", "白板").put("source", "测试").toString();
        long count = customers.count();
        mvc.perform(post("/api/customers/import").with(user("import_sales").roles("SALES"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        assertEquals(count, customers.count());
        mvc.perform(post("/api/customers/import").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.owner").value("白板"));
        assertEquals(count + 1, customers.count());
    }

    private long employee(String username, String name) throws Exception {
        String body = json.createObjectNode().put("username", username).put("displayName", name)
                .put("password", "EmployeePassword123!").toString();
        return json.readTree(mvc.perform(post("/api/auth/users").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();
    }

    private JsonNode customer(String owner, String collaborator) throws Exception {
        String body = json.createObjectNode().put("name", "权限回归客户").put("phone", "13800991234")
                .put("owner", owner).put("source", "测试").put("collaborator", collaborator)
                .put("idCard", "120000199405060000").put("wechat", "private_wechat")
                .put("note", "私密备注13800991234").put("remark", "私密补充")
                .put("birthday", "1994-05-06").put("workLocation", "精确工作地址").toString();
        JsonNode created = json.readTree(mvc.perform(post("/api/customers").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        assertEquals("白板", created.get("owner").asText());
        return json.readTree(mvc.perform(put("/api/customers/" + created.get("id").asText())
                .with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    @Test
    void sameNamesCannotReadOrModifyAnotherEmployeesCustomer() throws Exception {
        long first = employee("identity_one", "同名员工");
        employee("identity_two", "同名员工");
        String id = customer("user:" + first, "").get("id").asText();
        mvc.perform(get("/api/customers").param("keyword", id).with(user("identity_one").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/customers").param("keyword", id).with(user("identity_two").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/customers/" + id).with(user("identity_two").roles("SALES")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/customers/" + id + "/stage").with(user("identity_two").roles("SALES"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"stage\":\"已成交\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/customers/" + id + "/assignment-events").with(user("identity_two").roles("SALES")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/customers/" + id + "/assignment").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"owner\":\"同名员工\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void renamePreservesOwnershipAndInheritanceUsesIds() throws Exception {
        long first = employee("rename_one", "原始姓名");
        long second = employee("rename_two", "变更姓名");
        String id = customer("user:" + first, "").get("id").asText();
        mvc.perform(patch("/api/auth/users/" + first).with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"rename_one\",\"displayName\":\"变更姓名\",\"phone\":\"\",\"roles\":[\"SALES\"]}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/customers").param("keyword", id).with(user("rename_one").roles("SALES")))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/customers/" + id).with(user("rename_one").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.idCard").value("120000199405060000"));
        mvc.perform(post("/api/customers/inherit").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"sourceOwner\":\"变更姓名\",\"sourceUserId\":" + first
                        + ",\"targetOwner\":\"user:" + second + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/customers/" + id).with(user("rename_one").roles("SALES")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/customers/" + id).with(user("rename_two").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(second));
    }

    @Test
    void collaboratorsUseExactIdsAndNeverReceivePrivateFields() throws Exception {
        long owner = employee("collab_owner", "负责人甲");
        long collaborator = employee("collab_one", "小张");
        employee("collab_two", "小张");
        String id = customer("user:" + owner, "user:" + collaborator).get("id").asText();
        mvc.perform(get("/api/customers").param("keyword", id).param("scope", "collab")
                .with(user("collab_one").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].contactVisible").value(false))
                .andExpect(jsonPath("$.content[0].idCard").isEmpty())
                .andExpect(jsonPath("$.content[0].note").isEmpty())
                .andExpect(jsonPath("$.content[0].remark").isEmpty());
        mvc.perform(get("/api/customers").param("keyword", id).param("scope", "collab")
                .with(user("collab_two").roles("SALES")))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void publicPoolHidesContactsAndClaimRestoresOnlyOwnerVisibility() throws Exception {
        long owner = employee("pool_identity", "领取员工");
        employee("pool_other", "其他员工");
        String id = customer("user:" + owner, "user:" + owner).get("id").asText();
        mvc.perform(patch("/api/customers/" + id + "/pool").with(user("pool_identity").roles("SALES"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"inPool\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").isEmpty())
                .andExpect(jsonPath("$.collaboratorIds").isEmpty())
                .andExpect(jsonPath("$.phone").isEmpty())
                .andExpect(jsonPath("$.wechat").isEmpty());
        String result = mvc.perform(get("/api/customers/pool").param("size", "100")
                .with(user("pool_identity").roles("SALES"))).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode record = null;
        for (JsonNode row : json.readTree(result).get("content")) if (id.equals(row.get("id").asText())) record = row;
        assertNotNull(record);
        assertFalse(record.get("contactVisible").asBoolean());
        for (String field : new String[]{"idCard", "note", "remark", "birthday", "workLocation"}) assertTrue(record.get(field).isNull(), field);
        assertTrue(record.get("phone").isNull());
        assertTrue(record.get("wechat").isNull());
        for (String username : new String[]{"pool_identity", "pool_other"}) {
            mvc.perform(get("/api/customers").param("inPool", "true").param("keyword", id)
                    .with(user(username).roles("SALES")))
                    .andExpect(jsonPath("$.content[0].phone").isEmpty())
                    .andExpect(jsonPath("$.content[0].wechat").isEmpty());
            mvc.perform(get("/api/customers/" + id).with(user(username).roles("SALES")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/customers").param("inPool", "true").param("keyword", id)
                .with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$.content[0].phone").value("13800991234"))
                .andExpect(jsonPath("$.content[0].wechat").value("private_wechat"));
        mvc.perform(patch("/api/customers/" + id + "/pool").with(user("pool_identity").roles("SALES"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"inPool\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownerId").value(owner))
                .andExpect(jsonPath("$.idCard").value("120000199405060000"))
                .andExpect(jsonPath("$.note").value("私密备注13800991234"))
                .andExpect(jsonPath("$.phone").value("13800991234"))
                .andExpect(jsonPath("$.wechat").value("private_wechat"));
        mvc.perform(get("/api/customers/" + id).with(user("pool_other").roles("SALES")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/customers/" + id + "/pool").with(user("pool_identity").roles("SALES"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"inPool\":true}"))
                .andExpect(jsonPath("$.contactVisible").value(false))
                .andExpect(jsonPath("$.phone").isEmpty())
                .andExpect(jsonPath("$.wechat").isEmpty());
        mvc.perform(get("/api/customers/" + id).with(user("pool_identity").roles("SALES")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/customers/" + id + "/pool").with(user("pool_other").roles("SALES"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"inPool\":false}"))
                .andExpect(jsonPath("$.phone").value("13800991234"))
                .andExpect(jsonPath("$.wechat").value("private_wechat"));
        mvc.perform(get("/api/customers/" + id).with(user("pool_identity").roles("SALES")))
                .andExpect(status().isForbidden());
    }

    @Test
    void relatedCallsMessagesAndTasksFollowCustomerIdentityAfterReassignment() throws Exception {
        long first = employee("related_one", "关联同名");
        long second = employee("related_two", "关联同名");
        String id = customer("user:" + first, "").get("id").asText();
        mvc.perform(post("/api/calls").with(user("related_one").roles("SALES"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"customerId\":\"" + id
                        + "\",\"direction\":\"呼出\",\"status\":\"已接通\",\"note\":\"保密通话\"}"))
                .andExpect(status().isCreated());
        String conversation = mvc.perform(post("/api/conversations").with(user("related_one").roles("SALES"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"customerId\":\"" + id + "\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long conversationId = json.readTree(conversation).get("id").asLong();
        String task = mvc.perform(post("/api/tasks").with(user("related_one").roles("SALES"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"customerId\":\"" + id
                        + "\",\"customer\":\"权限回归客户\",\"title\":\"私密跟进\",\"owner\":\"关联同名\",\"dueAt\":\"2026-10-06T12:00:00\",\"type\":\"电话跟进\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long taskId = json.readTree(task).get("id").asLong();
        mvc.perform(get("/api/calls").param("keyword", id).with(user("related_two").roles("SALES")))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/conversations/" + conversationId + "/messages").with(user("related_two").roles("SALES")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/tasks/" + taskId).with(user("related_two").roles("SALES")))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/customers/" + id + "/assignment").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"owner\":\"user:" + second + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/customers/" + id + "/assignment-events").with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/calls").param("keyword", id).with(user("related_one").roles("SALES")))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/calls").param("keyword", id).with(user("related_two").roles("SALES")))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/conversations/" + conversationId + "/messages").with(user("related_two").roles("SALES")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tasks/" + taskId).with(user("related_two").roles("SALES")))
                .andExpect(status().isOk());
    }

    @Test
    void unresolvedLegacyOwnershipDoesNotResolveDynamically() throws Exception {
        long owner = employee("legacy_owner", "历史姓名");
        String id = customer("user:" + owner, "").get("id").asText();
        Customer record = customers.findByCustomerNo(id).orElseThrow();
        record.setOwnerId(null);
        customers.saveAndFlush(record);
        mvc.perform(get("/api/customers").param("keyword", id).with(user("legacy_owner").roles("SALES")))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/customers/" + id).with(user("legacy_owner").roles("SALES")))
                .andExpect(status().isForbidden());
    }
}
