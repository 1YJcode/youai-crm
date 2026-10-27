package com.youai.crm.communication;
import com.fasterxml.jackson.databind.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
class DeepSeekAnalysisClientTest {
    final ObjectMapper json=new ObjectMapper();
    String source="客户：已经加微信，周五下午再联系我。";
    String result="""
      {"summary":"客户约定周五联系","recommendedTemplate":"一通","recommendationReason":"首次沟通",
       "customerAnalysis":[{"text":"已加微信","evidence":"已经加微信"},{"text":"虚构收入","evidence":"并不存在的收入"}],
       "salesCoaching":[{"text":"建议确认下次联系渠道","evidence":"","kind":"SUGGESTION"}],
       "reminders":[{"text":"周五联系","evidence":"周五下午再联系我","kind":"AGREED"}],
       "templates":[{"name":"一通","fields":[{"label":"加微信：","value":"已添加微信","evidence":"已经加微信"},{"label":"核实资料：","value":"年收入五十万元","evidence":"不存在"}]}]}
      """;
    @Test void officialJsonProtocolUsesEvidenceAndRejectsUnsupportedFields()throws Exception{
        var captured=new AtomicReference<JsonNode>();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/chat/completions",exchange->{try{
            assertEquals("Bearer test-key",exchange.getRequestHeaders().getFirst("Authorization"));captured.set(json.readTree(exchange.getRequestBody()));
            var raw=json.readTree(result);for(var t:new FollowUpTemplates(json).all()){boolean present=false;for(var node:raw.path("templates"))if(t.name().equals(node.path("name").asText()))present=true;if(!present)((com.fasterxml.jackson.databind.node.ArrayNode)raw.path("templates")).addObject().put("name",t.name()).putArray("fields");}
            ((com.fasterxml.jackson.databind.node.ObjectNode)raw).put("title","约定周五联系").put("titleEvidence","周五下午再联系我");
            byte[] bytes=json.writeValueAsBytes(Map.of("choices",new Object[]{Map.of("finish_reason","stop","message",Map.of("content",json.writeValueAsString(raw)))}));exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);
        }finally{exchange.close();}});server.start();
        try{
            var client=new DeepSeekAnalysisClient(json,new FollowUpTemplates(json),"test-key","deepseek-flash","http://127.0.0.1:"+server.getAddress().getPort()+"/chat/completions");
            var data=client.analyze(source,null);assertEquals("json_object",captured.get().path("response_format").path("type").asText());assertEquals("disabled",captured.get().path("thinking").path("type").asText());
            assertEquals("约定周五联系",data.path("title").asText());assertEquals("ALL",data.path("generationScope").asText());assertEquals(1,data.path("customerAnalysis").size());assertEquals("AGREED",data.path("reminders").get(0).path("kind").asText());
            var fields=data.path("templates").get(0).path("fields");
            for(var field:fields){if(field.path("label").asText().equals("核实资料："))assertEquals("未提及",field.path("value").asText());if(field.path("label").asText().equals("加微信："))assertEquals("已添加微信",field.path("value").asText());}
            assertFalse(captured.get().toString().contains("test-key"));assertEquals(new FollowUpTemplates(json).all().size(),data.path("templates").size());
        }finally{server.stop(0);}
    }
    @Test void incompleteOrUnauthorizedResponsesCannotBecomeDrafts()throws Exception{
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var status=new int[]{401};
        server.createContext("/chat/completions",exchange->{byte[] bytes=(status[0]==401?"secret-provider-error":"{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"{}\"}}]}").getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(status[0],bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});server.start();
        try{
            var client=new DeepSeekAnalysisClient(json,new FollowUpTemplates(json),"test-key","deepseek-flash","http://127.0.0.1:"+server.getAddress().getPort()+"/chat/completions");
            var error=assertThrows(Exception.class,()->client.analyze(source,null));assertFalse(error.getMessage().contains("secret-provider-error"));status[0]=200;
            assertThrows(Exception.class,()->client.analyze(source,null));assertThrows(Exception.class,()->client.normalize(json.createObjectNode(),source));
        }finally{server.stop(0);}
    }
}
