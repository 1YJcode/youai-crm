package com.youai.crm.communication;
import com.youai.crm.customer.CustomerRepository;
import com.youai.crm.account.CrmUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"server.address=127.0.0.1","recording.deepseek.automatic-backfill=false",
        "spring.datasource.url=jdbc:h2:mem:call_pipeline_browser;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop","recording.queue.poll-ms=300","recording.storage.directory=./target/pipeline-browser-audio"})
@EnabledIfEnvironmentVariable(named="RUN_CALL_PIPELINE_BROWSER",matches="true")
class CallPipelineBrowserTest {
    @LocalServerPort int port;
    @Autowired CustomerRepository customers;
    @Autowired CrmUserRepository users;
    @Autowired ObjectMapper json;
    @Autowired FollowUpTemplates templates;
    @Autowired com.youai.crm.knowledge.KnowledgeRepository articles;
    @MockitoBean com.youai.crm.knowledge.RagNotebookClient rag;
    @MockitoBean com.youai.crm.knowledge.RagAnswerClient ragAnswer;
    @MockitoBean QwenTranscriptionClient cloud;
    @MockitoBean DeepSeekAnalysisClient deepseek;
    @Test void automaticUploadAnalysisTemplateAndConfirmedFollowupFlow()throws Exception{
        var shared=new com.youai.crm.knowledge.KnowledgeArticle();shared.title="回访沟通案例";shared.category="公司案例";shared.content="先确认顾虑，再约定下次联系。";shared.createdBy="admin";shared.updatedBy="admin";articles.saveAndFlush(shared);
        when(rag.configured()).thenReturn(true);when(ragAnswer.configured()).thenReturn(true);
        when(rag.search(anyString(),anyList())).thenAnswer(inv->{var scopes=(java.util.List<java.util.Map<String,Object>>)inv.getArgument(1);var docs=(java.util.List<java.util.Map<String,Object>>)scopes.getLast().get("documents");var doc=docs.getFirst();return json.valueToTree(java.util.List.of(java.util.Map.of("id",doc.get("id"),"version",doc.get("version"),"content",doc.get("content"),"chunk",0)));});
        when(ragAnswer.answer(anyString(),anyList(),anyBoolean())).thenAnswer(inv->{var e=((java.util.List<java.util.Map<String,Object>>)inv.getArgument(1)).getFirst();String text=(String)e.get("content");return json.valueToTree(java.util.Map.of("answer","建议先确认客户顾虑，再明确下次联系时间。","citations",java.util.List.of(java.util.Map.of("id",e.get("id"),"chunk",0,"quote",text.substring(0,Math.min(15,text.length()))))));});
        var customer=customers.findAll().getFirst();customer.setOwnerId(users.findByUsernameIgnoreCase("linxi").orElseThrow().getId());customers.saveAndFlush(customer);
        when(cloud.configured()).thenReturn(true);when(cloud.transcribe(any(),anyString())).thenReturn("客户：已经加微信，周五下午再联系我。","客户：已经加微信，咨询服务费用，下周继续联系。");
        when(deepseek.configured()).thenReturn(true);when(deepseek.model()).thenReturn("deepseek-flash");
        var raw=json.readTree("{\"summary\":\"客户约定周五继续联系\",\"recommendedTemplate\":\"一通\",\"recommendationReason\":\"首次需求沟通\",\"templates\":[{\"name\":\"一通\",\"fields\":[{\"label\":\"加微信：\",\"value\":\"已加微信\",\"evidence\":\"已经加微信\"}]}]}");
        ((com.fasterxml.jackson.databind.node.ArrayNode)raw.path("templates")).addObject().put("name","二通").putArray("fields");
        for(var t:templates.all()){boolean present=false;for(var node:raw.path("templates"))if(t.name().equals(node.path("name").asText()))present=true;if(!present)((com.fasterxml.jackson.databind.node.ArrayNode)raw.path("templates")).addObject().put("name",t.name()).putArray("fields");}
        ((com.fasterxml.jackson.databind.node.ObjectNode)raw).put("title","约定周五继续联系").put("titleEvidence","周五下午再联系我");
        when(deepseek.analyze(anyString(),nullable(String.class))).thenAnswer(inv->{String text=inv.getArgument(0);var response=raw.deepCopy();if(text.contains("服务费用"))((com.fasterxml.jackson.databind.node.ObjectNode)response).put("title","咨询服务费用").put("titleEvidence","咨询服务费用");return new DeepSeekAnalysisClient(json,templates,"unused","deepseek-flash","http://127.0.0.1").normalize(response,text);});
        Path root=Path.of(System.getProperty("user.dir")).toAbsolutePath();if(root.getFileName().toString().equals("backend"))root=root.getParent();
        var builder=new ProcessBuilder("node",root.resolve("scripts/test-recording-e2e.cjs").toString(),Integer.toString(port),"call-intelligence");builder.environment().put("PIPELINE_CUSTOMER_ID",customer.getCustomerNo());
        Path log=root.resolve(".tmp_call_pipeline_browser.log");var process=builder.directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean finished=process.waitFor(90,TimeUnit.SECONDS);if(!finished)process.destroyForcibly();assertTrue(finished,"Browser timed out");assertEquals(0,process.exitValue(),Files.readString(log));
        verify(cloud,times(2)).transcribe(any(),anyString());verify(deepseek,times(2)).analyze(anyString(),nullable(String.class));
    }
}
