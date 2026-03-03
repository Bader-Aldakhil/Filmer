package com.filmer.integration.controller;

import com.filmer.entity.Genre;
import com.filmer.integration.BaseIntegrationTest;
import com.filmer.repository.GenreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Concurrency/Stress Tests")
class ConcurrencyTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String baseUrl = "/api/v1/genres";

    @BeforeEach
    void setup() {
        jdbcTemplate.execute("DELETE FROM ratings");
        jdbcTemplate.execute("DELETE FROM genres_in_movies");
        jdbcTemplate.execute("DELETE FROM movies");
        jdbcTemplate.execute("DELETE FROM genres");
        jdbcTemplate.execute("ALTER SEQUENCE genres_id_seq RESTART WITH 1");

        genreRepository.save(new Genre(null, "Action"));
        genreRepository.save(new Genre(null, "Drama"));
        genreRepository.save(new Genre(null, "Comedy"));
    }

    @Test
    @DisplayName("Should handle multiple concurrent requests without crashing")
    void testConcurrentRequests() throws InterruptedException {
        int numberOfThreads = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<ResponseEntity<Object>>> futures = new ArrayList<>();

        for (int i = 0; i < numberOfThreads; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    return restTemplate.getForEntity(baseUrl, Object.class);
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        for (Future<ResponseEntity<Object>> future : futures) {
            try {
                ResponseEntity<Object> response = future.get();
                if (response.getStatusCode() == HttpStatus.OK) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        }

        assertThat(successCount.get()).isEqualTo(numberOfThreads);
        assertThat(failureCount.get()).isEqualTo(0);
    }
}
