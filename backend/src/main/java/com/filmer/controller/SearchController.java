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
 * Controller for search endpoints.
 * Handles keyword search and advanced movie filtering.
 */
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final MovieService movieService;

    @Autowired
    public SearchController(MovieService movieService) {
        this.movieService = movieService;
    }

    /**
     * Search movies with keyword and advanced filters.
     *
     * <p>
     * Performs a search across movies using various filter criteria.
     * At least one search parameter must be provided. Multiple filters
     * are combined with AND logic.
     * </p>
     *
     * @param query    Full-text search query (searches title, director, star names)
     * @param title    Filter by movie title (partial, case-insensitive match)
     * @param year     Filter by exact year
     * @param yearFrom Filter by minimum year (inclusive)
     * @param yearTo   Filter by maximum year (inclusive)
     * @param director Filter by director name (partial, case-insensitive match)
     * @param star     Filter by star name (partial, case-insensitive match)
     * @param genreId  Filter by genre ID
     * @param minVotes Filter by minimum number of votes
     * @param page     Page number (1-indexed), defaults to 1
     * @param size     Number of items per page, defaults to 20, max 100
     * @param sortBy   Field to sort by: title, year, or rating. Defaults to title
     * @param order    Sort order: asc or desc. Defaults to asc
     * @return ResponseEntity containing paginated search results
     *
     *         <p>
     *         <b>Query Parameters:</b>
     *         </p>
     *         <ul>
     *         <li>query (optional) - Full-text search term</li>
     *         <li>title (optional) - Title filter (partial match)</li>
     *         <li>year (optional) - Exact year filter</li>
     *         <li>yearFrom (optional) - Minimum year (inclusive)</li>
     *         <li>yearTo (optional) - Maximum year (inclusive)</li>
     *         <li>director (optional) - Director name filter (partial match)</li>
     *         <li>star (optional) - Star name filter (partial match)</li>
     *         <li>genreId (optional) - Genre ID filter</li>
     *         <li>page (optional) - Page number, min 1, default 1</li>
     *         <li>size (optional) - Items per page, min 1, max 100, default 20</li>
     *         <li>sortBy (optional) - Sort field: title|year|rating, default
     *         title</li>
     *         <li>order (optional) - Sort order: asc|desc, default asc</li>
     *         </ul>
     *
     *         <p>
     *         <b>Filter Logic:</b>
     *         </p>
     *         <ul>
     *         <li>query: Full-text search across title, director, and star
     *         names</li>
     *         <li>All other filters combined with AND logic</li>
     *         <li>At least one filter must be provided</li>
     *         </ul>
     *
     *         <p>
     *         <b>Responses:</b>
     *         </p>
     *         <ul>
     *         <li>200 OK - Search results retrieved successfully</li>
     *         <li>400 Bad Request - No search criteria provided or invalid
     *         parameters</li>
     *         </ul>
     */
    @GetMapping("/movies")
    public ResponseEntity<?> searchMovies(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) String director,
            @RequestParam(required = false) String star,
            @RequestParam(required = false) Long genreId,
            @RequestParam(required = false) Integer minVotes,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String order) {

        if (query == null && title == null && year == null && yearFrom == null &&
                yearTo == null && director == null && star == null && genreId == null && minVotes == null) {
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("BAD_REQUEST", "At least one search parameter must be provided"));
        }

        PaginatedResponse<MovieListItemResponse> response = movieService.searchMovies(
                query, title, year, yearFrom, yearTo, director, star, genreId, minVotes, page, size, sortBy, order);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
