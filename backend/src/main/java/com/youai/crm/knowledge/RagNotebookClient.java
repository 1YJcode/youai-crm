package com.youai.crm.knowledge;

import com.fasterxml.jackson.databind.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;

@Component
public class RagNotebookClient {
    private final ObjectMapper json;private final String endpoint,token;private final boolean enabled;
    private final HttpClient http=HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
    public RagNotebookClient(ObjectMapper json,@Value("${rag.notebook.endpoint:http://rag-notebook:8091}") String endpoint,
            @Value("${rag.notebook.token:}") String token,@Value("${rag.notebook.enabled:false}") boolean enabled){this.json=json;this.endpoint=endpoint;this.token=token;this.enabled=enabled;}
    public boolean configured(){return enabled&&!token.isBlank();}
    public JsonNode search(String query,List<Map<String,Object>> scopes)throws Exception{
        return request("/v2/search",Map.of("query",query,"scopes",scopes,"top_k",6)).path("matches");
    }
    public JsonNode catalog()throws Exception{return request("/v2/catalog",null).path("sources");}
    public void sync(String scope,String id,String expected,Map<String,Object> document)throws Exception{
        var payload=new LinkedHashMap<String,Object>();payload.put("scope",scope);payload.put("id",id);payload.put("expected",expected);payload.put("document",document);
        request("/v2/sync",payload);
    }
    public void cleanup()throws Exception{request("/v2/cleanup",Map.of());}
    private JsonNode request(String path,Object payload)throws Exception{
        if(!configured())throw new IllegalStateException("知识检索服务尚未启用");
        var builder=HttpRequest.newBuilder(URI.create(endpoint+path)).timeout(Duration.ofMinutes(3))
                .header("X-Rag-Token",token).header("Content-Type","application/json");
        var request=payload==null?builder.GET().build():builder.POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(payload))).build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofInputStream());try(var input=response.body()){
            if(response.statusCode()!=200)throw new IllegalStateException("知识检索暂不可用（HTTP "+response.statusCode()+"）");
            var bytes=input.readNBytes(16*1024*1024+1);if(bytes.length>16*1024*1024)throw new IllegalStateException("检索结果过长");return json.readTree(bytes);
        }
    }
}
