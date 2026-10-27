package com.youai.crm.communication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.junit.jupiter.api.BeforeAll;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
        "server.address=127.0.0.1","spring.datasource.url=jdbc:h2:mem:storage_browser;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop","recording.storage.directory=./target/storage-browser-audio",
        "recording.queue.poll-ms=2000"})
@EnabledIfEnvironmentVariable(named="RUN_RECORDING_STORAGE_BROWSER",matches="true")
class RecordingStorageBrowserTest {
    private static final String BUCKET="recording-browser-"+java.util.UUID.randomUUID();
    @DynamicPropertySource static void storage(DynamicPropertyRegistry properties) {
        String endpoint=System.getenv("S3_BROWSER_TEST_ENDPOINT");
        if(endpoint==null || endpoint.isBlank()) return;
        properties.add("recording.storage.backend",()->"s3"); properties.add("recording.storage.s3.bucket",()->BUCKET);
        properties.add("recording.storage.s3.endpoint",()->endpoint);
        properties.add("recording.storage.s3.path-style",()->true);
        properties.add("recording.storage.s3.access-key",()->"storage-test-user");
        properties.add("recording.storage.s3.secret-key",()->"storage-test-password-20261025");
    }
    @BeforeAll static void bucket() {
        String endpoint=System.getenv("S3_BROWSER_TEST_ENDPOINT");
        if(endpoint==null || endpoint.isBlank()) return;
        try(var client=software.amazon.awssdk.services.s3.S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.US_EAST_1).endpointOverride(java.net.URI.create(endpoint))
                .credentialsProvider(software.amazon.awssdk.auth.credentials.StaticCredentialsProvider.create(
                        software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create("storage-test-user","storage-test-password-20261025")))
                .serviceConfiguration(software.amazon.awssdk.services.s3.S3Configuration.builder().pathStyleAccessEnabled(true).build()).build()) {
            client.createBucket(software.amazon.awssdk.services.s3.model.CreateBucketRequest.builder().bucket(BUCKET).build());
        }
    }
    @LocalServerPort int port;
    @MockitoBean QwenTranscriptionClient cloud;
    @MockitoBean RecordingAiClient ai;
    @Test void browserUploadsQueuesAndReadsHistory() throws Exception {
        when(cloud.configured()).thenReturn(true);
        when(cloud.transcribe(any(),eq("audio/wav"))).thenReturn("客户：请周五下午回访。");
        Path project=Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if(project.getFileName().toString().equals("backend")) project=project.getParent();
        Files.createDirectories(project.resolve(".tmp_recording_browser"));
        var builder=new ProcessBuilder("node",project.resolve("scripts/test-recording-e2e.cjs").toString(),Integer.toString(port));
        builder.environment().put("RECORDING_STORAGE_E2E","true");
        Path log=project.resolve(".tmp_storage_browser.log"); builder.directory(project.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        var process=builder.start(); boolean finished=process.waitFor(90,TimeUnit.SECONDS);
        if(!finished) process.destroyForcibly(); assertTrue(finished,"Browser timeout; see "+log);
        assertEquals(0,process.exitValue(),Files.readString(log)); verify(cloud,times(1)).transcribe(any(),eq("audio/wav"));
    }
}
