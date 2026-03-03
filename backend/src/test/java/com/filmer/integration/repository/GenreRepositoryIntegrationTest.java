package com.filmer.integration.repository;

import com.filmer.entity.Genre;
import com.filmer.repository.GenreRepository;
import com.filmer.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for GenreRepository.
 * 
 * Tests real database interactions with PostgreSQL container:
 * - No mocks or stubs
 * - Real JPA/Hibernate behavior
 * - Full CRUD operations
 * - Custom query methods
 */
@DisplayName("GenreRepository Integration Tests")
class GenreRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setup() {
        // Clear and reset test data before each test
        jdbcTemplate.execute("DELETE FROM genres_in_movies");
        jdbcTemplate.execute("DELETE FROM genres");
        jdbcTemplate.execute("ALTER SEQUENCE genres_id_seq RESTART WITH 1");
    }

    @Test
    @DisplayName("Should save a new genre and persist to database")
    void testSaveGenre() {
        // Given
        Genre genre = new Genre();
        genre.setName("Action");

        // When
        Genre savedGenre = genreRepository.save(genre);

        // Then
        assertThat(savedGenre).isNotNull();
        assertThat(savedGenre.getId()).isNotNull().isGreaterThan(0);
        assertThat(savedGenre.getName()).isEqualTo("Action");

        // Verify in database
        Optional<Genre> foundGenre = genreRepository.findById(savedGenre.getId());
        assertThat(foundGenre).isPresent();
        assertThat(foundGenre.get().getName()).isEqualTo("Action");
    }

    @Test
    @DisplayName("Should find genre by name (case-insensitive)")
    void testFindByNameIgnoreCase() {
        // Given
        Genre genre = new Genre();
        genre.setName("Drama");
        genreRepository.save(genre);

        // When - search with different cases
        Optional<Genre> foundLowercase = genreRepository.findByNameIgnoreCase("drama");
        Optional<Genre> foundUppercase = genreRepository.findByNameIgnoreCase("DRAMA");
        Optional<Genre> foundMixedCase = genreRepository.findByNameIgnoreCase("DrAmA");

        // Then
        assertThat(foundLowercase).isPresent();
        assertThat(foundUppercase).isPresent();
        assertThat(foundMixedCase).isPresent();
        assertThat(foundLowercase.get().getName()).isEqualTo("Drama");
    }

    @Test
    @DisplayName("Should return empty Optional when genre not found")
    void testFindByNameNotFound() {
        // When
        Optional<Genre> found = genreRepository.findByNameIgnoreCase("NonExistentGenre");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should return all genres")
    void testFindAll() {
        // Given - insert multiple genres
        Genre action = new Genre();
        action.setName("Action");
        Genre comedy = new Genre();
        comedy.setName("Comedy");
        Genre drama = new Genre();
        drama.setName("Drama");

        genreRepository.saveAll(List.of(action, comedy, drama));

        // When
        List<Genre> allGenres = genreRepository.findAll();

        // Then
        assertThat(allGenres).hasSize(3);
        assertThat(allGenres).extracting("name").containsExactly("Action", "Comedy", "Drama");
    }

    @Test
    @DisplayName("Should update an existing genre")
    void testUpdateGenre() {
        // Given
        Genre genre = new Genre();
        genre.setName("Original Name");
        Genre savedGenre = genreRepository.save(genre);

        // When
        savedGenre.setName("Updated Name");
        Genre updatedGenre = genreRepository.save(savedGenre);

        // Then
        assertThat(updatedGenre.getName()).isEqualTo("Updated Name");

        // Verify in database
        Optional<Genre> foundGenre = genreRepository.findById(savedGenre.getId());
        assertThat(foundGenre.get().getName()).isEqualTo("Updated Name");
    }

    @Test
    @DisplayName("Should delete a genre")
    void testDeleteGenre() {
        // Given
        Genre genre = new Genre();
        genre.setName("To Be Deleted");
        Genre savedGenre = genreRepository.save(genre);
        Long genreId = savedGenre.getId();

        // When
        genreRepository.deleteById(genreId);

        // Then
        Optional<Genre> foundGenre = genreRepository.findById(genreId);
        assertThat(foundGenre).isEmpty();
    }

    @Test
    @DisplayName("Should enforce unique constraint on genre name")
    void testUniqueConstraintOnName() {
        // Given
        Genre genre1 = new Genre();
        genre1.setName("Unique");
        genreRepository.save(genre1);

        // When - try to save duplicate
        Genre genre2 = new Genre();
        genre2.setName("Unique");

        // Then - should throw exception
        assertThatThrownBy(() -> genreRepository.save(genre2))
            .isNotNull();
    }

    @Test
    @DisplayName("Should return correct count of genres")
    void testCount() {
        // Given
        genreRepository.save(new Genre(null, "Genre1"));
        genreRepository.save(new Genre(null, "Genre2"));
        genreRepository.save(new Genre(null, "Genre3"));

        // When
        long count = genreRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should verify existence of genre by id")
    void testExistsById() {
        // Given
        Genre genre = new Genre();
        genre.setName("Existing");
        Genre savedGenre = genreRepository.save(genre);

        // When
        boolean exists = genreRepository.existsById(savedGenre.getId());
        boolean notExists = genreRepository.existsById(999L);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
