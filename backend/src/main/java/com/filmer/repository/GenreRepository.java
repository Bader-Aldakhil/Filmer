package com.filmer.repository;

import com.filmer.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Genre entity operations.
 * Provides standard CRUD operations and custom queries for genres.
 */
@Repository
public interface GenreRepository extends JpaRepository<Genre, Long> {

    /**
     * Find a genre by its name (case-insensitive).
     *
     * @param name The genre name to search for
     * @return Optional containing the genre if found
     */
    Optional<Genre> findByNameIgnoreCase(String name);
}
