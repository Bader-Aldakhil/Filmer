package com.filmer.controller;

import com.filmer.dto.response.ApiErrorResponse;
import com.filmer.dto.response.ApiResponse;
import com.filmer.dto.response.LibraryItemResponse;
import com.filmer.dto.response.PlaybackGrantResponse;
import com.filmer.service.LibraryService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.filmer.dto.request.WatchProgressDto;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/library")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping
    public ResponseEntity<?> listLibrary(HttpSession session) {
        try {
            List<LibraryItemResponse> items = libraryService.listPurchasedTitles(session);
            return ResponseEntity.ok(ApiResponse.success(Map.of("items", items)));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        }
    }

    @GetMapping("/{movieId}/access")
    public ResponseEntity<?> checkAccess(@PathVariable String movieId, HttpSession session) {
        try {
            Map<String, Object> accessDetails = libraryService.getPlaybackAccessDetails(movieId, session);
            return ResponseEntity.ok(ApiResponse.success(accessDetails));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/{movieId}/stream")
    public ResponseEntity<?> getStreamGrant(
            @PathVariable String movieId,
            @RequestParam(required = false) Integer season,
            @RequestParam(required = false) Integer episode,
            HttpSession session) {
        try {
            PlaybackGrantResponse grant = libraryService.generatePlaybackGrant(movieId, season, episode, session);
            return ResponseEntity.ok(ApiResponse.success(grant));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            if ("NO_ACCESS".equals(e.getMessage())) {
                return ResponseEntity.status(403)
                        .body(ApiErrorResponse.of("NO_ACCESS", "You do not own this title"));
            }
            if ("INVALID_EPISODE".equals(e.getMessage())) {
                return ResponseEntity.status(400)
                        .body(ApiErrorResponse.of("INVALID_EPISODE", "Season and episode are required for series playback"));
            }
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/progress")
    public ResponseEntity<?> saveProgress(@RequestBody WatchProgressDto progress, HttpSession session) {
        try {
            libraryService.saveWatchProgress(session, progress);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        }
    }

    @GetMapping("/progress")
    public ResponseEntity<?> getAllProgress(HttpSession session) {
        try {
            List<WatchProgressDto> progressList = libraryService.getAllWatchProgress(session);
            return ResponseEntity.ok(ApiResponse.success(progressList));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        }
    }
}
