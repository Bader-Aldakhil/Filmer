package com.filmer.controller;

import com.filmer.dto.request.CheckoutRequest;
import com.filmer.dto.response.ApiErrorResponse;
import com.filmer.dto.response.ApiResponse;
import com.filmer.dto.response.CheckoutResponse;
import com.filmer.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for checkout endpoints.
 * Handles order processing and payment validation.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/v1/checkout")
public class CheckoutController {

    private final OrderService orderService;

    public CheckoutController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Process the checkout with payment information.
     *
     * <p>Validates payment information, processes the order, creates sales records,
     * and clears the cart. Requires authentication and a non-empty cart.</p>
     *
     * @param request The checkout request containing credit card information
     * @param session The HTTP session containing customer authentication and cart
     * @return ResponseEntity containing order confirmation
     *
     * <p><b>Request Body:</b></p>
     * <ul>
     *   <li>creditCardId (required) - Credit card ID, max 20 chars</li>
     *   <li>firstName (required) - Cardholder first name, max 100 chars</li>
     *   <li>lastName (required) - Cardholder last name, max 100 chars</li>
     *   <li>expiration (required) - Card expiration date, format YYYY-MM, must be future</li>
     * </ul>
     *
     * <p><b>Validation:</b></p>
     * <ul>
     *   <li>Cart must not be empty</li>
     *   <li>Credit card info must match records in database</li>
     *   <li>Card must not be expired</li>
     * </ul>
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Checkout successful, order created</li>
     *   <li>400 Bad Request - Validation error or empty cart</li>
     *   <li>401 Unauthorized - Not authenticated</li>
     *   <li>402 Payment Required - Credit card validation failed</li>
     * </ul>
     */
    @PostMapping
    public ResponseEntity<?> processCheckout(
            @RequestBody CheckoutRequest request,
            HttpSession session) {
        try {
            CheckoutResponse response = orderService.processCheckout(request, session);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(402)
                .body(ApiErrorResponse.of(
                    "PAYMENT_FAILED",
                    "Credit card validation failed",
                    Map.of("reason", e.getMessage())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", e.getMessage()));
        }
    }
}
