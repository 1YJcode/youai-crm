package com.youai.crm.order;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
@SpringBootTest @AutoConfigureMockMvc @Transactional
@WithMockUser(username="admin",roles="ADMIN")
class ContractControllerTest {
 @Autowired MockMvc mvc;
 @Autowired SalesContractRepository contracts;
 @Autowired jakarta.persistence.EntityManager em;
 @Test void includesEmployeesWithoutContracts() throws Exception {
  mvc.perform(get("/api/contracts/owners")).andExpect(status().isOk())
   .andExpect(jsonPath("$",org.hamcrest.Matchers.hasItems("林夕","陈晨","周倩","赵磊","元江")));
 }
 @Test void persistsAndFiltersAndReviews() throws Exception {
  mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
   {"orderNo":"619901","customer":"合同测试会员","product":"VIP","amount":100,"paid":0,"owner":"林夕"}
   """)).andExpect(status().isCreated());
  org.junit.jupiter.api.Assertions.assertTrue(contracts.findByContractNo("HT-619901").isPresent());
  var contract=contracts.findByContractNo("HT-619901").orElseThrow();
  contract.order.setCustomerNo("contract-member-001"); em.flush(); em.clear();
  mvc.perform(get("/api/contracts").param("customerId","contract-member-001"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(get("/api/contracts").param("owner","林夕").param("status","待审核"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.content[?(@.contractNo == 'HT-619901')].status").value(org.hamcrest.Matchers.hasItem("待审核")));
  mvc.perform(get("/api/contracts").param("customerId","missing-member"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
  mvc.perform(patch("/api/contracts/HT-619901/review").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"审核通过\",\"note\":\"核验完成\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.reviewedBy").isNumber()).andExpect(jsonPath("$.reviewedAt").isNotEmpty());
  em.flush(); em.clear();
  mvc.perform(get("/api/contracts/HT-619901"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("审核通过")).andExpect(jsonPath("$.reviewNote").value("核验完成"));
  mvc.perform(patch("/api/contracts/HT-619901/review").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"任意状态\"}"))
   .andExpect(status().isBadRequest());
  mvc.perform(get("/api/contracts").param("size","1"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(1));
 }
 @Test @WithMockUser(username="linxi",roles="SALES")
 void rejectsNonAdminReviewsAndOtherOwners() throws Exception {
  mvc.perform(post("/api/orders").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN"))
    .contentType(MediaType.APPLICATION_JSON).content("""
     {"orderNo":"619902","customer":"另一销售的合同","product":"VIP","amount":100,"paid":0,"owner":"陈晨"}
     """)).andExpect(status().isCreated());
  mvc.perform(get("/api/contracts/HT-619902")).andExpect(status().isForbidden());
  mvc.perform(get("/api/contracts").param("owner","陈晨")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
  mvc.perform(get("/api/contracts/owners")).andExpect(status().isOk()).andExpect(jsonPath("$",org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("陈晨"))));
  mvc.perform(patch("/api/contracts/missing/review").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"审核通过\"}"))
   .andExpect(status().isForbidden());
  mvc.perform(get("/api/contracts").param("owner","admin"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
 }
}
