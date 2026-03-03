package com.filmer.integration.service;

import com.filmer.dto.response.GenreListResponse;
import com.filmer.dto.response.GenreMoviesResponse;
import com.filmer.entity.Genre;
import com.filmer.entity.Movie;
import com.filmer.repository.GenreRepository;
import com.filmer.repository.MovieRepository;
import com.filmer.service.GenreService;
import com.filmer.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for GenreService.
 * 
 * Tests the full flow:
 * - Service layer logic
 * - Repository interactions with real database
 * - Data transformation and DTOs
 * - Error handling
 * 
 * No mocks - all interactions are with the real database.
 */
@DisplayName("GenreService Integration Tests")
class GenreServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private GenreService genreService;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    @DisplayName("Should retrieve all genres sorted by name")
    void testGetAllGenresSorted() {
        // Given
        genreRepository.save(new Genre(null, "Sci-Fi"));
        genreRepository.save(new Genre(null, "Action"));
        genreRepository.save(new Genre(null, "Drama"));

        // When
        GenreListResponse response = genreService.getAllGenres();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getItems()).hasSize(3);
        assertThat(response.getItems()).extracting("name").containsExactly("Action", "Drama", "Sci-Fi");
    }

    @Test
    @DisplayName("Should retrieve movies by genre with pagination")
    void testGetMoviesByGenre() {
        // Given
        Genre action = genreRepository.save(new Genre(null, "Action"));
        Genre drama = genreRepository.save(new Genre(null, "Drama"));

        Movie movie1 = new Movie("tt0001", "Action Movie 1", (short) 2020, "Director 1", new BigDecimal("8.5"), 100000);
        Movie movie2 = new Movie("tt0002", "Action Movie 2", (short) 2021, "Director 2", new BigDecimal("7.5"), 50000);
        Movie movie3 = new Movie("tt0003", "Drama Movie", (short) 2022, "Director 3", new BigDecimal("8.0"), 80000);

        movie1.setGenres(Set.of(action));
        movie2.setGenres(Set.of(action));
        movie3.setGenres(Set.of(drama));

        movieRepository.saveAll(List.of(movie1, movie2, movie3));

        // When
        GenreMoviesResponse response = genreService.getMoviesByGenre(action.getId(), 1, 10, "title", "asc");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getGenre().getName()).isEqualTo("Action");
        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems())
            .extracting("title")
            .contains("Action Movie 1", "Action Movie 2");
    }

    @Test
    @DisplayName("Should handle empty genre correctly")
    void testGetMoviesForEmptyGenre() {
        // Given
        Genre emptyGenre = genreRepository.save(new Genre(null, "Horror"));

        // When
        GenreMoviesResponse response = genreService.getMoviesByGenre(emptyGenre.getId(), 1, 10, "title", "asc");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getGenre().getName()).isEqualTo("Horror");
        assertThat(response.getItems()).isEmpty();
    }

    @Test
    @DisplayName("Should paginate movies within a genre")
    void testPaginateMoviesByGenre() {
        // Given
        Genre action = genreRepository.save(new Genre(null, "Action"));

        // Insert 15 action movies
        for (int i = 1; i <= 15; i++) {
            Movie movie = new Movie("tt" + i, "Action Movie " + i, (short) 2020, "Director", new BigDecimal("7.0"), 1000);
            movie.setGenres(Set.of(action));
            movieRepository.save(movie);
        }

        // When - get page 1
        GenreMoviesResponse page1 = genreService.getMoviesByGenre(action.getId(), 1, 5, "title", "asc");

        // Then
        assertThat(page1.getItems()).hasSize(5);
        assertThat(page1.getTotalItems()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should maintain data consistency across operations")
    void testDataConsistency() {
        // Given
        Genre genre = genreRepository.save(new Genre(null, "Thriller"));
        Movie movie = new Movie("tt0001", "Thriller Movie", (short) 2020, "Director", new BigDecimal("8.0"), 100000);
        movie.setGenres(Set.of(genre));
        movieRepository.save(movie);

        // When
        GenreMoviesResponse response = genreService.getMoviesByGenre(genre.getId(), 1, 10, "title", "asc");
        GenreListResponse allGenres = genreService.getAllGenres();

        // Then
        assertThat(response.getItems()).hasSize(1);
        assertThat(allGenres.getItems().stream().anyMatch(g -> g.getName().equals("Thriller"))).isTrue();
    }

    @Test
    @DisplayName("Should handle genre with multiple movies correctly")
    void testGenreWithMultipleMovies() {
        // Given
        Genre scifi = genreRepository.save(new Genre(null, "Sci-Fi"));

        Movie[] movies = new Movie[5];
        for (int i = 0; i < 5; i++) {
            movies[i] = new Movie("tt" + i, "SciFi Movie " + i, (short) (2020 + i), "Director", new BigDecimal(7.0 + i * 0.2), 1000 * (i + 1));
            movies[i].setGenres(Set.of(scifi));
        }
        movieRepository.saveAll(List.of(movies));

        // When
        GenreMoviesResponse response = genreService.getMoviesByGenre(scifi.getId(), 1, 10, "title", "asc");

        // Then
        assertThat(response.getItems()).hasSize(5);
        assertThat(response.getTotalItems()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should return genres in consistent order")
    void testGenreOrderConsistency() {
        // Given
        Genre[] genres = {
            new Genre(null, "Zombie"),
            new Genre(null, "Action"),
            new Genre(null, "Mystery"),
            new Genre(null, "Animation")
        };
        genreRepository.saveAll(List.of(genres));

        // When - call multiple times
        GenreListResponse response1 = genreService.getAllGenres();
        GenreListResponse response2 = genreService.getAllGenres();

        // Then - order should be consistent
        List<String> names1 = response1.getItems().stream().map(r -> r.getName()).toList();
        List<String> names2 = response2.getItems().stream().map(r -> r.getName()).toList();
        
        assertThat(names1).isEqualTo(names2);
        assertThat(names1).containsExactly("Action", "Animation", "Mystery", "Zombie");
    }
}
