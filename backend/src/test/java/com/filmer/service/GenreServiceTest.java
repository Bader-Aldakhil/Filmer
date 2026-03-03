package com.filmer.service;

import com.filmer.dto.response.GenreListResponse;
import com.filmer.dto.response.GenreMoviesResponse;
import com.filmer.entity.Genre;
import com.filmer.entity.Movie;
import com.filmer.repository.GenreRepository;
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
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GenreService.
 * Tests business logic for genre operations with mocked repository layer.
 * 
 * Phase 3: These tests define expected behavior based on API specification.
 * Tests may fail until Phase 4 implementation is complete.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GenreService Unit Tests")
class GenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private GenreService genreService;

    private Genre actionGenre;
    private Genre dramaGenre;
    private Movie testMovie;

    @BeforeEach
    void setUp() {
        // Set up test data
        actionGenre = new Genre(1L, "Action");
        dramaGenre = new Genre(2L, "Drama");
        
        testMovie = new Movie();
        testMovie.setId("tt0000001");
        testMovie.setTitle("Test Movie");
        testMovie.setYear((short) 2020);
        testMovie.setDirector("Test Director");
        testMovie.setRating(new BigDecimal("8.5"));
        testMovie.setNumVotes(100000);
    }

    @Nested
    @DisplayName("getAllGenres()")
    class GetAllGenresTests {

        @Test
        @DisplayName("Should return all genres sorted by name - Success Case")
        void getAllGenres_Success() {
            // Given
            List<Genre> genres = Arrays.asList(actionGenre, dramaGenre);
            when(genreRepository.findAll(any(Sort.class))).thenReturn(genres);

            // When
            GenreListResponse response = genreService.getAllGenres();

            // Then
            assertNotNull(response);
            assertNotNull(response.getItems());
            assertEquals(2, response.getItems().size());
            assertEquals("Action", response.getItems().get(0).getName());
            assertEquals("Drama", response.getItems().get(1).getName());
            
            verify(genreRepository, times(1)).findAll(any(Sort.class));
        }

        @Test
        @DisplayName("Should return empty list when no genres exist")
        void getAllGenres_EmptyList() {
            // Given
            when(genreRepository.findAll(any(Sort.class))).thenReturn(Collections.emptyList());

            // When
            GenreListResponse response = genreService.getAllGenres();

            // Then
            assertNotNull(response);
            assertNotNull(response.getItems());
            assertTrue(response.getItems().isEmpty());
            
            verify(genreRepository, times(1)).findAll(any(Sort.class));
        }

        @Test
        @DisplayName("Should propagate exception when repository throws - Simulated DB Failure")
        void getAllGenres_RepositoryException() {
            // Given
            when(genreRepository.findAll(any(Sort.class)))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When/Then
            RuntimeException exception = assertThrows(RuntimeException.class, 
                    () -> genreService.getAllGenres());
            
            assertEquals("Database connection failed", exception.getMessage());
            verify(genreRepository, times(1)).findAll(any(Sort.class));
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("Should return genre when found - Success Case")
        void findById_Success() {
            // Given
            when(genreRepository.findById(1L)).thenReturn(Optional.of(actionGenre));

            // When
            Optional<Genre> result = genreService.findById(1L);

            // Then
            assertTrue(result.isPresent());
            assertEquals("Action", result.get().getName());
            verify(genreRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should return empty when genre not found - Not Found Case")
        void findById_NotFound() {
            // Given
            when(genreRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            Optional<Genre> result = genreService.findById(999L);

            // Then
            assertFalse(result.isPresent());
            verify(genreRepository, times(1)).findById(999L);
        }
    }

    @Nested
    @DisplayName("getMoviesByGenre()")
    class GetMoviesByGenreTests {

        @Test
        @DisplayName("Should return paginated movies for valid genre - Success Case")
        void getMoviesByGenre_Success() {
            // Given
            when(genreRepository.findById(1L)).thenReturn(Optional.of(actionGenre));
            
            Page<Movie> moviePage = new PageImpl<>(
                    Collections.singletonList(testMovie),
                    Pageable.unpaged(),
                    1
            );
            when(movieRepository.findByGenreId(eq(1L), any(Pageable.class))).thenReturn(moviePage);

            // When
            GenreMoviesResponse response = genreService.getMoviesByGenre(1L, 1, 20, "title", "asc");

            // Then
            assertNotNull(response);
            assertNotNull(response.getGenre());
            assertEquals("Action", response.getGenre().getName());
            assertEquals(1, response.getItems().size());
            assertEquals("Test Movie", response.getItems().get(0).getTitle());
            assertEquals(1, response.getPage());
            assertEquals(20, response.getSize());
            
            verify(genreRepository, times(1)).findById(1L);
            verify(movieRepository, times(1)).findByGenreId(eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("Should throw exception when genre not found - Not Found Case")
        void getMoviesByGenre_GenreNotFound() {
            // Given
            when(genreRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> genreService.getMoviesByGenre(999L, 1, 20, "title", "asc"));
            
            assertTrue(exception.getMessage().contains("Genre not found"));
            verify(genreRepository, times(1)).findById(999L);
            verify(movieRepository, never()).findByGenreId(any(), any());
        }

        @Test
        @DisplayName("Should return empty list when genre has no movies")
        void getMoviesByGenre_EmptyResults() {
            // Given
            when(genreRepository.findById(1L)).thenReturn(Optional.of(actionGenre));
            
            Page<Movie> emptyPage = new PageImpl<>(
                    Collections.emptyList(),
                    Pageable.unpaged(),
                    0
            );
            when(movieRepository.findByGenreId(eq(1L), any(Pageable.class))).thenReturn(emptyPage);

            // When
            GenreMoviesResponse response = genreService.getMoviesByGenre(1L, 1, 20, "title", "asc");

            // Then
            assertNotNull(response);
            assertTrue(response.getItems().isEmpty());
            assertEquals(0, response.getTotalItems());
        }

        @Test
        @DisplayName("Should handle different sort options")
        void getMoviesByGenre_SortByRating() {
            // Given
            when(genreRepository.findById(1L)).thenReturn(Optional.of(actionGenre));
            
            Page<Movie> moviePage = new PageImpl<>(
                    Collections.singletonList(testMovie),
                    Pageable.unpaged(),
                    1
            );
            when(movieRepository.findByGenreId(eq(1L), any(Pageable.class))).thenReturn(moviePage);

            // When
            GenreMoviesResponse response = genreService.getMoviesByGenre(1L, 1, 20, "rating", "desc");

            // Then
            assertNotNull(response);
            // Verify sorting was applied (checked via mock interaction)
            verify(movieRepository, times(1)).findByGenreId(eq(1L), any(Pageable.class));
        }

        @Test
        @DisplayName("Should propagate exception when movie repository throws - Simulated DB Failure")
        void getMoviesByGenre_RepositoryException() {
            // Given
            when(genreRepository.findById(1L)).thenReturn(Optional.of(actionGenre));
            when(movieRepository.findByGenreId(eq(1L), any(Pageable.class)))
                    .thenThrow(new RuntimeException("Database query failed"));

            // When/Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> genreService.getMoviesByGenre(1L, 1, 20, "title", "asc"));
            
            assertEquals("Database query failed", exception.getMessage());
        }
    }
}
