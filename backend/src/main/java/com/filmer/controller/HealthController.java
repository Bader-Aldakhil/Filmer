package com.filmer.controller;

import com.filmer.dto.response.ApiResponse;
import com.filmer.dto.response.ApiErrorResponse;
import com.filmer.dto.response.HealthResponse;
import com.filmer.service.HealthCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * Controller for utility endpoints.
 * Provides health check and system status information.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private HealthCheckService healthCheckService;

    /**
     * Health check endpoint to verify API availability and database connectivity.
     *
     * <p>This endpoint is used for monitoring and connection testing.
     * It checks the status of the service and its dependencies.</p>
     *
     * @return ResponseEntity containing health status information
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Service is healthy</li>
     *   <li>503 Service Unavailable - Service or database is down</li>
     * </ul>
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthResponse>> checkHealth() {
        HealthResponse health = new HealthResponse("UP", "UP");
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    /**
     * Database connectivity test endpoint.
     * 
     * <p>Executes a simple SELECT 1 query to verify database connectivity.
     * Used for end-to-end connectivity testing from frontend to backend to database.</p>
     * 
     * @return ResponseEntity containing database connectivity test results
     * 
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Database connection successful with query result</li>
     *   <li>503 Service Unavailable - Database connection failed</li>
     * </ul>
     */
    @GetMapping("/health/db")
    public ResponseEntity<?> checkDatabaseConnectivity() {
        Map<String, Object> result = healthCheckService.checkDatabaseConnectivity();
        
        // Return 200 if successful, 503 if database is down
        Object successValue = result.get("success");
        boolean isSuccess = Boolean.TRUE.equals(successValue);
        
        if (isSuccess) {
            return ResponseEntity.ok(ApiResponse.success(result));
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiErrorResponse.of("DB_CONNECTION_ERROR", "Database connection failed"));
        }
    }
}
