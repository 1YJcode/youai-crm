package com.youai.crm.communication;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youai.crm.account.CrmUserRepository;
import com.youai.crm.account.JwtService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in only; the runner supplies a disposable MySQL database. */
@EnabledIfEnvironmentVariable(named = "RUN_CALL_LOAD", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.jpa.properties.hibernate.generate_statistics=true",
    "logging.level.org.hibernate.stat=ERROR",
    "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF",
    "logging.level.root=WARN"
})
class CallListLoadTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired CrmUserRepository users;
    @Autowired JwtService jwt;
    @Autowired EntityManagerFactory emf;
    @Autowired ObjectMapper json;
    @Autowired PlatformTransactionManager transactions;
    @LocalServerPort int port;
    private final List<Map<String,Object>> results = new ArrayList<>();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .version(HttpClient.Version.HTTP_1_1).build();
    private String token;
    private boolean fixtureDatabaseVerified;

    @AfterEach void removeSyntheticFixtures() {
        if (fixtureDatabaseVerified) {
            jdbc.update("delete from crm_call_record where customer_no like 'LOAD%'");
            jdbc.update("delete from crm_customer where customer_no like 'LOAD%'");
        }
    }

    @Test void benchmark() throws Exception {
        String url = System.getenv("TEST_DB_URL");
        assertTrue(url != null && url.startsWith("jdbc:mysql://127.0.0.1:") && url.contains("/recording_test?"),
                "Load fixtures must use the runner's disposable database");
        fixtureDatabaseVerified = true;
        var sales = users.findByUsernameIgnoreCase("linxi").orElseThrow();
        var other = users.findByUsernameIgnoreCase("chenchen").orElseThrow();
        token = jwt.issueAccess(sales);
        for (int count : new int[]{1000, 10000}) {
            new TransactionTemplate(transactions).executeWithoutResult(tx ->
                    seed(count == 1000 ? 0 : 1000, count, sales.getId(), other.getId()));
            for (int size : new int[]{20,100}) {
                for (int i=0;i<10;i++) assertEquals(200, request(size, i%3).statusCode());
                for (int concurrency : new int[]{1,10,30}) measure(count, size, concurrency);
            }
        }
        assertTrue(results.stream().allMatch(r -> ((Number)r.get("errors")).intValue() == 0));
    }

    private void seed(int from, int to, long sales, long other) {
        var customers = new ArrayList<Object[]>();
        var calls = new ArrayList<Object[]>();
        for (int i=from;i<to;i++) {
            String no = "LOAD" + i;
            long owner = i%3 == 0 ? other : sales;
            customers.add(new Object[]{no,"Load customer "+i,"199"+String.format("%08d",i),owner});
            for (int j=0;j<10;j++) {
                long agent = i%2 == 0 ? sales : other;
                calls.add(new Object[]{no,"Load customer "+i,"199"+String.format("%08d",i),agent,
                        agent == sales ? "Linxi" : "Other", TimestampHolder.time(i*10+j)});
            }
        }
        jdbc.batchUpdate("insert into crm_customer(customer_no,name,phone,owner_id,company,source,owner,stage,level,created_at,updated_at) values (?,?,?,?,'Load','Load','Load','Load','A',now(),now())", customers);
        jdbc.batchUpdate("insert into crm_call_record(customer_no,customer_name,phone,agent_user_id,agent,started_at,owner,direction,call_status,duration_seconds,note,recording_source) values (?,?,?,?,?,?,'Load','Load','Load',60,'Synthetic fixture','SYSTEM')", calls);
    }

    private HttpResponse<String> request(int size, int page) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/calls?size="+size+"&page="+page))
                .header("Authorization","Bearer "+token).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private void measure(int customers, int size, int concurrency) throws Exception {
        var stats = emf.unwrap(SessionFactory.class).getStatistics();
        // Isolated single-request probe includes authentication and list serialization queries.
        stats.clear();
        assertEquals(200,request(size,0).statusCode());
        long sqlPerRequest = stats.getPrepareStatementCount();
        stats.clear();
        var latencies = new ConcurrentLinkedQueue<Double>();
        var errors = new AtomicInteger();
        var sequence = new AtomicInteger();
        var startGate = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(concurrency);
        long start = System.nanoTime();
        long deadline = start + TimeUnit.SECONDS.toNanos(5);
        var futures = new ArrayList<Future<?>>();
        for(int n=0;n<concurrency;n++) futures.add(pool.submit(() -> {
            try {
                startGate.await();
                while(System.nanoTime()<deadline) {
                    long before=System.nanoTime();
                    try {
                        var response=request(size, sequence.getAndIncrement()%3 == 0 ? 0 : 5);
                        if(response.statusCode()!=200 || !response.body().contains("customerOwnershipStatus")) errors.incrementAndGet();
                    } catch(Exception e) { errors.incrementAndGet(); }
                    latencies.add((System.nanoTime()-before)/1e6);
                }
            } catch(InterruptedException e) { Thread.currentThread().interrupt(); }
        }));
        startGate.countDown();
        try { for(var future:futures) future.get(60,TimeUnit.SECONDS); }
        finally { pool.shutdownNow(); }
        double seconds=(System.nanoTime()-start)/1e9;
        var sorted=latencies.stream().sorted().toList();
        var row = new LinkedHashMap<String,Object>();
        row.put("syntheticCustomers",customers); row.put("syntheticCalls",customers*10);
        row.put("pageSize",size); row.put("concurrency",concurrency); row.put("requests",sorted.size());
        row.put("errors",errors.get()); row.put("seconds",seconds); row.put("requestsPerSecond",sorted.size()/seconds);
        row.put("p50Ms",percentile(sorted,.50)); row.put("p95Ms",percentile(sorted,.95)); row.put("p99Ms",percentile(sorted,.99));
        row.put("maxMs",sorted.getLast()); row.put("sqlPerSingleRequest",sqlPerRequest);
        row.put("sqlPerSecond",stats.getPrepareStatementCount()/seconds);
        results.add(row);
        var report=new LinkedHashMap<String,Object>();
        report.put("generatedAt",OffsetDateTime.now(ZoneOffset.ofHours(8)).toString());
        report.put("description","Local HTTP, real JWT authorization, isolated MySQL 8.4, Hikari pool 10; warm cache; closed-loop clients; Hibernate SQL instrumentation enabled; no audio upload/AI load");
        report.put("results",results);
        json.writerWithDefaultPrettyPrinter().writeValue(Path.of(System.getProperty("user.dir")).resolve("../.tmp_call_load_results.json").normalize().toFile(),report);
        System.out.println("CALL_LOAD "+json.writeValueAsString(row));
    }
    private double percentile(List<Double> values,double p) { return values.get(Math.min(values.size()-1,(int)Math.ceil(values.size()*p)-1)); }
    private static class TimestampHolder {
        static java.sql.Timestamp time(int i) { return java.sql.Timestamp.valueOf(LocalDateTime.of(2026,10,23,0,0).plusSeconds(i)); }
    }
}
