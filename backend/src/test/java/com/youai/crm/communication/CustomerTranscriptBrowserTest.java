package com.youai.crm.communication;
import com.youai.crm.customer.*;
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
        "spring.datasource.url=jdbc:h2:mem:customer_transcript_browser;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop","recording.queue.enabled=false"})
@EnabledIfEnvironmentVariable(named="RUN_CUSTOMER_TRANSCRIPT_BROWSER",matches="true")
class CustomerTranscriptBrowserTest {
    @LocalServerPort int port;
    @Autowired CustomerRepository customers;
    @Autowired CrmUserRepository users;
    @Autowired CallRecordRepository calls;
    @Autowired CallRecordingRepository recordings;
    @Autowired RecordingTranscriptionRepository results;
    @Autowired RecordingKnowledgeService knowledge;
    @Test void cumulativeCustomerArchiveAndMultiSourceCaseKeepSourcePrivacy()throws Exception{
        var customer=customers.findAll().getFirst();customer.setOwnerId(users.findByUsernameIgnoreCase("linxi").orElseThrow().getId());customers.saveAndFlush(customer);
        Long firstCall=null,firstSource=null;
        for(int i=0;i<2;i++){
            var c=new CallRecord();c.setCustomerNo(customer.getCustomerNo());c.setCustomerName(customer.getName());c.setPhone(customer.getPhone());c.setOwner("林夕");
            c.setAgent(i==0?"林夕":"陈晨");c.setAgentUserId(users.findByUsernameIgnoreCase(i==0?"linxi":"chenchen").orElseThrow().getId());
            c.setDirection("外部通话");c.setStatus("已接通");c.setRecordingSource("UPLOAD");c.setDurationSeconds(i==0?42:75);c.setStartedAt(LocalDateTime.now());calls.saveAndFlush(c);
            var audio=recordings.saveAndFlush(new CallRecording(c.getId(),"audio/wav",RecordingFixtures.wav()));
            var text=new RecordingTranscription(c.getId()+":cloud",audio.getRevision());text.status="COMPLETED";text.transcript="PRIVATE-BROWSER-ORIGINAL 第"+(i+1)+"次客户沟通";text.started=LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);results.saveAndFlush(text);
            Long source=knowledge.capture(c.getId(),"cloud",audio.getRevision(),text.started,text.transcript,LocalDateTime.now(),"linxi",false);
            if(i==0){firstCall=c.getId();firstSource=source;}
        }
        Path root=Path.of(System.getProperty("user.dir")).toAbsolutePath();if(root.getFileName().toString().equals("backend"))root=root.getParent();
        var builder=new ProcessBuilder("node",root.resolve("scripts/test-recording-e2e.cjs").toString(),Integer.toString(port),"customer-transcript");
        builder.environment().put("COMPANY_CASE_CALL_ID",firstCall.toString());builder.environment().put("COMPANY_CASE_SOURCE_ID",firstSource.toString());builder.environment().put("COMPANY_CASE_CUSTOMER_ID",customer.getCustomerNo());
        Path log=root.resolve(".tmp_customer_transcript_browser.log");var process=builder.directory(root.toFile()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        boolean finished=process.waitFor(90,TimeUnit.SECONDS);if(!finished)process.destroyForcibly();assertTrue(finished,"Browser timeout");assertEquals(0,process.exitValue(),Files.readString(log));
    }
}
