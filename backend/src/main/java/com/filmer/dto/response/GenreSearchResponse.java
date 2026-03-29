package com.filmer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for genre search results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenreSearchResponse {
    private List<GenreResponse> genres;
    private long totalGenres;
    private int page;
    private int size;
}
