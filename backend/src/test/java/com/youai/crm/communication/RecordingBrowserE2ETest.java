package com.youai.crm.communication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in: launches a local browser against an isolated random-port test server. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "server.address=127.0.0.1", "spring.datasource.url=jdbc:h2:mem:recording_browser;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@EnabledIfEnvironmentVariable(named="RUN_RECORDING_BROWSER", matches="true")
class RecordingBrowserE2ETest {
    @LocalServerPort int port;
    @MockitoBean RecordingAiClient ai;
    @Test
    void browserUsesRealAuthenticatedUploadAnalysisAndReviewEndpoints() throws Exception {
        when(ai.configured()).thenReturn(true);
        when(ai.analyze(any(), anyString())).thenReturn(new RecordingAiClient.Result("客户：请周五下午回访。", "约定事项：周五下午回访。"));
        Path project = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (project.getFileName().toString().equals("backend")) project = project.getParent();
        var builder = new ProcessBuilder("node", project.resolve("scripts/test-recording-e2e.cjs").toString(), Integer.toString(port));
        builder.directory(project.toFile()).redirectErrorStream(true);
        Path log = project.resolve(".tmp_recording_e2e.log");
        builder.redirectOutput(log.toFile());
        var process = builder.start();
        boolean finished = process.waitFor(90, TimeUnit.SECONDS);
        if (!finished) process.destroyForcibly();
        assertTrue(finished, "Browser timed out; see " + log);
        assertEquals(0, process.exitValue(), java.nio.file.Files.readString(log));
        verify(ai, times(1)).analyze(any(), eq("audio/wav"));
    }
}
