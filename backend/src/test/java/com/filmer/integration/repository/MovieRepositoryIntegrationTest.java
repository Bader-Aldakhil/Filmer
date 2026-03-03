package com.filmer.integration.repository;

import com.filmer.entity.Genre;
import com.filmer.entity.Movie;
import com.filmer.repository.GenreRepository;
import com.filmer.repository.MovieRepository;
import com.filmer.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for MovieRepository.
 * 
 * Tests real database interactions including:
 * - Full CRUD operations
 * - Complex queries with pagination
 * - Relationships with genres
 * - Sorting and filtering
 */
@DisplayName("MovieRepository Integration Tests")
class MovieRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        // Clear test data
        jdbcTemplate.execute("DELETE FROM ratings");
        jdbcTemplate.execute("DELETE FROM genres_in_movies");
        jdbcTemplate.execute("DELETE FROM movies");
        jdbcTemplate.execute("DELETE FROM genres");
        jdbcTemplate.execute("ALTER SEQUENCE genres_id_seq RESTART WITH 1");
    }

    @Test
    @DisplayName("Should save a new movie with ratings")
    void testSaveMovie() {
        // Given
        Movie movie = new Movie();
        movie.setId("tt0111161");
        movie.setTitle("The Shawshank Redemption");
        movie.setYear((short) 1994);
        movie.setDirector("Frank Darabont");
        movie.setRating(new BigDecimal("9.3"));
        movie.setNumVotes(2500000);

        // When
        Movie savedMovie = movieRepository.save(movie);

        // Then
        assertThat(savedMovie).isNotNull();
        assertThat(savedMovie.getId()).isEqualTo("tt0111161");
        assertThat(savedMovie.getTitle()).isEqualTo("The Shawshank Redemption");
        assertThat(savedMovie.getYear()).isEqualTo(1994);
        assertThat(savedMovie.getRating()).isEqualByComparingTo(new BigDecimal("9.3"));
    }

    @Test
    @DisplayName("Should find movie by id")
    void testFindMovieById() {
        // Given
        Movie movie = new Movie("tt1234567", "Test Movie", (short) 2020, "Test Director", new BigDecimal("8.0"), 100000);
        movieRepository.save(movie);

        // When
        Optional<Movie> found = movieRepository.findById("tt1234567");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Movie");
    }

    @Test
    @DisplayName("Should return empty when movie not found")
    void testFindMovieNotFound() {
        // When
        Optional<Movie> found = movieRepository.findById("nonexistent");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should search movies by title")
    void testSearchMoviesByTitle() {
        // Given - insert multiple movies
        movieRepository.save(new Movie("tt0001", "The Batman", (short) 2022, "Matt Reeves", new BigDecimal("8.0"), 500000));
        movieRepository.save(new Movie("tt0002", "Batman Begins", (short) 2005, "Christopher Nolan", new BigDecimal("8.3"), 800000));
        movieRepository.save(new Movie("tt0003", "Superman", (short) 1978, "Richard Donner", new BigDecimal("7.3"), 300000));

        // When
        Page<Movie> results = movieRepository.findByTitleStartingWithIgnoreCase("B", PageRequest.of(0, 10));

        // Then
        assertThat(results.getTotalElements()).isEqualTo(2);
        assertThat(results.getContent()).extracting("title").contains("The Batman", "Batman Begins");
    }

    @Test
    @DisplayName("Should search movies by genre with pagination")
    void testSearchMoviesByGenre() {
        // Given
        Genre action = genreRepository.save(new Genre(null, "Action"));
        Genre drama = genreRepository.save(new Genre(null, "Drama"));

        Movie movie1 = new Movie("tt0001", "Movie 1", (short) 2020, "Director 1", new BigDecimal("8.0"), 100000);
        Movie movie2 = new Movie("tt0002", "Movie 2", (short) 2021, "Director 2", new BigDecimal("7.5"), 50000);
        Movie movie3 = new Movie("tt0003", "Movie 3", (short) 2022, "Director 3", new BigDecimal("8.5"), 200000);

        movie1.setGenres(Set.of(action, drama));
        movie2.setGenres(Set.of(action));
        movie3.setGenres(Set.of(drama));

        movieRepository.saveAll(List.of(movie1, movie2, movie3));

        // When
        Page<Movie> actionMovies = movieRepository.findByGenreId(action.getId(), PageRequest.of(0, 10));

        // Then
        assertThat(actionMovies.getTotalElements()).isEqualTo(2);
        List<String> titles = actionMovies.getContent().stream().map(Movie::getTitle).toList();
        assertThat(titles).contains("Movie 1", "Movie 2");
    }

    @Test
    @DisplayName("Should sort movies by rating in descending order")
    void testSortMoviesByRating() {
        // Given
        movieRepository.save(new Movie("tt0001", "Low Rated", (short) 2020, "Dir1", new BigDecimal("5.0"), 1000));
        movieRepository.save(new Movie("tt0002", "Medium Rated", (short) 2021, "Dir2", new BigDecimal("7.5"), 5000));
        movieRepository.save(new Movie("tt0003", "High Rated", (short) 2022, "Dir3", new BigDecimal("9.0"), 10000));

        // When
        Page<Movie> sorted = movieRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "rating")));

        // Then
        assertThat(sorted.getContent())
            .extracting("rating")
            .containsExactly(
                new BigDecimal("9.0"),
                new BigDecimal("7.5"),
                new BigDecimal("5.0")
            );
    }

    @Test
    @DisplayName("Should update an existing movie")
    void testUpdateMovie() {
        // Given
        Movie movie = new Movie("tt0001", "Original Title", (short) 2020, "Director", new BigDecimal("8.0"), 100000);
        Movie savedMovie = movieRepository.save(movie);

        // When
        savedMovie.setTitle("Updated Title");
        savedMovie.setRating(new BigDecimal("9.0"));
        Movie updated = movieRepository.save(savedMovie);

        // Then
        assertThat(updated.getTitle()).isEqualTo("Updated Title");
        assertThat(updated.getRating()).isEqualByComparingTo(new BigDecimal("9.0"));
    }

    @Test
    @DisplayName("Should delete a movie")
    void testDeleteMovie() {
        // Given
        Movie movie = new Movie("tt0001", "To Delete", (short) 2020, "Director", new BigDecimal("8.0"), 100000);
        Movie savedMovie = movieRepository.save(movie);

        // When
        movieRepository.deleteById(savedMovie.getId());

        // Then
        Optional<Movie> found = movieRepository.findById("tt0001");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should find movies with pagination")
    void testMoviesPagination() {
        // Given - insert 15 movies
        for (int i = 1; i <= 15; i++) {
            movieRepository.save(new Movie("tt" + i, "Movie " + i, (short) 2020, "Director", new BigDecimal("7.0"), 1000));
        }

        // When
        Page<Movie> page1 = movieRepository.findAll(PageRequest.of(0, 5));
        Page<Movie> page2 = movieRepository.findAll(PageRequest.of(1, 5));
        Page<Movie> page3 = movieRepository.findAll(PageRequest.of(2, 5));

        // Then
        assertThat(page1.getTotalElements()).isEqualTo(15);
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page2.getContent()).hasSize(5);
        assertThat(page3.getContent()).hasSize(5);
        assertThat(page1.getTotalPages()).isEqualTo(3);
    }
}
