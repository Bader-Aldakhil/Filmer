package com.filmer.controller;

import com.filmer.dto.response.ApiResponse;
import com.filmer.dto.response.ApiErrorResponse;
import com.filmer.dto.response.MovieListItemResponse;
import com.filmer.dto.response.PaginatedResponse;
import com.filmer.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for movie-related endpoints.
 * Handles movie listing, pagination, sorting, filtering, and detail retrieval.
 */
@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {

    private final MovieService movieService;

    @Autowired
    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    /**
     * Get a paginated list of movies with optional sorting and filtering.
     *
     * <p>
     * Returns a paginated list of movies. Supports sorting by title, year, or
     * rating,
     * and filtering by starting character.
     * </p>
     *
     * @param page   Page number (1-indexed), defaults to 1
     * @param size   Number of items per page, defaults to 20, max 100
     * @param sortBy Field to sort by: title, year, or rating. Defaults to title
     * @param order  Sort order: asc or desc. Defaults to asc
     * @param title  Filter movies by title keyword search
     * @param type   Comma-separated list of title types (e.g., "movie" or
     * @return ResponseEntity containing paginated movie list
     *
     *         <p>
     *         <b>Query Parameters:</b>
     *         </p>
     *         <ul>
     *         <li>page (optional) - Page number, min 1, default 1</li>
     *         <li>size (optional) - Items per page, min 1, max 100, default 20</li>
     *         <li>sortBy (optional) - Sort field: title|year|rating, default
     *         title</li>
     *         <li>order (optional) - Sort order: asc|desc, default asc</li>
     *         <li>title (optional) - Filter by title keyword search</li>
     *         </ul>
     *
     *         <p>
     *         <b>Responses:</b>
     *         </p>
     *         <ul>
     *         <li>200 OK - Movies retrieved successfully</li>
     *         <li>400 Bad Request - Invalid query parameters</li>
     *         </ul>
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResponse<MovieListItemResponse>>> listMovies(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Integer minVotes) {

        PaginatedResponse<MovieListItemResponse> response = movieService.getMovies(page, size, sortBy, order,
                title, type, genreId, minRating, minVotes);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Get detailed information about a specific movie.
     *
     * <p>
     * Returns comprehensive movie details including all genres and stars
     * associated with the movie.
     * </p>
     *
     * @param movieId The unique identifier of the movie (max 10 chars)
     * @return ResponseEntity containing detailed movie information
     *
     *         <p>
     *         <b>Path Parameters:</b>
     *         </p>
     *         <ul>
     *         <li>movieId (required) - Movie ID, max 10 characters</li>
     *         </ul>
     *
     *         <p>
     *         <b>Responses:</b>
     *         </p>
     *         <ul>
     *         <li>200 OK - Movie details retrieved successfully</li>
     *         <li>404 Not Found - Movie not found with given ID</li>
     *         </ul>
     */
    @GetMapping("/{movieId}")
    public ResponseEntity<?> getMovieDetails(
            @PathVariable String movieId) {

        return movieService.getMovieById(movieId)
                .<ResponseEntity<?>>map(movie -> ResponseEntity.ok(ApiResponse.success(movie)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiErrorResponse.of("NOT_FOUND", "Movie not found")));
    }
}
