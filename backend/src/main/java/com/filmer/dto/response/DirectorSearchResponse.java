package com.filmer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for director-specific search results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DirectorSearchResponse {
    private String directorName;
    private long movieCount;
    private List<MovieListItemResponse> movies;
    private int totalPages;
    private boolean hasNextPage;
}
