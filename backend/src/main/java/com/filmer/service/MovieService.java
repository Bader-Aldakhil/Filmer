package com.filmer.service;

import com.filmer.dto.response.MovieDetailResponse;
import com.filmer.dto.response.MovieListItemResponse;
import com.filmer.dto.response.PaginatedResponse;
import com.filmer.entity.Movie;
import com.filmer.repository.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for movie-related business logic.
 * Handles movie listing, pagination, sorting, filtering, and detail retrieval.
 */
@Service
public class MovieService {

    private final MovieRepository movieRepository;

    @Autowired
    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    /**
     * Get a paginated list of movies with sorting and filtering.
     *
     * @param page       Page number (1-indexed)
     * @param size       Number of items per page
     * @param sortBy     Field to sort by (title, year, rating)
     * @param order      Sort order (asc, desc)
     * @param startsWith Filter by starting character (A-Z or * for non-alpha)
     * @return PaginatedResponse containing movie list
     */
    public PaginatedResponse<MovieListItemResponse> getMovies(int page, int size, String sortBy, String order, String startsWith) {
        // Validate pagination
        int validatedPage = Math.max(1, page);
        int validatedSize = Math.min(Math.max(1, size), 100);

        // Validate and map sort field
        String sortField = validateSortField(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Create pageable (convert from 1-indexed to 0-indexed)
        Pageable pageable = PageRequest.of(validatedPage - 1, validatedSize, Sort.by(direction, sortField));

        // Query with optional filter
        Page<Movie> moviePage;
        if (startsWith != null && !startsWith.isEmpty()) {
            if ("*".equals(startsWith)) {
                moviePage = movieRepository.findByTitleStartingWithNonAlpha(pageable);
            } else {
                moviePage = movieRepository.findByTitleStartingWithIgnoreCase(startsWith, pageable);
            }
        } else {
            moviePage = movieRepository.findAll(pageable);
        }

        // Map to response
        return new PaginatedResponse<>(
                moviePage.getContent().stream()
                        .map(this::mapToMovieListItem)
                        .collect(Collectors.toList()),
                validatedPage,
                validatedSize,
                moviePage.getTotalElements()
        );
    }

    /**
     * Get detailed information about a specific movie.
     *
     * @param movieId The movie ID
     * @return Optional containing movie details if found
     */
    public Optional<MovieDetailResponse> getMovieById(String movieId) {
        return movieRepository.findByIdWithGenres(movieId)
                .map(this::mapToMovieDetail);
    }

    /**
     * Find a movie entity by ID.
     *
     * @param movieId The movie ID
     * @return Optional containing the movie entity if found
     */
    public Optional<Movie> findById(String movieId) {
        return movieRepository.findById(movieId);
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
     * Map Movie entity to MovieListItemResponse.
     */
    private MovieListItemResponse mapToMovieListItem(Movie movie) {
        MovieListItemResponse item = new MovieListItemResponse();
        item.setId(movie.getId());
        item.setTitle(movie.getTitle());
        item.setYear(movie.getYear());
        item.setDirector(movie.getDirector());
        item.setRating(movie.getRating());
        item.setNumVotes(movie.getNumVotes());
        item.setGenres(movie.getGenres().stream()
                .map(g -> g.getName())
                .collect(Collectors.toList()));
        item.setStars(Collections.emptyList()); // Stars loaded separately
        return item;
    }

    /**
     * Map Movie entity to MovieDetailResponse.
     */
    private MovieDetailResponse mapToMovieDetail(Movie movie) {
        MovieDetailResponse detail = new MovieDetailResponse();
        detail.setId(movie.getId());
        detail.setTitle(movie.getTitle());
        detail.setYear(movie.getYear());
        detail.setDirector(movie.getDirector());
        detail.setRating(movie.getRating());
        detail.setNumVotes(movie.getNumVotes());
        // Genres and stars can be loaded with additional queries
        return detail;
    }
}
