package com.mkuligowski.velocity.loads;

import static org.assertj.core.api.Assertions.assertThat;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class LoadIdempotencyIntegrationTest {

    private static final String LOAD_BODY = """
            {"id":"99","customer_id":"42","load_amount":"$10.00","time":"2018-01-01T00:00:00Z"}""";

    private static final String SAME_KEY_DIFFERENT_BODY = """
            {"id":"99","customer_id":"42","load_amount":"$50.00","time":"2018-01-01T00:00:00Z"}""";

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
    void first_attempt_returns_decision_subsequent_duplicates_return_204() {
        assertThat(post(LOAD_BODY).getStatusCode())
                .as("first attempt should be processed")
                .isEqualTo(HttpStatus.OK);

        assertThat(post(LOAD_BODY).getStatusCode())
                .as("identical retry of (customer_id, id) returns 204 with no body")
                .isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(post(SAME_KEY_DIFFERENT_BODY).getStatusCode())
                .as("same (customer_id, id) with different amount is still a duplicate")
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void only_one_event_and_snapshot_row_per_idempotency_key() {
        post(LOAD_BODY);
        post(LOAD_BODY);
        post(LOAD_BODY);

        Integer events = jdbc.queryForObject(
                "SELECT COUNT(*) FROM load_attempt_event WHERE customer_id = '42' AND load_id = '99'",
                Integer.class);
        Integer snapshots = jdbc.queryForObject(
                "SELECT COUNT(*) FROM load_attempt_snapshot WHERE customer_id = '42' AND load_id = '99'",
                Integer.class);
        Integer locks = jdbc.queryForObject(
                "SELECT COUNT(*) FROM load_attempt_idempotency WHERE customer_id = '42' AND load_id = '99'",
                Integer.class);

        assertThat(events).isEqualTo(1);
        assertThat(snapshots).isEqualTo(1);
        assertThat(locks).isEqualTo(1);
    }

    private ResponseEntity<String> post(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/v1/loads", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }
}
