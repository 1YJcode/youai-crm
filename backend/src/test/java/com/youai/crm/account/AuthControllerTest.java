package com.youai.crm.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void logsInDemoAdministrator() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("admin"))
                .andExpect(jsonPath("$.user.roles[0]").value("ADMIN"));
    }

    @Test
    void registersSalesAccountAndReturnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"registertest\",\"password\":\"Password123\",\"displayName\":\"注册测试\",\"phone\":\"13800138000\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value("registertest"))
                .andExpect(jsonPath("$.user.departmentCode").value("SALES"))
                .andExpect(jsonPath("$.user.roles[0]").value("SALES"));
    }

    @Test
    void rejectsProtectedRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidatesTokenOnLogout() throws Exception {
        String registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"logouttest\",\"password\":\"Password123\",\"displayName\":\"注销测试\",\"phone\":\"13800138002\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(registration, "$.accessToken");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void temporarilyLocksAccountAfterRepeatedPasswordFailures() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(request -> { request.setRemoteAddr("192.0.2.44"); return request; })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"locked-test-user\",\"password\":\"wrong\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .with(request -> { request.setRemoteAddr("192.0.2.45"); return request; })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"locked-test-user\",\"password\":\"wrong\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_RATE_LIMITED"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotNull(
                        result.getResponse().getHeader("Retry-After")));
    }

    @Test
    void invalidatesExistingEmployeeTokenAfterAdministratorResetsPassword() throws Exception {
        String registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"credentialreset\",\"password\":\"Original123\",\"displayName\":\"凭证测试\",\"phone\":\"13800138001\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String employeeToken = JsonPath.read(registration, "$.accessToken");
        Number employeeId = JsonPath.read(registration, "$.user.id");

        String adminLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String adminToken = JsonPath.read(adminLogin, "$.accessToken");

        mockMvc.perform(patch("/api/auth/users/{id}/password", employeeId.longValue())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Changed123\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"credentialreset\",\"password\":\"Original123\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"credentialreset\",\"password\":\"Changed123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void administratorCanFreezeEmployeeAndInvalidateExistingToken() throws Exception {
        String registration = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"freezetest\",\"password\":\"Password123\",\"displayName\":\"冻结测试\",\"phone\":\"13800138003\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String employeeToken = JsonPath.read(registration, "$.accessToken");
        Number employeeId = JsonPath.read(registration, "$.user.id");

        String adminLogin = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String adminToken = JsonPath.read(adminLogin, "$.accessToken");

        mockMvc.perform(patch("/api/auth/users/{id}/freeze", employeeId.longValue())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"freezetest\",\"password\":\"Password123\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/auth/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'freezetest')].enabled").value(false));

        mockMvc.perform(patch("/api/auth/users/{id}/unfreeze", employeeId.longValue())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"freezetest\",\"password\":\"Password123\"}"))
                .andExpect(status().isOk());
    }
}
