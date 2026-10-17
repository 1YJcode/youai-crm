package com.youai.crm.knowledge;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "server.address=127.0.0.1","spring.datasource.url=jdbc:h2:mem:knowledge_browser;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop"})
@EnabledIfEnvironmentVariable(named="RUN_KNOWLEDGE_BROWSER",matches="true")
class KnowledgeBrowserE2ETest {
    @LocalServerPort int port;
    @Test void browserKnowledgeManagement() throws Exception {
        Path root=Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if(root.getFileName().toString().equals("backend"))root=root.getParent();
        Path log=root.resolve(".tmp_knowledge_e2e.log");
        var process=new ProcessBuilder("node",root.resolve("scripts/test-recording-e2e.cjs").toString(),Integer.toString(port),"knowledge")
                .directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean finished=process.waitFor(90,TimeUnit.SECONDS);if(!finished)process.destroyForcibly();
        assertTrue(finished,"Browser test timeout");assertEquals(0,process.exitValue(),java.nio.file.Files.readString(log));
    }
}
