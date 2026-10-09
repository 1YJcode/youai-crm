package com.youai.crm.customer;

import com.youai.crm.common.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerPoolConcurrencyTest {
    @Autowired CustomerRepository customers;
    @Autowired CustomerAssignmentService assignments;
    @Autowired CustomerAssignmentEventRepository events;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MockMvc mvc;

    @Test
    void simultaneousClaimsCommitOnlyOnceAndRollBackLosingAudit() throws Exception {
        String number = "race-" + UUID.randomUUID().toString().substring(0, 24);
        Customer customer = new Customer();
        customer.setCustomerNo(number);
        customer.setName("Concurrent pool claim");
        customer.setPhone(UUID.randomUUID().toString().substring(0, 20));
        customer.setCompany("Test");
        customer.setSource("Test");
        customer.setOwner("公海");
        customer.setStage("Test");
        customer.setLevel("Test");
        customer.setCity("Test");
        customer.setNote("Test");
        customer = customers.saveAndFlush(customer);
        Long customerId = customer.getId();
        CyclicBarrier bothRead = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            java.util.function.Function<String, Callable<Throwable>> claim = username -> () -> {
                try {
                    new TransactionTemplate(transactions).executeWithoutResult(status -> {
                        // Force both transactions to read the same version before either writes.
                        assertThat(customers.findByCustomerNo(number).orElseThrow().getOwner()).isEqualTo("公海");
                        try { bothRead.await(10, TimeUnit.SECONDS); }
                        catch (Exception failure) { throw new IllegalStateException(failure); }
                        assignments.updatePool(number, false, new UsernamePasswordAuthenticationToken(
                                username, "", AuthorityUtils.createAuthorityList("ROLE_SALES")));
                    });
                    return null;
                } catch (Throwable failure) { return failure; }
            };
            Future<Throwable> first = executor.submit(claim.apply("linxi"));
            Future<Throwable> second = executor.submit(claim.apply("chenchen"));
            Throwable one = first.get(30, TimeUnit.SECONDS);
            Throwable two = second.get(30, TimeUnit.SECONDS);
            assertThat((one == null ? 1 : 0) + (two == null ? 1 : 0)).isEqualTo(1);
            Throwable loser = one == null ? two : one;
            assertThat(loser).isInstanceOf(OptimisticLockingFailureException.class);
            var response = new ApiExceptionHandler().handleConcurrentUpdate((Exception) loser);
            assertThat(response.getStatusCode().value()).isEqualTo(409);
            assertThat(response.getBody().code()).isEqualTo("CONCURRENT_UPDATE");
            assertThat(customers.findByCustomerNo(number).orElseThrow().getOwnerId()).isNotNull();
            assertThat(events.findAllByCustomerIdOrderByAssignedAtDesc(customerId)).hasSize(1);

            // Even an administrator cannot claim an already assigned customer via the pool endpoint.
            mvc.perform(patch("/api/customers/" + number + "/pool").with(user("admin").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"inPool\":false}"))
                    .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
            assertThat(events.findAllByCustomerIdOrderByAssignedAtDesc(customerId)).hasSize(1);
        } finally {
            executor.shutdownNow();
            new TransactionTemplate(transactions).executeWithoutResult(status -> {
                events.deleteAll(events.findAllByCustomerIdOrderByAssignedAtDesc(customerId));
                customers.deleteById(customerId);
            });
        }
    }
}
