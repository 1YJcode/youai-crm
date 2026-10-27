package com.youai.crm.communication;
import com.youai.crm.knowledge.RagNotebookClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class RagNotebookClientTest {
    @Test void internalClientUsesHttpOneWithoutUpgradeAndPassesOnlyServerToken()throws Exception{
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        server.createContext("/v1/search",exchange->{try{
            assertNull(exchange.getRequestHeaders().getFirst("Upgrade"));assertEquals("internal-test",exchange.getRequestHeaders().getFirst("X-Rag-Token"));
            var body=new ObjectMapper().readTree(exchange.getRequestBody());assertEquals("company",body.path("scopes").get(0).path("id").asText());
            byte[] bytes="{\"matches\":[]}".getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);
        }catch(Throwable e){failure.set(e);}finally{exchange.close();}});server.start();
        try{var client=new RagNotebookClient(new ObjectMapper(),"http://127.0.0.1:"+server.getAddress().getPort(),"internal-test",true);assertTrue(client.search("test",List.of(Map.of("id","company","documents",List.of()))).isArray());assertNull(failure.get());}finally{server.stop(0);}
    }
}
