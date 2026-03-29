package com.filmer.controller;

import com.filmer.dto.request.CheckoutRequest;
import com.filmer.dto.response.ApiResponse;
import com.filmer.dto.response.CheckoutResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

/**
 * Controller for checkout endpoints.
 * Handles order processing and payment validation.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

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
    public ResponseEntity<ApiResponse<CheckoutResponse>> processCheckout(
            @RequestBody CheckoutRequest request,
            HttpSession session) {
        // TODO: Implement checkout logic
        // 1. Verify customer is authenticated
        // 2. Verify cart is not empty
        // 3. Validate credit card information against database
        // 4. Verify card is not expired
        // 5. Create sales records for each cart item
        // 6. Clear the cart
        // 7. Return order confirmation
        CheckoutResponse response = new CheckoutResponse();
        response.setOrderId(12345L);
        response.setMessage("Order placed successfully");
        response.setItems(Collections.emptyList());
        response.setTotalPrice(BigDecimal.ZERO);
        response.setOrderDate(LocalDate.now());
        return ResponseEntity.status(501).body(ApiResponse.success(response));
    }
}
