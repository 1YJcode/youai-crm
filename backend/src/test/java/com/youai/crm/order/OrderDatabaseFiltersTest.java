package com.youai.crm.order;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest @AutoConfigureMockMvc @Transactional
@WithMockUser(username="admin", roles="ADMIN")
class OrderDatabaseFiltersTest {
    @Autowired MockMvc mvc;
    @Autowired SalesOrderRepository orders;
    @Autowired com.youai.crm.account.CrmUserRepository users;
    @Autowired com.youai.crm.customer.CustomerRepository customers;
    @Autowired com.youai.crm.invitation.InvitationRecordRepository invitations;
    @Autowired com.youai.crm.account.DepartmentRepository departments;
    @Autowired jakarta.persistence.EntityManager em;

    private void create(String id, String owner, int paid) throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content("""
            {"orderNo":"%s","customer":"数据库筛选客户","product":"线下VIP","amount":100,"paid":%d,"owner":"%s"}
            """.formatted(id, paid, owner))).andExpect(status().isCreated());
    }
    private void invite(String no, String order, String inviter) {
        var i = new com.youai.crm.invitation.InvitationRecord();
        i.setInvitationNo(no); i.setCustomerNo("filter-customer"); i.setInviter(inviter); i.setDepartment("销售部");
        i.setInvitationMethod("电话邀约"); i.setStoreName("优爱天津店"); i.setScheduledAt(java.time.LocalDateTime.now());
        i.setArrivalStatus("已到店"); i.setMaritalStatus(""); i.setAnnualIncome(""); i.setSource("");
        i.setRelatedOrderNo(order); invitations.saveAndFlush(i);
    }
    @Test void returnsPersistedFieldsAndDerivesServiceWhenPaymentChanges() throws Exception {
        create("630001", "林夕", 0);
        var saved = orders.findByOrderNo("630001").orElseThrow();
        assertEquals("线下VIP", saved.getProduct()); assertEquals("优爱天津店", saved.getStoreName()); assertEquals("", saved.getServiceStatus());
        mvc.perform(get("/api/orders/630001")).andExpect(jsonPath("$.storeName").value("优爱天津店"))
                .andExpect(jsonPath("$.product").value("线下VIP")).andExpect(jsonPath("$.service").value(""));
        mvc.perform(patch("/api/orders/630001/payment").contentType(MediaType.APPLICATION_JSON).content("{\"paid\":100}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.statusLabel").value("支付成功"))
                .andExpect(jsonPath("$.service").value("待启用"));
        em.flush(); em.clear(); assertEquals("待启用", orders.findByOrderNo("630001").orElseThrow().getServiceStatus());
        mvc.perform(get("/api/orders").param("keyword","630001").param("serviceStatus","待启用"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/orders").param("keyword","630001").param("serviceStatus","__empty__"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(patch("/api/orders/630001/payment").contentType(MediaType.APPLICATION_JSON).content("{\"paid\":50}"))
                .andExpect(jsonPath("$.service").value(""));
    }
    @Test void combinesAllFiltersBeforePagination() throws Exception {
        create("630011", "林夕", 100); create("630012", "林夕", 100); create("630013", "陈晨", 100); create("630014", "林夕", 0);
        Long ownerId = users.findByUsernameIgnoreCase("linxi").orElseThrow().getId();
        for (String id : java.util.List.of("630011","630012","630013","630014")) {
            var o = orders.findByOrderNo(id).orElseThrow(); o.setCustomerNo("filter-customer");
            o.setCreatedAt(java.time.LocalDateTime.of(2026,11,6,23,59,59));
            invite("INV-"+id,id,"筛选邀约人");
        }
        em.flush();
        mvc.perform(get("/api/orders").param("keyword","数据库筛选").param("customerId","filter-customer")
                .param("ownerId",ownerId.toString()).param("inviter","筛选邀约人").param("paymentStatus","已支付")
                .param("serviceStatus","待启用").param("createdFrom","2026-11-06").param("createdTo","2026-11-06")
                .param("size","1").param("page","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/orders").param("keyword","数据库筛选").param("createdTo","2026-11-05"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void invitationDisplayFilterAndOptionsUseTheSameLatestLinkedRecord() throws Exception {
        create("630021","林夕",100); invite("INV-OLD","630021","旧邀约人"); invite("INV-NEW","630021","新邀约人");
        mvc.perform(get("/api/orders/630021")).andExpect(jsonPath("$.inviter").value("新邀约人"));
        mvc.perform(get("/api/orders").param("keyword","630021").param("inviter","旧邀约人"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/orders").param("keyword","630021").param("inviter","新邀约人"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/orders/filter-options")).andExpect(status().isOk())
                .andExpect(jsonPath("$.inviters[*].value",hasItem("新邀约人")))
                .andExpect(jsonPath("$.inviters[*].value",not(hasItem("旧邀约人"))));
        mvc.perform(get("/api/orders").param("keyword","630021").param("inviter","__empty__"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void salesCannotExpandTheirScopeThroughFiltersOrOptions() throws Exception {
        create("630031","林夕",100); create("630032","陈晨",100); invite("INV-PRIVATE","630032","私有邀约人");
        Long otherId=users.findByUsernameIgnoreCase("chenchen").orElseThrow().getId();
        mvc.perform(get("/api/orders").param("ownerId",otherId.toString()).with(user("linxi").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/orders/filter-options").with(user("linxi").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sales[*].value",not(hasItem(otherId.toString()))))
                .andExpect(jsonPath("$.inviters[*].value",not(hasItem("私有邀约人"))));
    }
    @Test void includesActiveSalespeopleWithoutAnyOrders() throws Exception {
        var employee = users.findByUsernameIgnoreCase("yuanjiang").orElseThrow();
        Long id = employee.getId();
        mvc.perform(get("/api/orders").param("ownerId",id.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/orders/filter-options"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sales[*].value",hasItem(id.toString())))
                .andExpect(jsonPath("$.sales[*].label",hasItem(employee.getDisplayName())));
        mvc.perform(get("/api/orders/filter-options").with(user("yuanjiang").roles("SALES")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sales.length()").value(1))
                .andExpect(jsonPath("$.sales[0].value").value(id.toString()));
    }

    @Test void newOrdersUseTheSalesDepartmentsPersistedStore() throws Exception {
        var dept = departments.findByCode("SALES").orElseThrow(); dept.setStoreName("测试门店"); em.flush();
        create("630041","林夕",0);
        mvc.perform(get("/api/orders/630041")).andExpect(jsonPath("$.storeName").value("测试门店"));
    }
}
