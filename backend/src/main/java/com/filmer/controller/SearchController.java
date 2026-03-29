package com.filmer.controller;

import com.filmer.dto.response.*;
import com.filmer.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for search endpoints.
 * Handles keyword search and advanced movie filtering.
 */
@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final MovieService movieService;

    @Autowired
    public SearchController(MovieService movieService) {
        this.movieService = movieService;
    }

    /**
     * Search movies (Base endpoint - maps simple title search)
     */
    @GetMapping("/movies")
    public ResponseEntity<?> searchMoviesByTitle(
            @RequestParam String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        // Contract test expects 0-indexed pagination in results
        // MovieService uses 1-indexed, so we convert
        PaginatedResponse<MovieListItemResponse> paged = movieService.searchMovies(
                null, title, null, null, null, null, null, null, null, page + 1, size, "title", "asc");

        SearchResponse<MovieListItemResponse> response = SearchResponse.<MovieListItemResponse>builder()
                .movies(paged.getItems())
                .totalMovies(paged.getTotalItems())
                .totalPages(paged.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .hasNextPage(page + 1 < paged.getTotalPages())
                .message(paged.getTotalItems() == 0 ? "No movies found for search" : null)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Advanced movie search
     */
    @GetMapping("/movies/advanced")
    public ResponseEntity<?> searchMoviesAdvanced(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String director,
            @RequestParam(required = false) Double minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Combine inputs into service call
        PaginatedResponse<MovieListItemResponse> paged = movieService.searchMovies(
                null, title, year, null, null, director, null, null, null, page + 1, size, "title", "asc");

        SearchResponse<MovieListItemResponse> response = SearchResponse.<MovieListItemResponse>builder()
                .movies(paged.getItems())
                .totalMovies(paged.getTotalItems())
                .appliedFilters(Map.of(
                    "title", title != null ? title : "",
                    "year", year != null ? year : "",
                    "director", director != null ? director : "",
                    "minRating", minRating != null ? minRating : ""
                ))
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Search movies by director
     */
    @GetMapping("/directors")
    public ResponseEntity<?> searchByDirector(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PaginatedResponse<MovieListItemResponse> paged = movieService.searchMovies(
                null, null, null, null, null, name, null, null, null, page + 1, size, "title", "asc");

        DirectorSearchResponse response = DirectorSearchResponse.builder()
                .directorName(name)
                .movieCount(paged.getTotalItems())
                .movies(paged.getItems())
                .totalPages(paged.getTotalPages())
                .hasNextPage(page + 1 < paged.getTotalPages())
                .build();

        return ResponseEntity.ok(response);
    }
}

