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
        if(!configured())throw new IllegalStateException("知识检索服务尚未启用");
        var body=json.writeValueAsBytes(Map.of("query",query,"scopes",scopes,"top_k",6));
        var request=HttpRequest.newBuilder(URI.create(endpoint+"/v1/search")).timeout(Duration.ofMinutes(3))
                .header("X-Rag-Token",token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofInputStream());try(var input=response.body()){
            if(response.statusCode()!=200)throw new IllegalStateException("知识检索暂不可用（HTTP "+response.statusCode()+"）");
            var bytes=input.readNBytes(1024*1024+1);if(bytes.length>1024*1024)throw new IllegalStateException("检索结果过长");return json.readTree(bytes).path("matches");
        }
    }
}
