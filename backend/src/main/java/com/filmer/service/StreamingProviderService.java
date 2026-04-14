package com.filmer.service;

import com.filmer.dto.response.PlaybackGrantResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Streaming provider service that generates embed URLs for real movie/TV content
 * using the VidSrc embed platform (vidsrc.xyz), which resolves titles via IMDb or TMDB IDs.
 *
 * Movies:   https://vidsrc.xyz/embed/movie?imdb={imdbId}   OR  ?tmdb={tmdbId}
 * TV Series: https://vidsrc.xyz/embed/tv?imdb={imdbId}&season=S&episode=E  OR  ?tmdb={tmdbId}&...
 */
@Service
public class StreamingProviderService {

    private static final String VIDSRC_BASE = "https://vidsrc.xyz/embed";
    private static final String PROVIDER_NAME = "vidsrc";

    private final int grantTtlMinutes;

    public StreamingProviderService(
            @Value("${streaming.provider.grant-ttl-minutes:120}") int grantTtlMinutes) {
        this.grantTtlMinutes = Math.max(grantTtlMinutes, 30);
    }

    /**
     * Generate a playback grant containing a VidSrc embed URL for the given title.
     *
     * @param movieId   The movie/show ID — may be an IMDb ID (tt...), a synthetic IMDb-like ID,
     *                  or a TMDB-prefixed ID (tmdb-movie-XXX / tmdb-tv-XXX).
     * @param titleType The title type string (e.g. "movie", "tvSeries", "tvMiniSeries").
     * @param season    Season number (series only).
     * @param episode   Episode number (series only).
     */
    public PlaybackGrantResponse generateGrant(String movieId, String titleType, Integer season, Integer episode) {
        boolean isSeries = isSeriesTitle(titleType);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(grantTtlMinutes);

        String embedUrl = buildEmbedUrl(movieId, isSeries, season, episode);

        PlaybackGrantResponse response = new PlaybackGrantResponse();
        response.setMovieId(movieId);
        response.setTitleType(titleType == null ? "movie" : titleType);
        response.setSeason(season);
        response.setEpisode(episode);
        response.setProvider(PROVIDER_NAME);
        response.setEmbedUrl(embedUrl);
        // streamUrl / fallbackUrl are null — embed is used exclusively
        response.setStreamUrl(null);
        response.setContentType(null);
        response.setFallbackUrl(null);
        response.setExpiresAt(expiresAt);
        return response;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String buildEmbedUrl(String movieId, boolean isSeries, Integer season, Integer episode) {
        String mediaType = isSeries ? "tv" : "movie";

        if (movieId == null || movieId.trim().isEmpty()) {
            return null;
        }

        String id = movieId.trim();
        StringBuilder url = new StringBuilder(VIDSRC_BASE)
                .append("/")
                .append(mediaType)
                .append("?");

        if (id.startsWith("tmdb-movie-")) {
            // TMDB movie numeric ID
            String tmdbId = id.replace("tmdb-movie-", "");
            url.append("tmdb=").append(tmdbId);
        } else if (id.startsWith("tmdb-tv-")) {
            // TMDB TV numeric ID
            String tmdbId = id.replace("tmdb-tv-", "");
            url.append("tmdb=").append(tmdbId);
        } else if (id.startsWith("tt")) {
            // Real IMDb ID
            url.append("imdb=").append(id);
        } else if (id.startsWith("s") && id.length() > 1 && id.substring(1).matches("\\d+")) {
            // Synthetic series ID: s{tmdbId}
            url.append("tmdb=").append(id.substring(1));
        } else if (id.startsWith("m") && id.length() > 1 && id.substring(1).matches("\\d+")) {
            // Synthetic movie ID: m{tmdbId}
            url.append("tmdb=").append(id.substring(1));
        } else {
            // Fallback: treat as IMDb-style
            url.append("imdb=").append(id);
        }

        if (isSeries && season != null && season >= 1) {
            url.append("&season=").append(season);
        }
        if (isSeries && episode != null && episode >= 1) {
            url.append("&episode=").append(episode);
        }

        return url.toString();
    }

    private boolean isSeriesTitle(String titleType) {
        if (titleType == null) return false;
        String type = titleType.toLowerCase();
        return type.contains("tv") || type.contains("series");
    }
}
