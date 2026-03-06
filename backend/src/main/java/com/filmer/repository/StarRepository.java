package com.filmer.repository;

import com.filmer.entity.Star;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Star entity operations.
 */
@Repository
public interface StarRepository extends JpaRepository<Star, String> {

    /**
     * Find stars by name with partial match and pagination.
     */
    Page<Star> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find a star by ID with its movies eagerly loaded.
     */
    @Query("SELECT s FROM Star s LEFT JOIN FETCH s.movies WHERE s.id = :id")
    Optional<Star> findByIdWithMovies(@Param("id") String id);
}
