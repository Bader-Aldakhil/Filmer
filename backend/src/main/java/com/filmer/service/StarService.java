package com.filmer.service;

import com.filmer.dto.response.PaginatedResponse;
import com.filmer.dto.response.StarDetailResponse;
import com.filmer.dto.response.StarListItemResponse;
import com.filmer.entity.Star;
import com.filmer.repository.StarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.List;

/**
 * Service for star-related business logic.
 */
@Service
public class StarService {

    private final StarRepository starRepository;

    @Autowired
    public StarService(StarRepository starRepository) {
        this.starRepository = starRepository;
    }

    /**
     * Get a paginated list of stars with sorting and filtering.
     */
    public PaginatedResponse<StarListItemResponse> getStars(int page, int size, String sortBy, String order,
            String name) {
        int validatedPage = Math.max(1, page);
        int validatedSize = Math.min(Math.max(1, size), 100);

        String sortField = "birthYear".equalsIgnoreCase(sortBy) ? "birthYear" : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(order) ? Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(validatedPage - 1, validatedSize, Sort.by(direction, sortField));

        Page<Star> starPage;
        if (name != null && !name.trim().isEmpty()) {
            starPage = starRepository.findByNameContainingIgnoreCase(name.trim(), pageable);
        } else {
            starPage = starRepository.findAll(pageable);
        }

        return new PaginatedResponse<>(
                starPage.getContent().stream()
                        .map(this::mapToStarListItem)
                        .collect(Collectors.toList()),
                validatedPage,
                validatedSize,
                starPage.getTotalElements());
    }

    /**
     * Get detailed information about a specific star.
     */
    public Optional<StarDetailResponse> getStarById(String starId) {
        return starRepository.findByIdWithMovies(starId)
                .map(this::mapToStarDetail);
    }

    private StarListItemResponse mapToStarListItem(Star star) {
        StarListItemResponse item = new StarListItemResponse();
        item.setId(star.getId());
        item.setName(star.getName());
        item.setBirthYear(star.getBirthYear());
        item.setMovieCount(star.getMovies() != null ? star.getMovies().size() : 0);
        return item;
    }

    private StarDetailResponse mapToStarDetail(Star star) {
        StarDetailResponse detail = new StarDetailResponse();
        detail.setId(star.getId());
        detail.setName(star.getName());
        detail.setBirthYear(star.getBirthYear());
        if (star.getMovies() != null) {
            List<StarDetailResponse.MovieInfo> movies = star.getMovies().stream()
                    .map(m -> new StarDetailResponse.MovieInfo(m.getId(), m.getTitle(), m.getYear(), m.getDirector()))
                    .collect(Collectors.toList());
            detail.setMovies(movies);
        } else {
            detail.setMovies(Collections.emptyList());
        }
        return detail;
    }
}
