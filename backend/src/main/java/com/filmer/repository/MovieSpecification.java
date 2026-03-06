package com.filmer.repository;

import com.filmer.entity.Genre;
import com.filmer.entity.Movie;
import com.filmer.entity.Star;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Specifications for dynamic movie search queries.
 */
public class MovieSpecification {

    public static Specification<Movie> searchMovies(
            String query, String title, Integer year, Integer yearFrom, Integer yearTo,
            String director, String starName, Long genreId, List<String> titleTypes, Double minRating,
            Integer minVotes) {

        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null && !query.trim().isEmpty()) {
                String likeQuery = "%" + query.trim().toLowerCase() + "%";
                Predicate titlePred = cb.like(cb.lower(root.get("title")), likeQuery);
                Predicate directorPred = cb.like(cb.lower(root.get("director")), likeQuery);
                Join<Movie, Star> starJoin = root.join("stars", JoinType.LEFT);
                Predicate starPred = cb.like(cb.lower(starJoin.get("name")), likeQuery);

                cq.distinct(true);
                predicates.add(cb.or(titlePred, directorPred, starPred));
            }

            if (title != null && !title.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + title.trim().toLowerCase() + "%"));
            }

            if (year != null) {
                predicates.add(cb.equal(root.get("year"), year));
            }

            if (yearFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("year"), yearFrom));
            }

            if (yearTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("year"), yearTo));
            }

            if (director != null && !director.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("director")), "%" + director.trim().toLowerCase() + "%"));
            }

            if (starName != null && !starName.trim().isEmpty()) {
                Join<Movie, Star> starJoin = root.join("stars", JoinType.INNER);
                predicates.add(cb.like(cb.lower(starJoin.get("name")), "%" + starName.trim().toLowerCase() + "%"));
                cq.distinct(true);
            }

            if (genreId != null) {
                Join<Movie, Genre> genreJoin = root.join("genres", JoinType.INNER);
                predicates.add(cb.equal(genreJoin.get("id"), genreId));
                cq.distinct(true);
            }

            if (titleTypes != null && !titleTypes.isEmpty()) {
                predicates.add(root.get("titleType").in(titleTypes));
            }

            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }

            if (minVotes != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("numVotes"), minVotes));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
