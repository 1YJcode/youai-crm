package com.youai.crm.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class KnowledgeControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Test void createsSearchesUpdatesAndDeletesWithVersionProtection() throws Exception {
        var created=json.readTree(mvc.perform(post("/api/knowledge").with(user("admin").roles("ADMIN"))
                .contentType("application/json").content("{\"title\":\"知识测试\",\"category\":\"服务流程\",\"content\":\"回访关键词 100% 完成\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long id=created.get("id").asLong();
        mvc.perform(get("/api/knowledge").param("keyword","回访关键词").param("category","服务流程").with(user("linxi").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/knowledge").param("keyword","100%").with(user("linxi").roles("SALES")))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/knowledge").param("keyword","999%").with(user("linxi").roles("SALES")))
                .andExpect(jsonPath("$.totalElements").value(0));
        var body=json.createObjectNode().put("title","更新知识").put("category","常见问题").put("content","新正文").put("version",created.get("version").asLong());
        mvc.perform(put("/api/knowledge/{id}",id).with(user("admin").roles("ADMIN")).contentType("application/json").content(body.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(put("/api/knowledge/{id}",id).with(user("admin").roles("ADMIN")).contentType("application/json").content(body.toString())).andExpect(status().isConflict());
        mvc.perform(delete("/api/knowledge/{id}",id).param("version","0").with(user("admin").roles("ADMIN"))).andExpect(status().isConflict());
        mvc.perform(delete("/api/knowledge/{id}",id).param("version","1").with(user("admin").roles("ADMIN"))).andExpect(status().isNoContent());
        mvc.perform(get("/api/knowledge/{id}",id).with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
    }
    @Test void requiresLoginAndAdministratorForWrites() throws Exception {
        mvc.perform(get("/api/knowledge")).andExpect(status().isUnauthorized());
        String body="{\"title\":\"测试\",\"category\":\"测试\",\"content\":\"测试\",\"version\":0}";
        mvc.perform(post("/api/knowledge").with(user("linxi").roles("SALES")).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(put("/api/knowledge/1").with(user("linxi").roles("SALES")).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/knowledge/1").param("version","0").with(user("linxi").roles("SALES"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/knowledge").with(user("admin").roles("ADMIN")).contentType("application/json").content("{\"title\":\" \",\"category\":\"x\",\"content\":\"x\"}")).andExpect(status().isBadRequest());
    }
}
