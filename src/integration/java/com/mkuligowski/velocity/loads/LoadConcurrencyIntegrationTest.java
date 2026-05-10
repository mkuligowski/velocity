package com.mkuligowski.velocity.loads;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Exercises the concurrent same-customer race that the per-customer DB lock is designed
 * to close. Without the lock, two threads reading the daily total at the same moment
 * could both pass the limit check and both be accepted over-limit.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class LoadConcurrencyIntegrationTest {

    @Autowired private TestRestTemplate rest;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void resetState() {
        jdbc.execute("DELETE FROM load_attempt_event");
        jdbc.execute("DELETE FROM load_attempt_snapshot");
        jdbc.execute("DELETE FROM load_attempt_idempotency");
        jdbc.execute("DELETE FROM customer_lock");
    }

    @Test
    void parallel_attempts_never_exceed_daily_amount_limit() throws Exception {
        // Each attempt is $1000; the daily cap is $5000. Fire 10 parallel attempts for
        // the same customer — at most 5 may be accepted, never 6+.
        int attempts = 10;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        List<Callable<HttpStatus>> jobs = new ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            int idx = i;
            jobs.add(() -> postLoad("c-1", "load-" + idx, "$1000.00", "2018-01-01T00:00:0" + idx + "Z"));
        }

        List<Future<HttpStatus>> futures = pool.invokeAll(jobs);
        pool.shutdown();

        long accepted = jdbc.queryForObject(
                "SELECT COUNT(*) FROM load_attempt_snapshot WHERE accepted = TRUE",
                Long.class);
        BigDecimal totalAccepted = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount), 0) FROM load_attempt_snapshot WHERE accepted = TRUE",
                BigDecimal.class);

        // Every request returned 200 (no duplicates, since all load IDs are distinct).
        for (Future<HttpStatus> f : futures) {
            assertThat(f.get()).isEqualTo(HttpStatus.OK);
        }

        // Daily count limit: ≤ 3 accepted. So at most 3 accepts × $1000 = $3000.
        // Both limits hold simultaneously.
        assertThat(accepted)
                .as("daily-count limit must hold under concurrency")
                .isLessThanOrEqualTo(3);
        assertThat(totalAccepted)
                .as("daily-amount limit must hold under concurrency")
                .isLessThanOrEqualTo(new BigDecimal("5000.0000"));
    }

    @Test
    void parallel_attempts_for_different_customers_do_not_serialize_unnecessarily() throws Exception {
        // Sanity check: per-customer lock should not serialize ACROSS customers.
        // 5 different customers, 1 attempt each — all should succeed independently.
        int customers = 5;
        ExecutorService pool = Executors.newFixedThreadPool(customers);
        List<Callable<HttpStatus>> jobs = new ArrayList<>();
        for (int i = 0; i < customers; i++) {
            int idx = i;
            jobs.add(() -> postLoad("c-" + idx, "load-" + idx, "$100.00", "2018-01-01T00:00:00Z"));
        }

        List<Future<HttpStatus>> futures = pool.invokeAll(jobs);
        pool.shutdown();

        for (Future<HttpStatus> f : futures) {
            assertThat(f.get()).isEqualTo(HttpStatus.OK);
        }

        Long acceptedCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM load_attempt_snapshot WHERE accepted = TRUE",
                Long.class);
        assertThat(acceptedCount).isEqualTo((long) customers);
    }

    private HttpStatus postLoad(String customerId, String loadId, String amount, String time) {
        var body = """
                {"id":"%s","customer_id":"%s","load_amount":"%s","time":"%s"}"""
                .formatted(loadId, customerId, amount, time);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.exchange(
                "/api/v1/loads", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        return HttpStatus.valueOf(resp.getStatusCode().value());
    }
}
