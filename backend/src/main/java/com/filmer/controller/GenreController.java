package com.filmer.controller;

import com.filmer.dto.response.*;
import com.filmer.service.GenreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for genre endpoints.
 * Handles genre listing and browsing movies by genre.
 */
@RestController
@RequestMapping("/api/genres")
public class GenreController {

    private final GenreService genreService;

    @Autowired
    public GenreController(GenreService genreService) {
        this.genreService = genreService;
    }

    /**
     * Get all available genres.
     */
    @GetMapping
    public ResponseEntity<GenreListResponse> getAllGenres() {
        GenreListResponse response = genreService.getAllGenres();
        return ResponseEntity.ok(response);
    }

    /**
     * Get movies by genre with pagination.
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

    /**
     * Search genres by name.
     */
    @GetMapping("/search")
    public ResponseEntity<GenreSearchResponse> searchGenresByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<GenreResponse> allGenres = genreService.getAllGenres().getItems();
        List<GenreResponse> filtered = allGenres.stream()
                .filter(g -> g.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();

        int start = Math.min(page * size, filtered.size());
        int end = Math.min(start + size, filtered.size());
        List<GenreResponse> paginated = filtered.subList(start, end);

        GenreSearchResponse response = GenreSearchResponse.builder()
                .genres(paginated)
                .totalGenres(filtered.size())
                .page(page)
                .size(size)
                .build();

        return ResponseEntity.ok(response);
    }
}
