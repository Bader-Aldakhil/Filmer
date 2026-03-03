package com.filmer.controller;

import com.filmer.dto.response.GenreListResponse;
import com.filmer.dto.response.GenreMoviesResponse;
import com.filmer.dto.response.GenreResponse;
import com.filmer.service.GenreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.filmer.config.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for GenreController.
 * Tests HTTP layer behavior for genre endpoints using MockMvc.
 * 
 * Phase 3: These tests define expected API behavior based on specification.
 * Tests verify status codes and response shapes as per API contract.
 */
@WebMvcTest(GenreController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("GenreController Unit Tests")
class GenreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GenreService genreService;

    @Nested
    @DisplayName("GET /api/v1/genres")
    class ListGenresTests {

        @Test
        @DisplayName("Should return 200 OK with list of genres - Success Case")
        void listGenres_Success() throws Exception {
            // Given
            GenreListResponse response = new GenreListResponse(Arrays.asList(
                    new GenreResponse(1L, "Action"),
                    new GenreResponse(2L, "Comedy"),
                    new GenreResponse(3L, "Drama")
            ));
            when(genreService.getAllGenres()).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/genres")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.items", hasSize(3)))
                    .andExpect(jsonPath("$.data.items[0].id", is(1)))
                    .andExpect(jsonPath("$.data.items[0].name", is("Action")))
                    .andExpect(jsonPath("$.data.items[1].name", is("Comedy")))
                    .andExpect(jsonPath("$.data.items[2].name", is("Drama")));

            verify(genreService, times(1)).getAllGenres();
        }

        @Test
        @DisplayName("Should return 200 OK with empty list when no genres exist")
        void listGenres_EmptyList() throws Exception {
            // Given
            GenreListResponse response = new GenreListResponse(Collections.emptyList());
            when(genreService.getAllGenres()).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/genres")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.items", hasSize(0)));

            verify(genreService, times(1)).getAllGenres();
        }

        @Test
        @DisplayName("Should return 500 when service throws exception - Simulated DB Failure")
        void listGenres_ServiceException() throws Exception {
            // Given
            when(genreService.getAllGenres())
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When/Then
            mockMvc.perform(get("/api/v1/genres")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());

            verify(genreService, times(1)).getAllGenres();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/genres/{genreId}/movies")
    class GetMoviesByGenreTests {

        @Test
        @DisplayName("Should return 200 OK with paginated movies - Success Case")
        void getMoviesByGenre_Success() throws Exception {
            // Given
            GenreMoviesResponse response = createMockGenreMoviesResponse();
            when(genreService.getMoviesByGenre(1L, 1, 20, "title", "asc")).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/genres/1/movies")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.genre.id", is(1)))
                    .andExpect(jsonPath("$.data.genre.name", is("Action")))
                    .andExpect(jsonPath("$.data.items", hasSize(1)))
                    .andExpect(jsonPath("$.data.items[0].id", is("tt0000001")))
                    .andExpect(jsonPath("$.data.items[0].title", is("Die Hard")))
                    .andExpect(jsonPath("$.data.page", is(1)))
                    .andExpect(jsonPath("$.data.size", is(20)))
                    .andExpect(jsonPath("$.data.totalItems", is(1)))
                    .andExpect(jsonPath("$.data.totalPages", is(1)));

            verify(genreService, times(1)).getMoviesByGenre(1L, 1, 20, "title", "asc");
        }

        @Test
        @DisplayName("Should return 200 OK with custom pagination parameters")
        void getMoviesByGenre_CustomPagination() throws Exception {
            // Given
            GenreMoviesResponse response = createMockGenreMoviesResponse();
            response.setPage(2);
            response.setSize(10);
            when(genreService.getMoviesByGenre(1L, 2, 10, "rating", "desc")).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/genres/1/movies")
                            .param("page", "2")
                            .param("size", "10")
                            .param("sortBy", "rating")
                            .param("order", "desc")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page", is(2)))
                    .andExpect(jsonPath("$.data.size", is(10)));

            verify(genreService, times(1)).getMoviesByGenre(1L, 2, 10, "rating", "desc");
        }

        @Test
        @DisplayName("Should return 404 Not Found when genre does not exist - Not Found Case")
        void getMoviesByGenre_GenreNotFound() throws Exception {
            // Given
            when(genreService.getMoviesByGenre(999L, 1, 20, "title", "asc"))
                    .thenThrow(new IllegalArgumentException("Genre not found with id: 999"));

            // When/Then
            mockMvc.perform(get("/api/v1/genres/999/movies")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("NOT_FOUND")))
                    .andExpect(jsonPath("$.error.message", containsString("Genre not found")));

            verify(genreService, times(1)).getMoviesByGenre(999L, 1, 20, "title", "asc");
        }

        @Test
        @DisplayName("Should return 200 OK with empty list when genre has no movies")
        void getMoviesByGenre_EmptyResults() throws Exception {
            // Given
            GenreMoviesResponse response = new GenreMoviesResponse();
            response.setGenre(new GenreResponse(1L, "Action"));
            response.setItems(Collections.emptyList());
            response.setPage(1);
            response.setSize(20);
            response.setTotalItems(0);
            response.setTotalPages(0);
            when(genreService.getMoviesByGenre(1L, 1, 20, "title", "asc")).thenReturn(response);

            // When/Then
            mockMvc.perform(get("/api/v1/genres/1/movies")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalItems", is(0)));
        }

        @Test
        @DisplayName("Should return 500 when service throws unexpected exception - Simulated DB Failure")
        void getMoviesByGenre_ServiceException() throws Exception {
            // Given
            when(genreService.getMoviesByGenre(1L, 1, 20, "title", "asc"))
                    .thenThrow(new RuntimeException("Database query failed"));

            // When/Then
            mockMvc.perform(get("/api/v1/genres/1/movies")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());
        }

        /**
         * Helper method to create mock genre movies response.
         */
        private GenreMoviesResponse createMockGenreMoviesResponse() {
            GenreMoviesResponse response = new GenreMoviesResponse();
            response.setGenre(new GenreResponse(1L, "Action"));
            
            GenreMoviesResponse.GenreMovieItem movie = new GenreMoviesResponse.GenreMovieItem();
            movie.setId("tt0000001");
            movie.setTitle("Die Hard");
            movie.setYear((short) 1988);
            movie.setDirector("John McTiernan");
            movie.setRating(new BigDecimal("8.2"));
            
            response.setItems(Collections.singletonList(movie));
            response.setPage(1);
            response.setSize(20);
            response.setTotalItems(1);
            response.setTotalPages(1);
            
            return response;
        }
    }
}
