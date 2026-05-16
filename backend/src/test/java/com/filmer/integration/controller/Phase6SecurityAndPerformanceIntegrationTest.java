package com.filmer.integration.controller;

import com.filmer.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Phase 6 Security and Performance Integration Tests")
class Phase6SecurityAndPerformanceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Unauthenticated user cannot access protected APIs")
    void protectedApisRequireAuthentication() {
        ResponseEntity<String> cart = restTemplate.getForEntity("/api/v1/cart", String.class);
        ResponseEntity<String> orders = restTemplate.getForEntity("/api/v1/orders", String.class);
        ResponseEntity<String> library = restTemplate.getForEntity("/api/v1/library", String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> checkoutPayload = new HttpEntity<>(
                Map.of(
                        "creditCardId", "4111111111111111",
                        "firstName", "John",
                        "lastName", "Doe",
                        "expiration", "2028-12"),
                headers);
        ResponseEntity<String> checkout = restTemplate.exchange(
                "/api/v1/checkout",
                HttpMethod.POST,
                checkoutPayload,
                String.class);

        assertThat(cart.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(orders.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(library.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(checkout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Movies listing endpoint responds within an acceptable budget")
    void moviesEndpointPerformanceSmokeTest() {
        restTemplate.getForEntity("/api/v1/movies?page=1&size=20", String.class);

        long start = System.nanoTime();
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/movies?page=1&size=20", String.class);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(elapsedMs).isLessThan(500);
    }
}
