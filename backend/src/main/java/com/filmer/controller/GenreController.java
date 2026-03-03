package com.filmer.controller;

import com.filmer.dto.response.ApiErrorResponse;
import com.filmer.dto.response.ApiResponse;
import com.filmer.dto.response.GenreListResponse;
import com.filmer.dto.response.GenreMoviesResponse;
import com.filmer.dto.response.GenreResponse;
import com.filmer.service.GenreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * Controller for genre-related endpoints.
 * Handles genre listing and browsing movies by genre.
 */
@RestController
@RequestMapping("/api/v1/genres")
public class GenreController {

    private final GenreService genreService;

    @Autowired
    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    /**
     * Get all available genres.
     *
     * <p>Returns a list of all movie genres in the system.
     * This endpoint does not require pagination as the genre list is typically small.</p>
     *
     * @return ResponseEntity containing list of all genres
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Genres retrieved successfully</li>
     * </ul>
     */
    @GetMapping
    public ResponseEntity<ApiResponse<GenreListResponse>> listGenres() {
        GenreListResponse response = genreService.getAllGenres();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get movies filtered by a specific genre.
     *
     * <p>Returns a paginated list of movies belonging to the specified genre.
     * Supports sorting by title, year, or rating.</p>
     *
     * @param genreId The unique identifier of the genre
     * @param page    Page number (1-indexed), defaults to 1
     * @param size    Number of items per page, defaults to 20, max 100
     * @param sortBy  Field to sort by: title, year, or rating. Defaults to title
     * @param order   Sort order: asc or desc. Defaults to asc
     * @return ResponseEntity containing genre info and paginated movie list
     *
     * <p><b>Path Parameters:</b></p>
     * <ul>
     *   <li>genreId (required) - Genre ID, min 1</li>
     * </ul>
     *
     * <p><b>Query Parameters:</b></p>
     * <ul>
     *   <li>page (optional) - Page number, min 1, default 1</li>
     *   <li>size (optional) - Items per page, min 1, max 100, default 20</li>
     *   <li>sortBy (optional) - Sort field: title|year|rating, default title</li>
     *   <li>order (optional) - Sort order: asc|desc, default asc</li>
     * </ul>
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Movies retrieved successfully</li>
     *   <li>404 Not Found - Genre not found with given ID</li>
     * </ul>
     */
    @GetMapping("/{genreId}/movies")
    public ResponseEntity<?> getMoviesByGenre(
            @PathVariable Long genreId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {
        try {
            GenreMoviesResponse response = genreService.getMoviesByGenre(genreId, page, size, sortBy, order);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(ApiErrorResponse.of("NOT_FOUND", e.getMessage()));
        }
    }
}
