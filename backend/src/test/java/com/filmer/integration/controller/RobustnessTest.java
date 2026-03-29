package com.filmer.integration.controller;

import com.filmer.entity.Genre;
import com.filmer.repository.GenreRepository;
import com.filmer.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("Robustness / Failure Simulation Tests")
class RobustnessTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private GenreRepository genreRepositoryMock;

    private String baseUrl = "/api/v1/genres";

    @Test
    @DisplayName("Should return 500 Internal Server Error when DB fails gracefully")
    void testDatabaseFailureSimulation() {
        // Given: Simulate a dreaded database failure
        when(genreRepositoryMock.findAll()).thenThrow(new DataAccessException("Simulated DB connection failure") {});

        // When
        ResponseEntity<?> response = restTemplate.getForEntity(baseUrl, Object.class);

        // Then: Should not crash, but return a graceful 500 error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @Test
    @DisplayName("Should return 500 Internal Server Error when DB fetch by ID fails gracefully")
    void testDatabaseFailureSimulationGetMovies() {
        // Given: Simulate a dreaded database failure
        when(genreRepositoryMock.findById(any())).thenThrow(new DataAccessException("Simulated DB connection failure") {});

        // When
        ResponseEntity<?> response = restTemplate.getForEntity(baseUrl + "/1/movies", Object.class);

        // Then: Should not crash, but return a graceful 500 error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
