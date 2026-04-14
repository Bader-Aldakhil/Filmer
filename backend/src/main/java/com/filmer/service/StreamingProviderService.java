package com.filmer.service;

import com.filmer.dto.response.PlaybackGrantResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class StreamingProviderService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String movieTemplate;
    private final String seriesTemplate;
    private final String fallbackMovieTemplate;
    private final String fallbackSeriesTemplate;
    private final String signingSecret;
    private final int grantTtlMinutes;

    public StreamingProviderService(
            @Value("${streaming.provider.movie-template:https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4}") String movieTemplate,
            @Value("${streaming.provider.series-template:https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4}") String seriesTemplate,
            @Value("${streaming.provider.fallback-movie-template:}") String fallbackMovieTemplate,
            @Value("${streaming.provider.fallback-series-template:}") String fallbackSeriesTemplate,
            @Value("${streaming.provider.signing-secret:filmer-dev-secret}") String signingSecret,
            @Value("${streaming.provider.grant-ttl-minutes:30}") int grantTtlMinutes) {
        this.movieTemplate = movieTemplate;
        this.seriesTemplate = seriesTemplate;
        this.fallbackMovieTemplate = fallbackMovieTemplate;
        this.fallbackSeriesTemplate = fallbackSeriesTemplate;
        this.signingSecret = signingSecret;
        this.grantTtlMinutes = Math.max(grantTtlMinutes, 5);
    }

    public PlaybackGrantResponse generateGrant(String movieId, String titleType, Integer season, Integer episode) {
        String providerId = normalizeProviderId(movieId);
        boolean isSeries = isSeriesTitle(titleType);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(grantTtlMinutes);
        long expiresEpoch = expiresAt.toEpochSecond(ZoneOffset.UTC);
        String token = sign(providerId + ":" + (season == null ? "" : season) + ":" + (episode == null ? "" : episode) + ":" + expiresEpoch);
        String primary = buildStreamUrl(isSeries ? seriesTemplate : movieTemplate, providerId, season, episode, expiresEpoch, token);
        String fallback = buildStreamUrl(isSeries ? fallbackSeriesTemplate : fallbackMovieTemplate, providerId, season, episode, expiresEpoch, token);

        PlaybackGrantResponse response = new PlaybackGrantResponse();
        response.setMovieId(movieId);
        response.setTitleType(titleType == null ? "movie" : titleType);
        response.setSeason(season);
        response.setEpisode(episode);
        response.setProvider("self-hosted");
        response.setStreamUrl(primary);
        response.setContentType(primary != null && primary.toLowerCase().contains(".m3u8") ? "application/x-mpegURL" : "video/mp4");
        response.setEmbedUrl(null);
        response.setFallbackUrl(fallback);
        response.setExpiresAt(expiresAt);
        return response;
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(signingSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign streaming token", e);
        }
    }

    private String normalizeProviderId(String movieId) {
        if (movieId == null) {
            return "";
        }
        String id = movieId.trim();
        if (id.startsWith("tmdb-movie-") || id.startsWith("tmdb-tv-")) {
            return id.replace("tmdb-movie-", "").replace("tmdb-tv-", "");
        }
        if ((id.startsWith("m") || id.startsWith("s")) && id.length() > 1 && id.substring(1).matches("\\d+")) {
            return id.substring(1);
        }
        return id;
    }

    private boolean isSeriesTitle(String titleType) {
        if (titleType == null) {
            return false;
        }
        String type = titleType.toLowerCase();
        return type.contains("tv") || type.contains("series");
    }

    private String buildStreamUrl(String template, String id, Integer season, Integer episode, long expires, String token) {
        if (template == null || template.trim().isEmpty()) {
            return null;
        }
        return template
                .replace("{id}", id)
                .replace("{season}", season == null ? "" : String.valueOf(season))
                .replace("{episode}", episode == null ? "" : String.valueOf(episode))
                .replace("{expires}", String.valueOf(expires))
                .replace("{token}", token);
    }
}
