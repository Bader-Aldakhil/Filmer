package com.filmer.repository;

import com.filmer.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Movie entity operations.
 * Provides standard CRUD operations and custom queries for movies.
 */
@Repository
public interface MovieRepository extends JpaRepository<Movie, String> {

    /**
     * Find movies by genre ID with pagination.
     *
     * @param genreId  The genre ID to filter by
     * @param pageable Pagination and sorting parameters
     * @return Page of movies belonging to the specified genre
     */
    @Query("SELECT m FROM Movie m JOIN m.genres g WHERE g.id = :genreId")
    Page<Movie> findByGenreId(@Param("genreId") Long genreId, Pageable pageable);

    /**
     * Find movies where title starts with a specific character.
     *
     * @param prefix   The prefix to match (single character)
     * @param pageable Pagination and sorting parameters
     * @return Page of movies with titles starting with the prefix
     */
    Page<Movie> findByTitleStartingWithIgnoreCase(String prefix, Pageable pageable);

    /**
     * Find movies where title starts with a non-alphabetic character.
     * Used for the '*' filter option.
     *
     * @param pageable Pagination and sorting parameters
     * @return Page of movies with non-alpha starting titles
     */
    @Query("SELECT m FROM Movie m WHERE m.title NOT REGEXP '^[A-Za-z]'")
    Page<Movie> findByTitleStartingWithNonAlpha(Pageable pageable);

    /**
     * Find a movie by ID with its genres eagerly loaded.
     *
     * @param id The movie ID
     * @return Optional containing the movie with genres if found
     */
    @Query("SELECT m FROM Movie m LEFT JOIN FETCH m.genres WHERE m.id = :id")
    Optional<Movie> findByIdWithGenres(@Param("id") String id);

    /**
     * Count movies by genre ID.
     *
     * @param genreId The genre ID to count movies for
     * @return Number of movies in the specified genre
     */
    @Query("SELECT COUNT(m) FROM Movie m JOIN m.genres g WHERE g.id = :genreId")
    long countByGenreId(@Param("genreId") Long genreId);
}
