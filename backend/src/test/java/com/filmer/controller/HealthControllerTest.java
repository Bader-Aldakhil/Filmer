package com.filmer.controller;

import com.filmer.dto.response.HealthResponse;
import com.filmer.service.HealthCheckService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.filmer.config.GlobalExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for HealthController.
 * Tests HTTP layer behavior for health check endpoints using MockMvc.
 * 
 * Phase 3: These tests define expected API behavior based on specification.
 * Tests verify status codes and response shapes as per API contract.
 */
@WebMvcTest(HealthController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("HealthController Unit Tests")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HealthCheckService healthCheckService;

    @Nested
    @DisplayName("GET /api/v1/health")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return 200 OK with UP status - Success Case")
        void healthCheck_Success() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/v1/health")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.status", is("UP")))
                    .andExpect(jsonPath("$.data.database", is("UP")))
                    .andExpect(jsonPath("$.data.timestamp", notNullValue()));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/health/db")
    class DatabaseConnectivityTests {

        @Test
        @DisplayName("Should return 200 OK when database is connected - Success Case")
        void checkDatabaseConnectivity_Success() throws Exception {
            // Given
            Map<String, Object> successResult = Map.of(
                    "success", true,
                    "result", 1,
                    "message", "Database connection successful",
                    "database_status", "UP"
            );
            when(healthCheckService.checkDatabaseConnectivity()).thenReturn(successResult);

            // When/Then
            mockMvc.perform(get("/api/v1/health/db")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.success", is(true)))
                    .andExpect(jsonPath("$.data.database_status", is("UP")))
                    .andExpect(jsonPath("$.data.result", is(1)));

            verify(healthCheckService, times(1)).checkDatabaseConnectivity();
        }

        @Test
        @DisplayName("Should return 503 Service Unavailable when database is down - DB Failure Case")
        void checkDatabaseConnectivity_DatabaseDown() throws Exception {
            // Given
            Map<String, Object> failureResult = Map.of(
                    "success", false,
                    "message", "Database connection failed: Connection refused",
                    "database_status", "DOWN"
            );
            when(healthCheckService.checkDatabaseConnectivity()).thenReturn(failureResult);

            // When/Then
            mockMvc.perform(get("/api/v1/health/db")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.error.code", is("DB_CONNECTION_ERROR")));

            verify(healthCheckService, times(1)).checkDatabaseConnectivity();
        }

        @Test
        @DisplayName("Should handle null result from service gracefully")
        void checkDatabaseConnectivity_NullResult() throws Exception {
            // Given - service returns map with null success (edge case)
            Map<String, Object> nullResult = new java.util.HashMap<>();
            nullResult.put("success", null);
            nullResult.put("message", "Unknown state");
            nullResult.put("database_status", "UNKNOWN");
            when(healthCheckService.checkDatabaseConnectivity()).thenReturn(nullResult);

            // When/Then - Should handle gracefully (null != true, so treated as failure)
            mockMvc.perform(get("/api/v1/health/db")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isServiceUnavailable());
        }

        @Test
        @DisplayName("Should return 503 when service throws exception - Simulated Failure")
        void checkDatabaseConnectivity_ServiceException() throws Exception {
            // Given
            when(healthCheckService.checkDatabaseConnectivity())
                    .thenThrow(new RuntimeException("Unexpected error"));

            // When/Then - Exception should result in 500/503 error response
            mockMvc.perform(get("/api/v1/health/db")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().is5xxServerError());
        }
    }
}
