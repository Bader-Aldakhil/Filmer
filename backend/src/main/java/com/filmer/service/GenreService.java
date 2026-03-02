package com.filmer.service;

import com.filmer.dto.response.GenreListResponse;
import com.filmer.dto.response.GenreMoviesResponse;
import com.filmer.dto.response.GenreResponse;
import com.filmer.entity.Genre;
import com.filmer.entity.Movie;
import com.filmer.repository.GenreRepository;
import com.filmer.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for genre-related business logic.
 * Handles genre listing and browsing movies by genre.
 */
@Service
public class GenreService {

    private final GenreRepository genreRepository;
    private final MovieRepository movieRepository;

    @Autowired
    public GenreService(GenreRepository genreRepository, MovieRepository movieRepository) {
        this.genreRepository = genreRepository;
        this.movieRepository = movieRepository;
    }

    /**
     * Get all available genres.
     *
     * @return GenreListResponse containing all genres
     */
    public GenreListResponse getAllGenres() {
        List<Genre> genres = genreRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        List<GenreResponse> genreResponses = genres.stream()
                .map(g -> new GenreResponse(g.getId(), g.getName()))
                .collect(Collectors.toList());
        return new GenreListResponse(genreResponses);
    }

    /**
     * Find a genre by its ID.
     *
     * @param genreId The genre ID to find
     * @return Optional containing the genre if found
     */
    public Optional<Genre> findById(Long genreId) {
        return genreRepository.findById(genreId);
    }

    /**
     * Get movies by genre with pagination.
     *
     * @param genreId The genre ID to filter by
     * @param page    Page number (1-indexed)
     * @param size    Number of items per page
     * @param sortBy  Field to sort by (title, year, rating)
     * @param order   Sort order (asc, desc)
     * @return GenreMoviesResponse with genre info and paginated movies
     * @throws IllegalArgumentException if genre is not found
     */
    public GenreMoviesResponse getMoviesByGenre(Long genreId, int page, int size, String sortBy, String order) {
        // Validate genre exists
        Genre genre = genreRepository.findById(genreId)
                .orElseThrow(() -> new IllegalArgumentException("Genre not found with id: " + genreId));

        // Validate and map sort field
        String sortField = validateSortField(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Create pageable (convert from 1-indexed to 0-indexed)
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(direction, sortField));

        // Query movies by genre
        Page<Movie> moviePage = movieRepository.findByGenreId(genreId, pageable);

        // Map to response
        GenreMoviesResponse response = new GenreMoviesResponse();
        response.setGenre(new GenreResponse(genre.getId(), genre.getName()));
        response.setItems(moviePage.getContent().stream()
                .map(this::mapToGenreMovieItem)
                .collect(Collectors.toList()));
        response.setPage(page);
        response.setSize(size);
        response.setTotalItems(moviePage.getTotalElements());
        response.setTotalPages(moviePage.getTotalPages());

        return response;
    }

    /**
     * Validate and map sort field to entity field name.
     */
    private String validateSortField(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "year" -> "year";
            case "rating" -> "rating";
            default -> "title"; // default to title
        };
    }

    /**
     * Map Movie entity to GenreMovieItem response.
     */
    private GenreMoviesResponse.GenreMovieItem mapToGenreMovieItem(Movie movie) {
        GenreMoviesResponse.GenreMovieItem item = new GenreMoviesResponse.GenreMovieItem();
        item.setId(movie.getId());
        item.setTitle(movie.getTitle());
        item.setYear(movie.getYear());
        item.setDirector(movie.getDirector());
        item.setRating(movie.getRating());
        return item;
    }
}
