package com.filmer.controller;

import com.filmer.dto.request.LoginRequest;
import com.filmer.dto.request.RegisterRequest;
import com.filmer.dto.response.ApiErrorResponse;
import com.filmer.dto.response.ApiResponse;
import com.filmer.dto.response.CustomerSessionResponse;
import com.filmer.dto.response.MessageResponse;
import com.filmer.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication endpoints.
 * Handles customer login, logout, and session management.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Authenticate a customer and create a session.
     *
     * <p>Validates customer credentials and creates an HTTP session upon successful authentication.
     * The session is maintained via HTTP-only cookies.</p>
     *
     * @param loginRequest The login credentials containing email and password
     * @param session      The HTTP session for storing authentication state
     * @return ResponseEntity containing customer session information
     *
     * <p><b>Request Body:</b></p>
     * <ul>
     *   <li>email (required) - Customer email address, max 255 chars</li>
     *   <li>password (required) - Customer password, min 1 char</li>
     * </ul>
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Login successful, returns customer info</li>
     *   <li>400 Bad Request - Validation error (missing/invalid fields)</li>
     *   <li>401 Unauthorized - Invalid email or password</li>
     * </ul>
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest loginRequest,
            HttpSession session) {
        try {
            CustomerSessionResponse response = authService.login(loginRequest, session);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("INVALID_CREDENTIALS", "Invalid email or password"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", e.getMessage()));
        }
    }

    /**
     * Register a new customer and create a session.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest, HttpSession session) {
        try {
            CustomerSessionResponse response = authService.register(registerRequest, session);
            return ResponseEntity.status(201).body(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", e.getMessage()));
        }
    }

    /**
     * Invalidate the current session and log out the customer.
     *
     * <p>Destroys the HTTP session, effectively logging out the customer.
     * Requires an active authenticated session.</p>
     *
     * @param session The HTTP session to invalidate
     * @return ResponseEntity containing logout confirmation message
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Logout successful</li>
     *   <li>401 Unauthorized - No active session</li>
     * </ul>
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        try {
            authService.logout(session);
            MessageResponse response = new MessageResponse("Logged out successfully");
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        }
    }

    /**
     * Verify if the current session is valid and retrieve customer information.
     *
     * <p>Checks the validity of the current session and returns the authenticated
     * customer's information if the session is active.</p>
     *
     * @param session The HTTP session to verify
     * @return ResponseEntity containing session status and customer info
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Session is valid, returns customer info</li>
     *   <li>401 Unauthorized - Session invalid or expired</li>
     * </ul>
     */
    @GetMapping("/session")
    public ResponseEntity<?> checkSession(HttpSession session) {
        try {
            CustomerSessionResponse response = authService.getSession(session);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        }
    }
}
