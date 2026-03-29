package com.filmer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for movie search results, strictly following the API contract.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse<T> {
    private List<T> movies;
    private long totalMovies;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNextPage;
    private Object appliedFilters; // For advanced search
    private String message;        // For empty results
}
