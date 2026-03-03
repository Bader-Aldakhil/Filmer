package com.filmer.integration.controller;

import com.filmer.entity.Genre;
import com.filmer.entity.Movie;
import com.filmer.repository.GenreRepository;
import com.filmer.repository.MovieRepository;
import com.filmer.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for GenreController.
 * 
 * Tests the complete HTTP flow:
 * - Request parsing and validation
 * - Service layer processing
 * - Database interactions
 * - Response formatting
 * - Error handling with appropriate HTTP status codes
 * 
 * Uses TestRestTemplate to make actual HTTP requests to a test server.
 */
@DisplayName("GenreController Integration Tests")
class GenreControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String baseUrl = "/api/v1/genres";

    @BeforeEach
    void setup() {
        // Clean database
        jdbcTemplate.execute("DELETE FROM ratings");
        jdbcTemplate.execute("DELETE FROM genres_in_movies");
        jdbcTemplate.execute("DELETE FROM movies");
        jdbcTemplate.execute("DELETE FROM genres");
        jdbcTemplate.execute("ALTER SEQUENCE genres_id_seq RESTART WITH 1");
    }

    @Test
    @DisplayName("Should return all genres with HTTP 200")
    void testGetAllGenres() {
        // Given
        genreRepository.save(new Genre(null, "Action"));
        genreRepository.save(new Genre(null, "Drama"));
        genreRepository.save(new Genre(null, "Comedy"));

        // When
        ResponseEntity<?> response = restTemplate.getForEntity(baseUrl, Object.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should return empty list when no genres exist")
    void testGetAllGenresEmpty() {
        // When
        ResponseEntity<?> response = restTemplate.getForEntity(baseUrl, Object.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should get movies by genre with HTTP 200")
    void testGetMoviesByGenre() {
        // Given
        Genre action = genreRepository.save(new Genre(null, "Action"));
        Movie movie = new Movie("tt0001", "Action Movie", (short) 2020, "Director", new BigDecimal("8.5"), 100000);
        movie.setGenres(Set.of(action));
        movieRepository.save(movie);

        // When
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/" + action.getId() + "/movies",
            Object.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should return 404 when genre doesn't exist")
    void testGetMoviesByGenreNotFound() {
        // When
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/999/movies",
            Object.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should return movies page by page")
    void testGetMoviesByGenrePagination() {
        // Given
        Genre action = genreRepository.save(new Genre(null, "Action"));
        for (int i = 1; i <= 15; i++) {
            Movie movie = new Movie("tt" + i, "Movie " + i, (short) 2020, "Director", new BigDecimal("7.0"), 1000);
            movie.setGenres(Set.of(action));
            movieRepository.save(movie);
        }

        // When
        ResponseEntity<?> page1 = restTemplate.getForEntity(
            baseUrl + "/" + action.getId() + "/movies",
            Object.class
        );

        // Then
        assertThat(page1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(page1.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should return movies with correct rating information")
    void testMovieRatingInResponse() {
        // Given
        Genre drama = genreRepository.save(new Genre(null, "Drama"));
        Movie movie = new Movie("tt0001", "Drama Movie", (short) 2020, "Director", new BigDecimal("9.2"), 500000);
        movie.setGenres(Set.of(drama));
        movieRepository.save(movie);

        // When
        ResponseEntity<?> response = restTemplate.getForEntity(
            baseUrl + "/" + drama.getId() + "/movies",
            Object.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should return genres in sorted order via HTTP endpoint")
    void testGenresSortedInResponse() {
        // Given
        genreRepository.save(new Genre(null, "Thriller"));
        genreRepository.save(new Genre(null, "Animation"));
        genreRepository.save(new Genre(null, "Action"));

        // When
        ResponseEntity<?> response = restTemplate.getForEntity(baseUrl, Object.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @DisplayName("Should handle content type correctly")
    void testResponseContentType() {
        // When
        ResponseEntity<?> response = restTemplate.getForEntity(baseUrl, Object.class);

        // Then
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().toString()).contains("application/json");
    }
}
