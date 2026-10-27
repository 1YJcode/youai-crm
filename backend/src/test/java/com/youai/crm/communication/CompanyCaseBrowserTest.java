package com.youai.crm.communication;

import com.youai.crm.customer.CustomerRepository;
import com.youai.crm.account.CrmUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"server.address=127.0.0.1",
        "spring.datasource.url=jdbc:h2:mem:company_case_browser;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop","recording.queue.enabled=false"})
@EnabledIfEnvironmentVariable(named="RUN_COMPANY_CASE_BROWSER",matches="true")
class CompanyCaseBrowserTest {
    @LocalServerPort int port;
    @Autowired CallRecordRepository calls;
    @Autowired CallRecordingRepository recordings;
    @Autowired RecordingTranscriptionRepository transcriptions;
    @Autowired RecordingKnowledgeService knowledge;
    @Autowired CustomerRepository customers;
    @Autowired CrmUserRepository users;
    @Test void ownerPublishesAndOtherEmployeeReadsOnlyCuratedCompanyCase() throws Exception {
        var call=calls.findAll().getFirst();var customer=customers.findByCustomerNo(call.getCustomerNo()).orElseThrow();
        customer.setOwnerId(users.findByUsernameIgnoreCase("linxi").orElseThrow().getId());customers.saveAndFlush(customer);
        var audio=recordings.saveAndFlush(new CallRecording(call.getId(),"audio/wav",RecordingFixtures.wav()));
        var text=new RecordingTranscription(call.getId()+":cloud",audio.getRevision());text.status="COMPLETED";
        text.started=LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);text.transcript="PRIVATE-BROWSER-ORIGINAL 原始客户内容";transcriptions.saveAndFlush(text);
        Long source=knowledge.capture(call.getId(),"cloud",audio.getRevision(),text.started,text.transcript,LocalDateTime.now(),"linxi",false);
        Path project=Path.of(System.getProperty("user.dir")).toAbsolutePath();if(project.getFileName().toString().equals("backend"))project=project.getParent();
        var builder=new ProcessBuilder("node",project.resolve("scripts/test-recording-e2e.cjs").toString(),Integer.toString(port),"company-case");
        builder.environment().put("COMPANY_CASE_CALL_ID",call.getId().toString());builder.environment().put("COMPANY_CASE_SOURCE_ID",source.toString());
        Path log=project.resolve(".tmp_company_case_browser.log");builder.directory(project.toFile()).redirectErrorStream(true).redirectOutput(log.toFile());
        var process=builder.start();boolean finished=process.waitFor(90,TimeUnit.SECONDS);if(!finished)process.destroyForcibly();
        assertTrue(finished,"Browser timeout; see "+log);assertEquals(0,process.exitValue(),Files.readString(log));
    }
}
