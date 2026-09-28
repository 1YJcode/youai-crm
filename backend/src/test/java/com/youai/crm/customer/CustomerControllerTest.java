package com.youai.crm.customer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import com.youai.crm.communication.CallRecord;
import com.youai.crm.communication.CallRecordRepository;
import com.youai.crm.account.CrmUser;
import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.account.DepartmentRepository;
import com.youai.crm.account.RoleRepository;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CallRecordRepository callRecordRepository;

    @Autowired private CrmUserRepository userRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private RoleRepository roleRepository;

    @BeforeEach
    void ensureSalesUsers() {
        createSalesUser("linxi", "林夕", "13810000001");
        createSalesUser("chenchen", "陈晨", "13810000002");
    }

    private void createSalesUser(String username, String displayName, String phone) {
        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) return;
        CrmUser user = new CrmUser();
        user.setUsername(username);
        user.setPasswordHash("test-only-password-hash");
        user.setDisplayName(displayName);
        user.setPhone(phone);
        user.setEnabled(true);
        user.setDepartment(departmentRepository.findByCode("SALES").orElseThrow());
        user.setRoles(Set.of(roleRepository.findByCode("SALES").orElseThrow()));
        userRepository.save(user);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void listsSeededCustomers() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").isNotEmpty())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createsAndFindsCustomer() throws Exception {
        String body = """
                {
                  "name": "接口测试客户",
                  "phone": "13800009999",
                  "company": "优客测试公司",
                  "source": "线上咨询",
                  "owner": "林夕",
                  "level": "重点客户",
                  "amount": 12000,
                  "tags": ["自动化测试"]
                }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.matchesPattern("\\d{10}")))
                .andExpect(jsonPath("$.name").value("接口测试客户"))
                .andExpect(jsonPath("$.stage").value("初步沟通"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void recordsManualPoolEntryTypeAndFiltersPoolByIt() throws Exception {
        String suffix = String.format("%08d", Math.abs(System.nanoTime() % 100_000_000L));
        String created = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"主动放弃测试\",\"phone\":\"138" + suffix + "\",\"source\":\"测试\",\"owner\":\"林夕\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String customerNo = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("id").asText();

        mockMvc.perform(patch("/api/customers/{customerNo}/pool", customerNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inPool\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.poolEntryType").value("主动放弃"));

        mockMvc.perform(get("/api/customers/pool").param("poolEntryType", "主动放弃"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].poolEntryType").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("主动放弃"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void updatesAndReturnsCompleteCustomerProfile() throws Exception {
        String created = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"完整资料测试\",\"phone\":\"13800004444\",\"source\":\"线上咨询\",\"owner\":\"林夕\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String customerNo = new com.fasterxml.jackson.databind.ObjectMapper().readTree(created).get("id").asText();
        String body = """
                {
                  "name":"完整资料测试","phone":"13800004444","company":"个人客户","source":"线上咨询","owner":"林夕",
                  "stage":"需求确认","level":"重点客户","amount":88000,"city":"天津","note":"客户备注","gender":"女",
                  "birthday":"1994-05-06","age":"32","height":"168cm","maritalStatus":"未婚","education":"本科",
                  "monthlyIncome":"20000","annualIncome":"300000","occupation":"设计师","housing":"已购房","car":"已购车",
                  "nativePlace":"天津","workLocation":"天津","wechat":"profile_wechat","idCard":"120000199405060000",
                  "remark":"补充说明","certificationStatus":"已认证","familyStatus":"家庭和睦","childrenStatus":"无子女",
                  "vehicleHousing":"有房有车","matchAgeRange":"28-36","matchMaritalStatus":"未婚","matchHeightRange":"175-185cm",
                  "matchEducation":"本科及以上","matchMonthlyIncome":"20000以上","matchMostImportant":"责任心",
                  "matchPersonality":"开朗","matchChildren":"希望有孩子","matchDealbreakers":"吸烟","collaborator":"赵娣、林夕",
                  "tags":["完整资料"]
                }
                """;

        mockMvc.perform(put("/api/customers/{customerNo}", customerNo).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.birthday").value("1994-05-06"))
                .andExpect(jsonPath("$.matchMostImportant").value("责任心"))
                .andExpect(jsonPath("$.collaborator").value("赵娣、林夕"));
    }

    @Test
    @WithAnonymousUser
    void validatesPhoneNumber() throws Exception {
        String body = """
                {
                  "name": "错误号码",
                  "phone": "123",
                  "source": "线上咨询",
                  "owner": "林夕"
                }
                """;

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "linxi", roles = "SALES")
    void salesOnlySeesOwnedCustomers() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].owner").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("林夕"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanFilterCustomersByOwner() throws Exception {
        mockMvc.perform(get("/api/customers").param("owner", "林夕"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isNotEmpty())
                .andExpect(jsonPath("$.content[*].owner").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.equalTo("林夕"))));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void filtersCustomersByMultipleCustomerStatuses() throws Exception {
        String suffix = String.format("%08d", Math.abs(System.nanoTime() % 100_000_000L));
        String marker = "状态筛选" + suffix;
        String firstBody = """
                {"name":"%s甲","phone":"138%s","company":"测试","source":"测试","owner":"林夕","stage":"筛选阶段甲"}
                """.formatted(marker, suffix);
        String secondBody = """
                {"name":"%s乙","phone":"139%s","company":"测试","source":"测试","owner":"林夕","stage":"筛选阶段乙"}
                """.formatted(marker, suffix);
        String excludedBody = """
                {"name":"%s丙","phone":"137%s","company":"测试","source":"测试","owner":"林夕","stage":"筛选阶段丙"}
                """.formatted(marker, suffix);

        for (String body : new String[] { firstBody, secondBody, excludedBody }) {
            mockMvc.perform(post("/api/customers").with(user("linxi").roles("SALES"))
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/customers").param("customerStatus", "筛选阶段甲,筛选阶段乙"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].stage")
                        .value(containsInAnyOrder("筛选阶段甲", "筛选阶段乙")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void filtersCustomerStatusContainingChineseComma() throws Exception {
        String suffix = String.format("%08d", Math.abs(System.nanoTime() % 100_000_000L));
        String status = "1类：未接通，待跟进";
        String body = "{\"name\":\"中文逗号状态" + suffix
                + "\",\"phone\":\"136" + suffix
                + "\",\"company\":\"测试\",\"source\":\"测试\",\"owner\":\"林夕\",\"stage\":\""
                + status + "\"}";

        mockMvc.perform(post("/api/customers").with(user("linxi").roles("SALES"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/customers").param("customerStatus", status))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].stage").value(org.hamcrest.Matchers.hasItem(status)));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void keepsWhiteboardCustomersOutOfCustomerListAndInWhiteboardList() throws Exception {
        String marker = "白板隔离测试" + System.nanoTime();
        String phoneSuffix = String.format("%08d", Math.abs(System.nanoTime() % 100_000_000L));
        String assignedBody = "{\"name\":\"" + marker + "-销售\",\"phone\":\"138" + phoneSuffix
                + "\",\"source\":\"测试\",\"owner\":\"林夕\"}";
        String whiteboardBody = "{\"name\":\"" + marker + "-白板\",\"phone\":\"139" + phoneSuffix
                + "\",\"source\":\"测试\",\"owner\":\"白板\"}";

        mockMvc.perform(post("/api/customers").with(user("linxi").roles("SALES"))
                        .contentType(MediaType.APPLICATION_JSON).content(assignedBody))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(whiteboardBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/customers").param("keyword", marker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].owner").value("林夕"));
        mockMvc.perform(get("/api/customers").param("keyword", marker).param("whiteboardOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].owner").value("白板"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void administratorCreatesToWhiteboardWhileSalesCreatesToOwnLibrary() throws Exception {
        String suffix = String.format("%08d", Math.abs(System.nanoTime() % 100_000_000L));
        String adminBody = "{\"name\":\"管理员新建白板\",\"phone\":\"138" + suffix
                + "\",\"source\":\"测试\",\"owner\":\"林夕\"}";
        String salesBody = "{\"name\":\"销售新建归属\",\"phone\":\"139" + suffix
                + "\",\"source\":\"测试\",\"owner\":\"陈晨\"}";

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(adminBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner").value("白板"));
        mockMvc.perform(post("/api/customers").with(user("linxi").roles("SALES"))
                        .contentType(MediaType.APPLICATION_JSON).content(salesBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.owner").value("林夕"));
    }

    @Test
    @WithMockUser(username = "linxi", roles = "SALES")
    void salesCannotQueryUnassignedWhiteboardCustomers() throws Exception {
        mockMvc.perform(get("/api/customers").param("whiteboardOnly", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void duplicateRegistrationIncrementsCountAndCreatesSystemEvent() throws Exception {
        String body = """
                {
                  "name": "重复注册测试客户",
                  "phone": "13800008888",
                  "source": "线下到店",
                  "owner": "林夕"
                }
                """;

        String firstResponse = mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registrationCount").value(1))
                .andReturn().getResponse().getContentAsString();
        String customerNo = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(firstResponse).get("id").asText();

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(customerNo))
                .andExpect(jsonPath("$.registrationCount").value(2));

        mockMvc.perform(get("/api/customers/{customerNo}/registration-events", customerNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].registrationNumber").value(2));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void importsToWhiteboardAndRecordsEveryAdminAssignment() throws Exception {
        String body = """
                {
                  "name": "白板分配测试客户",
                  "phone": "13800007777",
                  "company": "优爱分配测试公司",
                  "source": "批量导入",
                  "owner": "白板"
                }
                """;

        String customerNo = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                mockMvc.perform(post("/api/customers/import")
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.owner").value("白板"))
                        .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/customers/{customerNo}/assignment", customerNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"owner\":\"林夕\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("林夕"))
                .andExpect(jsonPath("$.firstAllocationAt").isNotEmpty())
                .andExpect(jsonPath("$.lastAllocationAt").isNotEmpty());

        mockMvc.perform(patch("/api/customers/{customerNo}/assignment", customerNo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"owner\":\"陈晨\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("陈晨"));

        mockMvc.perform(get("/api/customers/{customerNo}/assignment-events", customerNo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].previousOwner").value("林夕"))
                .andExpect(jsonPath("$[0].owner").value("陈晨"))
                .andExpect(jsonPath("$[0].assignedAt").isNotEmpty())
                .andExpect(jsonPath("$[1].previousOwner").value("白板"))
                .andExpect(jsonPath("$[1].owner").value("林夕"));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void aPublicPoolCustomerCanBeClaimedBySales() throws Exception {
        String body = """
                {
                  "name": "公海领取测试客户",
                  "phone": "13800006666",
                  "company": "优爱公海测试公司",
                  "source": "批量导入",
                  "owner": "白板"
                }
                """;
        String customerNo = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                mockMvc.perform(post("/api/customers/import")
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/customers/{customerNo}/pool", customerNo)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inPool\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("公海"));

        mockMvc.perform(get("/api/customers").param("keyword", "公海领取测试客户"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/customers/pool").with(user("chenchen").roles("SALES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(customerNo)).isNotEmpty());

        mockMvc.perform(patch("/api/customers/{customerNo}/pool", customerNo)
                        .with(user("linxi").roles("SALES"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inPool\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value("林夕"))
                .andExpect(jsonPath("$.firstAllocationAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void filtersPublicPoolByDeepTalkDuration() throws Exception {
        String body = """
                {
                  "name": "深沟时长测试客户",
                  "phone": "13800003333",
                  "company": "优爱测试公司",
                  "source": "线上咨询",
                  "owner": "公海"
                }
                """;
        String customerNo = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                mockMvc.perform(post("/api/customers")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/customers/{customerNo}/pool", customerNo)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inPool\":true}"))
                .andExpect(status().isOk());

        CallRecord call = new CallRecord();
        call.setCustomerNo(customerNo);
        call.setCustomerName("深沟时长测试客户");
        call.setPhone("13800003333");
        call.setOwner("公海");
        call.setAgent("管理员");
        call.setDirection("呼出");
        call.setStatus("已接通");
        call.setDurationSeconds(240);
        callRecordRepository.save(call);

        mockMvc.perform(get("/api/customers/pool").param("deepTalkDuration", "3-5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')].deepTalkDurationSeconds".formatted(customerNo))
                        .value(org.hamcrest.Matchers.hasItem(240)));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void masksContactDetailsUntilCustomerIsAssignedToEmployee() throws Exception {
        String body = """
                {
                  "name": "联系方式权限测试客户",
                  "phone": "13800005555",
                  "wechat": "contact_test_55",
                  "company": "权限测试公司",
                  "source": "批量导入",
                  "owner": "公海"
                }
                """;
        String customerNo = new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                mockMvc.perform(post("/api/customers/import")
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.phone").value("13800005555"))
                        .andExpect(jsonPath("$.wechat").value("contact_test_55"))
                        .andExpect(jsonPath("$.contactVisible").value(true))
                        .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(patch("/api/customers/{customerNo}/pool", customerNo)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"inPool\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customers/pool").with(user("chenchen").roles("SALES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')].phone".formatted(customerNo)).value("138****5555"))
                .andExpect(jsonPath("$.content[?(@.id == '%s')].wechat".formatted(customerNo)).value("c****5"))
                .andExpect(jsonPath("$.content[?(@.id == '%s')].contactVisible".formatted(customerNo)).value(false));

        mockMvc.perform(patch("/api/customers/{customerNo}/assignment", customerNo)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"owner\":\"林夕\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customers/{customerNo}", customerNo)
                        .with(user("linxi").roles("SALES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("13800005555"))
                .andExpect(jsonPath("$.wechat").value("contact_test_55"))
                .andExpect(jsonPath("$.contactVisible").value(true));
    }
}
