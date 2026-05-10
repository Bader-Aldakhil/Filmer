package com.filmer.service;

import com.filmer.dto.response.LibraryItemResponse;
import com.filmer.dto.response.PlaybackGrantResponse;
import com.filmer.dto.request.WatchProgressDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LibraryService {

    private final JdbcTemplate jdbcTemplate;
    private final AuthService authService;
    private final StreamingProviderService streamingProviderService;

    public LibraryService(JdbcTemplate jdbcTemplate, AuthService authService, StreamingProviderService streamingProviderService) {
        this.jdbcTemplate = jdbcTemplate;
        this.authService = authService;
        this.streamingProviderService = streamingProviderService;
        ensureStreamAccessLogTable();
    }

    public List<LibraryItemResponse> listPurchasedTitles(HttpSession session) {
        Long customerId = authService.requireAuthenticatedCustomerId(session);

        return jdbcTemplate.query(
                """
                SELECT
                    m.id AS movie_id,
                    m.title,
                    m.year,
                    m.title_type,
                    MAX(s.sale_date) AS last_purchased_at
                FROM sales s
                JOIN movies m ON m.id = s.movie_id
                WHERE s.customer_id = ?
                GROUP BY m.id, m.title, m.year, m.title_type
                ORDER BY MAX(s.sale_date) DESC, m.title ASC
                """,
                (rs, rowNum) -> {
                    LibraryItemResponse item = new LibraryItemResponse();
                    item.setMovieId(rs.getString("movie_id"));
                    item.setTitle(rs.getString("title"));
                    Number yearValue = (Number) rs.getObject("year");
                    if (yearValue != null) {
                        item.setYear(yearValue.shortValue());
                    }
                    item.setTitleType(rs.getString("title_type"));

                    Date purchasedDate = rs.getDate("last_purchased_at");
                    if (purchasedDate != null) {
                        item.setLastPurchasedAt(purchasedDate.toLocalDate());
                    }
                    return item;
                },
                customerId);
    }

    public boolean hasPlaybackAccess(String movieId, HttpSession session) {
        Long customerId = authService.requireAuthenticatedCustomerId(session);
        if (movieId == null || movieId.trim().isEmpty()) {
            throw new IllegalArgumentException("movieId is required");
        }

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales WHERE customer_id = ? AND movie_id = ?",
                Long.class,
                customerId,
                movieId.trim());
        return count != null && count > 0;
    }

    public Map<String, Object> getPlaybackAccessDetails(String movieId, HttpSession session) {
        Long customerId = authService.requireAuthenticatedCustomerId(session);
        if (movieId == null || movieId.trim().isEmpty()) {
            throw new IllegalArgumentException("movieId is required");
        }

        String safeMovieId = movieId.trim();
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales WHERE customer_id = ? AND movie_id = ?",
                Long.class,
                customerId,
                safeMovieId);
        boolean hasAccess = count != null && count > 0;

        String titleType = jdbcTemplate.query(
                "SELECT title_type FROM movies WHERE id = ?",
                rs -> rs.next() ? rs.getString("title_type") : null,
                safeMovieId);

        boolean isSeries = titleType != null && (titleType.toLowerCase().contains("tv") || titleType.toLowerCase().contains("series"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("movieId", safeMovieId);
        response.put("hasAccess", hasAccess);
        response.put("titleType", titleType == null ? "movie" : titleType);
        response.put("isSeries", isSeries);
        return response;
    }

    public PlaybackGrantResponse generatePlaybackGrant(String movieId, Integer season, Integer episode, String tmdbId, HttpSession session) {
        Long customerId = authService.requireAuthenticatedCustomerId(session);
        if (movieId == null || movieId.trim().isEmpty()) {
            throw new IllegalArgumentException("movieId is required");
        }

        String safeMovieId = movieId.trim();
        if (!hasPlaybackAccess(safeMovieId, session)) {
            throw new IllegalArgumentException("NO_ACCESS");
        }

        String titleType = jdbcTemplate.query(
                "SELECT title_type FROM movies WHERE id = ?",
                rs -> rs.next() ? rs.getString("title_type") : "movie",
                safeMovieId);

        boolean isSeries = titleType != null && (titleType.toLowerCase().contains("tv") || titleType.toLowerCase().contains("series"));
        if (isSeries) {
            if ((season == null || season < 1) || (episode == null || episode < 1)) {
                throw new IllegalArgumentException("INVALID_EPISODE");
            }
        }

        // Use tmdbId if provided by frontend, otherwise use the database movieId
        String effectiveStreamingId = (tmdbId != null && !tmdbId.trim().isEmpty()) ? tmdbId : safeMovieId;
        PlaybackGrantResponse grant = streamingProviderService.generateGrant(effectiveStreamingId, titleType, season, episode);
        // Important: Reset the grant's movieId to the original one so frontend tracking works
        grant.setMovieId(safeMovieId);
        logStreamGrant(customerId, grant, true, null);
        return grant;
    }

    private void ensureStreamAccessLogTable() {
        jdbcTemplate.execute(
                """
                CREATE TABLE IF NOT EXISTS stream_access_log (
                    id BIGSERIAL PRIMARY KEY,
                    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
                    movie_id VARCHAR(10) NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                    season INT,
                    episode INT,
                    provider VARCHAR(64),
                    stream_url TEXT,
                    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP,
                    success BOOLEAN NOT NULL DEFAULT TRUE,
                    error_message VARCHAR(255)
                )
                """);
    }

    private void logStreamGrant(Long customerId, PlaybackGrantResponse grant, boolean success, String errorMessage) {
        jdbcTemplate.update(
                """
                INSERT INTO stream_access_log
                (customer_id, movie_id, season, episode, provider, stream_url, granted_at, expires_at, success, error_message)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                customerId,
                grant.getMovieId(),
                grant.getSeason(),
                grant.getEpisode(),
                grant.getProvider(),
                grant.getStreamUrl(),
                Timestamp.valueOf(LocalDateTime.now()),
                grant.getExpiresAt() == null ? null : Timestamp.valueOf(grant.getExpiresAt()),
                success,
                errorMessage);
    }

    public void saveWatchProgress(HttpSession session, WatchProgressDto progress) {
        Long customerId = authService.requireAuthenticatedCustomerId(session);
        jdbcTemplate.update("""
            INSERT INTO watch_progress (customer_id, movie_id, is_series, season, episode, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (customer_id, movie_id)
            DO UPDATE SET
                is_series = EXCLUDED.is_series,
                season = EXCLUDED.season,
                episode = EXCLUDED.episode,
                updated_at = EXCLUDED.updated_at
            """,
            customerId,
            progress.getMovieId(),
            progress.getIsSeries(),
            progress.getSeason(),
            progress.getEpisode()
        );
    }

    public List<WatchProgressDto> getAllWatchProgress(HttpSession session) {
        Long customerId = authService.requireAuthenticatedCustomerId(session);
        return jdbcTemplate.query("""
            SELECT movie_id, is_series, season, episode
            FROM watch_progress
            WHERE customer_id = ?
            ORDER BY updated_at DESC
            """,
            (rs, rowNum) -> {
                WatchProgressDto dto = new WatchProgressDto();
                dto.setMovieId(rs.getString("movie_id"));
                dto.setIsSeries(rs.getBoolean("is_series"));
                dto.setSeason(rs.getInt("season") == 0 ? null : rs.getInt("season"));
                dto.setEpisode(rs.getInt("episode") == 0 ? null : rs.getInt("episode"));
                return dto;
            },
            customerId
        );
    }
}
