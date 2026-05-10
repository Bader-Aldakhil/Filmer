package com.filmer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filmer.dto.response.PlaybackGrantResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class StreamingProviderService {

    private static final String VIDKING_BASE = "https://www.vidking.net/embed";
    private static final String FALLBACK_BASE = "https://vsrc.su/embed";

    private final int grantTtlMinutes;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public StreamingProviderService(
            @Value("${streaming.provider.grant-ttl-minutes:120}") int grantTtlMinutes) {
        this.grantTtlMinutes = Math.max(grantTtlMinutes, 30);
        this.httpClient  = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper();
    }

    public PlaybackGrantResponse generateGrant(
            String movieId, String titleType, Integer season, Integer episode) {

        boolean isSeries = isSeriesTitle(titleType);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(grantTtlMinutes);

        PlaybackGrantResponse response = new PlaybackGrantResponse();
        response.setMovieId(movieId);
        response.setTitleType(titleType == null ? "movie" : titleType);
        response.setSeason(season);
        response.setEpisode(episode);
        response.setExpiresAt(expiresAt);

        // ── Vidking Mirror Player ────────────────────────────────────────
        response.setProvider("vidking");
        response.setEmbedUrl(buildStreamingUrl(movieId, isSeries, season, episode));
        response.setStreamUrl(null);
        return response;
    }

    private String buildStreamingUrl(String movieId, boolean isSeries, Integer season, Integer episode) {
        if (movieId == null || movieId.trim().isEmpty()) return null;
        String id = movieId.trim();
        String mediaType = isSeries ? "tv" : "movie";
        
        // Clean ID for TMDB-based providers
        String numericId = id;
        if (id.startsWith("tmdb-movie-")) numericId = id.replace("tmdb-movie-", "");
        else if (id.startsWith("tmdb-tv-")) numericId = id.replace("tmdb-tv-", "");
        else if (id.startsWith("m") && id.length() > 1 && id.substring(1).matches("\\d+")) numericId = id.substring(1);
        else if (id.startsWith("s") && id.length() > 1 && id.substring(1).matches("\\d+")) numericId = id.substring(1);
        
        // Use Vidking if it's a TMDB numeric ID
        if (numericId.matches("\\d+")) {
            StringBuilder url = new StringBuilder(VIDKING_BASE)
                    .append("/").append(mediaType).append("/").append(numericId);
            if (isSeries) {
                url.append("/").append(season != null && season >= 1 ? season : 1);
                url.append("/").append(episode != null && episode >= 1 ? episode : 1);
            }
            return url.toString();
        }
        
        // Fallback for IMDB IDs (tt...) to a mirror that supports them natively
        StringBuilder url = new StringBuilder(FALLBACK_BASE)
                .append("/").append(mediaType).append("/").append(id);
        if (isSeries) {
            url.append("/").append(season != null && season >= 1 ? season : 1);
            url.append("/").append(episode != null && episode >= 1 ? episode : 1);
        }
        return url.toString();
    }

    private boolean isSeriesTitle(String titleType) {
        if (titleType == null) return false;
        String t = titleType.toLowerCase();
        return t.contains("tv") || t.contains("series");
    }
}
