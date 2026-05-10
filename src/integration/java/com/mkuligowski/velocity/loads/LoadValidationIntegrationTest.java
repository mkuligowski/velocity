package com.mkuligowski.velocity.loads;

import static org.assertj.core.api.Assertions.assertThat;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class LoadValidationIntegrationTest {

    @Autowired private TestRestTemplate rest;

    @Test
    void malformed_money_returns_400_with_validation_error() {
        var body = """
                {"id":"x","customer_id":"y","load_amount":"1000","time":"2018-01-01T00:00:00Z"}""";
        ResponseEntity<String> resp = post(body);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).contains("\"error\":\"VALIDATION_FAILED\"");
        assertThat(resp.getHeaders().getFirst("X-Correlation-Id")).isNotBlank();
    }

    @Test
    void blank_id_returns_400() {
        var body = """
                {"id":"","customer_id":"42","load_amount":"$1.00","time":"2018-01-01T00:00:00Z"}""";
        assertThat(post(body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void missing_required_field_returns_400() {
        var body = """
                {"id":"x","customer_id":"42","time":"2018-01-01T00:00:00Z"}""";
        assertThat(post(body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void malformed_json_returns_400() {
        var body = "not even json";
        assertThat(post(body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<String> post(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange("/api/v1/loads", HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }
}
