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

import com.filmer.repository.MovieSpecification;
import org.springframework.data.jpa.domain.Specification;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

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
     * @param type       Comma-separated list of title types (e.g., "movie" or
     *                   "tvSeries,tvMiniSeries")
     * @return PaginatedResponse containing movie list
     */
    public PaginatedResponse<MovieListItemResponse> getMovies(int page, int size, String sortBy, String order,
            String title, String type, Long genreId, Double minRating, Integer minVotes) {
        // Validate pagination
        int validatedPage = Math.max(1, page);
        int validatedSize = Math.min(Math.max(1, size), 100);

        // Validate and map sort field
        String sortField = validateSortField(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Create pageable (convert from 1-indexed to 0-indexed)
        Pageable pageable = PageRequest.of(validatedPage - 1, validatedSize, Sort.by(direction, sortField));

        // Create type list if provided
        List<String> typesList = null;
        if (type != null && !type.trim().isEmpty()) {
            typesList = Arrays.asList(type.split(","));
        }

        // Default minVotes to 100 if rating filter is applied and no minVotes provided
        Integer effectiveMinVotes = minVotes;
        if (minRating != null && minVotes == null) {
            effectiveMinVotes = 100;
        }

        // Use Specification for all filtering
        Specification<Movie> spec = MovieSpecification.searchMovies(
                null, // query
                title, // title
                null, // year
                null, // yearFrom
                null, // yearTo
                null, // director
                null, // starName
                genreId,
                typesList,
                minRating,
                effectiveMinVotes);

        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);

        // Map to response
        return new PaginatedResponse<>(
                moviePage.getContent().stream()
                        .map(this::mapToMovieListItem)
                        .collect(Collectors.toList()),
                validatedPage,
                validatedSize,
                moviePage.getTotalElements());
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
     * Search movies based on various criteria.
     */
    public PaginatedResponse<MovieListItemResponse> searchMovies(
            String query, String title, Integer year, Integer yearFrom, Integer yearTo,
            String director, String starName, Long genreId, Integer minVotes,
            int page, int size, String sortBy, String order) {

        int validatedPage = Math.max(1, page);
        int validatedSize = Math.min(Math.max(1, size), 100);

        String sortField = validateSortField(sortBy);
        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(validatedPage - 1, validatedSize, Sort.by(direction, sortField));

        Specification<Movie> spec = MovieSpecification.searchMovies(
                query, title, year, yearFrom, yearTo, director, starName, genreId, null, null, minVotes);

        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);

        return new PaginatedResponse<>(
                moviePage.getContent().stream()
                        .map(this::mapToMovieListItem)
                        .collect(Collectors.toList()),
                validatedPage,
                validatedSize,
                moviePage.getTotalElements());
    }

    /**
     * Validate and map sort field to entity field name.
     */
    private String validateSortField(String sortBy) {
        if ("year".equalsIgnoreCase(sortBy)) {
            return "year";
        } else if ("rating".equalsIgnoreCase(sortBy)) {
            return "rating";
        } else if ("popularity".equalsIgnoreCase(sortBy) || "numVotes".equalsIgnoreCase(sortBy)
                || "num_votes".equalsIgnoreCase(sortBy)) {
            return "numVotes";
        } else if ("title".equalsIgnoreCase(sortBy)) {
            return "title";
        }
        return "numVotes"; // default to popularity (numVotes) as requested
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
        item.setGenres(movie.getGenres() != null ? movie.getGenres().stream()
                .map(g -> g.getName())
                .collect(Collectors.toList()) : new ArrayList<>());
        item.setStars(movie.getStars() != null ? movie.getStars().stream()
                .map(s -> new MovieListItemResponse.StarSummary(s.getId(), s.getName()))
                .collect(Collectors.toList()) : new ArrayList<>());
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

        if (movie.getGenres() != null) {
            detail.setGenres(movie.getGenres().stream()
                    .map(g -> new MovieDetailResponse.GenreInfo(g.getId(), g.getName()))
                    .collect(Collectors.toList()));
        } else {
            detail.setGenres(new ArrayList<>());
        }

        if (movie.getStars() != null) {
            detail.setStars(movie.getStars().stream()
                    .map(s -> new MovieDetailResponse.StarInfo(s.getId(), s.getName(), s.getBirthYear()))
                    .collect(Collectors.toList()));
        } else {
            detail.setStars(new ArrayList<>());
        }

        return detail;
    }
}
