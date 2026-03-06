package com.filmer.service;

import com.filmer.dto.response.MovieDetailResponse;
import com.filmer.dto.response.MovieListItemResponse;
import com.filmer.dto.response.PaginatedResponse;
import com.filmer.entity.Genre;
import com.filmer.entity.Movie;
import com.filmer.repository.MovieRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MovieService.
 * Tests business logic for movie operations with mocked repository layer.
 * 
 * Phase 3: These tests define expected behavior based on API specification.
 * Tests may fail until Phase 4 implementation is complete.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MovieService Unit Tests")
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieService movieService;

    private Movie testMovie1;
    private Movie testMovie2;
    private Genre actionGenre;

    @BeforeEach
    void setUp() {
        // Set up test genres
        actionGenre = new Genre(1L, "Action");

        // Set up test movies
        testMovie1 = new Movie();
        testMovie1.setId("tt0000001");
        testMovie1.setTitle("The Shawshank Redemption");
        testMovie1.setYear((short) 1994);
        testMovie1.setDirector("Frank Darabont");
        testMovie1.setRating(new BigDecimal("9.3"));
        testMovie1.setNumVotes(2500000);
        testMovie1.setGenres(new HashSet<>(Collections.singletonList(actionGenre)));

        testMovie2 = new Movie();
        testMovie2.setId("tt0000002");
        testMovie2.setTitle("The Godfather");
        testMovie2.setYear((short) 1972);
        testMovie2.setDirector("Francis Ford Coppola");
        testMovie2.setRating(new BigDecimal("9.2"));
        testMovie2.setNumVotes(1800000);
        testMovie2.setGenres(new HashSet<>());
    }

    @Nested
    @DisplayName("getMovies()")
    class GetMoviesTests {

        @Test
        @DisplayName("Should return paginated movies with default parameters - Success Case")
        void getMovies_Success() {
            // Given
            List<Movie> movies = Arrays.asList(testMovie1, testMovie2);
            Page<Movie> moviePage = new PageImpl<>(movies, Pageable.unpaged(), 2);
            when(movieRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(moviePage);

            // When
            PaginatedResponse<MovieListItemResponse> response = movieService.getMovies(1, 20, "title", "asc", null,
                    null, null, null, null);

            // Then
            assertNotNull(response);
            assertEquals(2, response.getItems().size());
            assertEquals(1, response.getPage());
            assertEquals(20, response.getSize());
            assertEquals(2, response.getTotalItems());

            // Verify first movie details
            MovieListItemResponse firstMovie = response.getItems().get(0);
            assertEquals("tt0000001", firstMovie.getId());
            assertEquals("The Shawshank Redemption", firstMovie.getTitle());
            assertEquals((short) 1994, firstMovie.getYear());
            assertEquals("Frank Darabont", firstMovie.getDirector());

            verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty list when no movies exist")
        void getMovies_EmptyList() {
            // Given
            Page<Movie> emptyPage = new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0);
            when(movieRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

            // When
            PaginatedResponse<MovieListItemResponse> response = movieService.getMovies(1, 20, "title", "asc", null,
                    null, null, null, null);

            // Then
            assertNotNull(response);
            assertTrue(response.getItems().isEmpty());
            assertEquals(0, response.getTotalItems());
        }

        @Test
        @DisplayName("Should filter movies by starting character")
        void getMovies_FilterByStartsWith() {
            // Given
            List<Movie> movies = Collections.singletonList(testMovie1);
            Page<Movie> moviePage = new PageImpl<>(movies, Pageable.unpaged(), 1);
            when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(moviePage);

            // When
            PaginatedResponse<MovieListItemResponse> response = movieService.getMovies(1, 20, "title", "asc", "T",
                    null, null, null, null);

            // Then
            assertNotNull(response);
            assertEquals(1, response.getItems().size());
            assertTrue(response.getItems().get(0).getTitle().startsWith("The"));

            verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
            verify(movieRepository, never()).findByTitleStartingWithIgnoreCase(anyString(), any(Pageable.class));
        }

        @Test
        @DisplayName("Should filter movies starting with non-alpha character using *")
        void getMovies_FilterByNonAlpha() {
            // Given
            Movie numericMovie = new Movie();
            numericMovie.setId("tt0000003");
            numericMovie.setTitle("2001: A Space Odyssey");
            numericMovie.setYear((short) 1968);
            numericMovie.setGenres(new HashSet<>());

            Page<Movie> moviePage = new PageImpl<>(
                    Collections.singletonList(numericMovie),
                    Pageable.unpaged(),
                    1);
            when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(moviePage);

            // When
            PaginatedResponse<MovieListItemResponse> response = movieService.getMovies(1, 20, "title", "asc", "*",
                    null, null, null, null);

            // Then
            assertNotNull(response);
            assertEquals(1, response.getItems().size());
            assertEquals("2001: A Space Odyssey", response.getItems().get(0).getTitle());

            verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should validate pagination parameters - size capped at 100")
        void getMovies_ValidatePaginationSize() {
            // Given
            Page<Movie> moviePage = new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0);
            when(movieRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(moviePage);

            // When - request size > 100
            PaginatedResponse<MovieListItemResponse> response = movieService.getMovies(1, 500, "title", "asc", null,
                    null, null, null, null);

            // Then - size should be capped at 100
            assertEquals(100, response.getSize());
        }

        @Test
        @DisplayName("Should validate pagination parameters - page minimum is 1")
        void getMovies_ValidatePaginationPage() {
            // Given
            Page<Movie> moviePage = new PageImpl<>(Collections.emptyList(), Pageable.unpaged(), 0);
            when(movieRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(moviePage);

            // When - request page < 1
            PaginatedResponse<MovieListItemResponse> response = movieService.getMovies(0, 20, "title", "asc", null,
                    null, null, null, null);

            // Then - page should be minimum 1
            assertEquals(1, response.getPage());
        }

        @Test
        @DisplayName("Should propagate exception when repository throws - Simulated DB Failure")
        void getMovies_RepositoryException() {
            // Given
            when(movieRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When/Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> movieService.getMovies(1, 20, "title", "asc", null, null, null, null, null));
            
            assertEquals("Database connection failed", exception.getMessage());
        }

        @Test
        @DisplayName("Should handle descending sort order")
        void getMovies_SortDescending() {
            // Given
            Page<Movie> moviePage = new PageImpl<>(
                    Arrays.asList(testMovie1, testMovie2),
                    Pageable.unpaged(),
                    2);
            when(movieRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(moviePage);

            // When
            PaginatedResponse<MovieListItemResponse> response = movieService.getMovies(1, 20, "rating", "desc", null,
                    null, null, null, null);

            // Then
            assertNotNull(response);
            assertEquals(2, response.getItems().size());
            verify(movieRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getMovieById()")
    class GetMovieByIdTests {

        @Test
        @DisplayName("Should return movie details when found - Success Case")
        void getMovieById_Success() {
            // Given
            when(movieRepository.findByIdWithGenres("tt0000001")).thenReturn(Optional.of(testMovie1));

            // When
            Optional<MovieDetailResponse> result = movieService.getMovieById("tt0000001");

            // Then
            assertTrue(result.isPresent());
            MovieDetailResponse movieDetail = result.get();
            assertEquals("tt0000001", movieDetail.getId());
            assertEquals("The Shawshank Redemption", movieDetail.getTitle());
            assertEquals((short) 1994, movieDetail.getYear());
            assertEquals("Frank Darabont", movieDetail.getDirector());
            assertEquals(new BigDecimal("9.3"), movieDetail.getRating());
            assertEquals(2500000, movieDetail.getNumVotes());
            
            verify(movieRepository, times(1)).findByIdWithGenres("tt0000001");
        }

        @Test
        @DisplayName("Should return empty when movie not found - Not Found Case")
        void getMovieById_NotFound() {
            // Given
            when(movieRepository.findByIdWithGenres("tt9999999")).thenReturn(Optional.empty());

            // When
            Optional<MovieDetailResponse> result = movieService.getMovieById("tt9999999");

            // Then
            assertFalse(result.isPresent());
            verify(movieRepository, times(1)).findByIdWithGenres("tt9999999");
        }

        @Test
        @DisplayName("Should propagate exception when repository throws - Simulated DB Failure")
        void getMovieById_RepositoryException() {
            // Given
            when(movieRepository.findByIdWithGenres("tt0000001"))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When/Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> movieService.getMovieById("tt0000001"));
            
            assertEquals("Database connection failed", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Should return movie entity when found")
        void findById_Success() {
            // Given
            when(movieRepository.findById("tt0000001")).thenReturn(Optional.of(testMovie1));

            // When
            Optional<Movie> result = movieService.findById("tt0000001");

            // Then
            assertTrue(result.isPresent());
            assertEquals("tt0000001", result.get().getId());
            verify(movieRepository, times(1)).findById("tt0000001");
        }

        @Test
        @DisplayName("Should return empty when movie not found")
        void findById_NotFound() {
            // Given
            when(movieRepository.findById("invalid")).thenReturn(Optional.empty());

            // When
            Optional<Movie> result = movieService.findById("invalid");

            // Then
            assertFalse(result.isPresent());
        }
    }
}
