package com.filmer.controller;

import com.filmer.dto.request.LoginRequest;
import com.filmer.dto.response.ApiResponse;
import com.filmer.dto.response.CustomerSessionResponse;
import com.filmer.dto.response.MessageResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for authentication endpoints.
 * Handles customer login, logout, and session management.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

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
    public ResponseEntity<ApiResponse<CustomerSessionResponse>> login(
            @RequestBody LoginRequest loginRequest,
            HttpSession session) {
        // TODO: Implement login logic
        // 1. Validate request
        // 2. Verify credentials against database
        // 3. Create session and store customer info
        CustomerSessionResponse response = new CustomerSessionResponse(
                1L, "user@example.com", "John", "Doe"
        );
        return ResponseEntity.status(501).body(ApiResponse.success(response));
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
    public ResponseEntity<ApiResponse<MessageResponse>> logout(HttpSession session) {
        // TODO: Implement logout logic
        // 1. Verify session exists
        // 2. Invalidate session
        MessageResponse response = new MessageResponse("Logged out successfully");
        return ResponseEntity.status(501).body(ApiResponse.success(response));
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
    public ResponseEntity<ApiResponse<CustomerSessionResponse>> checkSession(HttpSession session) {
        // TODO: Implement session check logic
        // 1. Verify session exists and is valid
        // 2. Return customer info from session
        CustomerSessionResponse response = new CustomerSessionResponse(
                1L, "user@example.com", "John", "Doe"
        );
        return ResponseEntity.status(501).body(ApiResponse.success(response));
    }
}
