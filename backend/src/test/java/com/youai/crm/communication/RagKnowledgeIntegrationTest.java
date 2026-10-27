package com.youai.crm.communication;

import com.fasterxml.jackson.databind.*;
import com.youai.crm.knowledge.*;
import com.youai.crm.account.*;
import com.youai.crm.customer.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDateTime;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=${TEST_DB_URL:jdbc:h2:mem:rag_integration;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE}","recording.queue.enabled=false","recording.deepseek.automatic-backfill=false"})
@AutoConfigureMockMvc
class RagKnowledgeIntegrationTest {
    @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired KnowledgeRepository articles;
    @Autowired CustomerRepository customers;@Autowired CrmUserRepository users;@Autowired CallRecordRepository calls;
    @Autowired CustomerTranscriptSegmentRepository segments;
    @MockitoBean RagNotebookClient rag;@MockitoBean RagAnswerClient answer;
    Customer customer;KnowledgeArticle company;CustomerTranscriptSegment segment;CallRecord call;
    List<Long> created=new ArrayList<>();
    @BeforeEach void setup()throws Exception{
        customer=customers.findAll().getFirst();customer.setOwnerId(users.findByUsernameIgnoreCase("linxi").orElseThrow().getId());customers.saveAndFlush(customer);
        company=article("公开案例","先核实顾虑，再约定具体回访时间。",null);
        call=new CallRecord();call.setCustomerNo(customer.getCustomerNo());call.setCustomerName(customer.getName());call.setAgent("林夕");call.setOwner("林夕");call.setPhone("13800000000");call.setDirection("呼出");call.setStatus("已接通");call.setStartedAt(LocalDateTime.now());call=calls.saveAndFlush(call);
        segment=new CustomerTranscriptSegment();segment.callId=call.getId();segment.customerNo=customer.getCustomerNo();segment.customerName=customer.getName();segment.salesName="林夕";segment.recordingSource="UPLOAD";segment.provider="cloud";segment.recordingRevision=UUID.randomUUID().toString();segment.transcript="客户提出预算顾虑，销售约定周五回访。";segment.sourceTime=LocalDateTime.now();segment.addedAt=LocalDateTime.now();segment=segments.saveAndFlush(segment);
        when(rag.configured()).thenReturn(true);when(answer.configured()).thenReturn(true);
        when(rag.search(anyString(),anyList())).thenAnswer(inv->{var scopes=(List<Map<String,Object>>)inv.getArgument(1);var docs=(List<Map<String,Object>>)scopes.get(scopes.size()-1).get("documents");return json.valueToTree(docs.stream().limit(1).map(d->Map.of("id",d.get("id"),"version",d.get("version"),"content",d.get("content"),"chunk",0)).toList());});
        when(answer.answer(anyString(),anyList(),anyBoolean())).thenAnswer(inv->{var e=((List<Map<String,Object>>)inv.getArgument(1)).getFirst();String quote=((String)e.get("content")).substring(0,Math.min(15,((String)e.get("content")).length()));return json.valueToTree(Map.of("answer","建议核实顾虑并约定回访，依据见引用。","citations",List.of(Map.of("id",e.get("id"),"chunk",0,"quote",quote))));});
    }
    KnowledgeArticle article(String title,String text,String privateCustomer){var a=new KnowledgeArticle();a.title=title;a.content=text;a.category="测试";a.createdBy="admin";a.updatedBy="admin";a.customerNo=privateCustomer;if(privateCustomer!=null)a.sourceCallId=99999L;a=articles.saveAndFlush(a);created.add(a.id);return a;}
    @AfterEach void cleanup(){segments.deleteById(segment.id);calls.deleteById(call.getId());created.forEach(articles::deleteById);created.clear();}
    String request(boolean personal)throws Exception{var body=json.createObjectNode().put("question","如何处理客户顾虑？");if(personal)body.put("customerNo",customer.getCustomerNo()).put("segmentId",segment.id);return body.toString();}
    @Test void companyQuestionsCannotSearchPrivateCustomerSources()throws Exception{
        var hidden=article("不可共享的原始案例","PRIVATE-OTHER-CUSTOMER",customer.getCustomerNo());
        mvc.perform(post("/api/knowledge/ask").with(user("chenchen").roles("SALES")).contentType("application/json").content(request(false)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.grounded").value(true)).andExpect(jsonPath("$.citations[0].articleId").value(company.id)).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRIVATE-OTHER-CUSTOMER"))));
        verify(rag).search(anyString(),argThat(scopes->scopes.size()==1&&!scopes.toString().contains("article:"+hidden.id)&&!scopes.toString().contains("segment:")));
    }
    @Test void authorizedCustomerAnswersLinkToExactTranscriptAndRejectNonOwner()throws Exception{
        mvc.perform(post("/api/knowledge/ask").with(user("linxi").roles("SALES")).contentType("application/json").content(request(true))).andExpect(status().isOk()).andExpect(jsonPath("$.citations[0].segmentId").value(segment.id));
        clearInvocations(rag,answer);
        mvc.perform(post("/api/knowledge/ask").with(user("chenchen").roles("SALES")).contentType("application/json").content(request(true))).andExpect(status().isForbidden());verifyNoInteractions(rag,answer);
        mvc.perform(get("/api/customers/"+customer.getCustomerNo()+"/transcript/segments/"+segment.id).with(user("chenchen").roles("SALES"))).andExpect(status().isForbidden());
    }
    @Test void foreignRetrieverIdsAndInventedQuotesCannotBecomeAnswers()throws Exception{
        doReturn(json.valueToTree(List.of(Map.of("id","segment:999999","version","0","content","PRIVATE-FOREIGN","chunk",0)))).when(rag).search(anyString(),anyList());
        mvc.perform(post("/api/knowledge/ask").with(user("linxi").roles("SALES")).contentType("application/json").content(request(false))).andExpect(status().isOk()).andExpect(jsonPath("$.grounded").value(false));verifyNoInteractions(answer);
    }
    @Test void revokedArticleIsFilteredBeforeCallingLanguageModel()throws Exception{
        doAnswer(inv->{var d=((List<Map<String,Object>>)((List<Map<String,Object>>)inv.getArgument(1)).getFirst().get("documents")).getFirst();articles.deleteById(company.id);created.remove(company.id);return json.valueToTree(List.of(Map.of("id",d.get("id"),"version",d.get("version"),"content",d.get("content"),"chunk",0)));}).when(rag).search(anyString(),anyList());
        mvc.perform(post("/api/knowledge/ask").with(user("linxi").roles("SALES")).contentType("application/json").content(request(false))).andExpect(status().isOk()).andExpect(jsonPath("$.grounded").value(false));verifyNoInteractions(answer);
    }
    @Test void inventedCitationQuoteCannotBeDisplayedAsGroundedAnswer()throws Exception{
        doReturn(json.valueToTree(Map.of("answer","无依据的成交保证","citations",List.of(Map.of("id","article:"+company.id,"chunk",0,"quote","原文没有这样的保证"))))).when(answer).answer(anyString(),anyList(),anyBoolean());
        mvc.perform(post("/api/knowledge/ask").with(user("linxi").roles("SALES")).contentType("application/json").content(request(false))).andExpect(status().isOk()).andExpect(jsonPath("$.grounded").value(false)).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("无依据的成交保证"))));
    }
    @Test void customerTransferDuringGenerationPreventsAnswerDisclosure()throws Exception{
        doAnswer(inv->{var fresh=customers.findById(customer.getId()).orElseThrow();fresh.setOwnerId(users.findByUsernameIgnoreCase("chenchen").orElseThrow().getId());customers.saveAndFlush(fresh);return json.valueToTree(Map.of("answer","不得返回的答案","citations",List.of()));}).when(answer).answer(anyString(),anyList(),anyBoolean());
        mvc.perform(post("/api/knowledge/ask").with(user("linxi").roles("SALES")).contentType("application/json").content(request(true))).andExpect(status().isForbidden()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("不得返回的答案"))));
    }
}
