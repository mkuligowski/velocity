package com.mkuligowski.velocity.loads;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The headline test: replay {@code input.txt} through the running service and assert the
 * stream of accepted/declined responses equals {@code output.txt} byte-for-byte. This is
 * the take-home spec's fitness function.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class LoadEndToEndIntegrationTest {

    @Autowired private TestRestTemplate rest;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void resetTransactionalTables() {
        // Wipe attempt tables but preserve velocity_policy bootstrap rows.
        jdbc.execute("DELETE FROM load_attempt_event");
        jdbc.execute("DELETE FROM load_attempt_snapshot");
        jdbc.execute("DELETE FROM load_attempt_idempotency");
        jdbc.execute("DELETE FROM customer_lock");
    }

    @Test
    void replays_input_lines_and_matches_expected_output() throws IOException {
        List<String> inputLines = readClasspathLines("/loads/input.txt");
        List<String> expectedOutputs = readClasspathLines("/loads/output.txt");

        List<String> actualOutputs = new ArrayList<>(expectedOutputs.size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        for (int i = 0; i < inputLines.size(); i++) {
            String body = inputLines.get(i);
            ResponseEntity<String> response = rest.exchange(
                    "/api/v1/loads",
                    org.springframework.http.HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class);

            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            switch (status) {
                case OK -> actualOutputs.add(response.getBody());
                case NO_CONTENT -> { /* duplicate — silently skipped per spec */ }
                default -> throw new AssertionError(
                        "Unexpected status %s on input line %d: %s".formatted(
                                status, i + 1, body));
            }
        }

        assertThat(actualOutputs)
                .as("response stream length must match expected output line count")
                .hasSameSizeAs(expectedOutputs);

        for (int i = 0; i < expectedOutputs.size(); i++) {
            assertThat(actualOutputs.get(i))
                    .as("output line %d", i + 1)
                    .isEqualTo(expectedOutputs.get(i));
        }
    }

    private List<String> readClasspathLines(String resource) throws IOException {
        InputStream stream = getClass().getResourceAsStream(resource);
        if (stream == null) {
            throw new IllegalStateException("Classpath resource not found: " + resource);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        }
    }
}
