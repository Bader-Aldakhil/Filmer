package com.filmer.controller;

import com.filmer.dto.request.AddToCartRequest;
import com.filmer.dto.request.UpdateCartItemRequest;
import com.filmer.dto.response.ApiErrorResponse;
import com.filmer.dto.response.ApiResponse;
import com.filmer.dto.response.CartResponse;
import com.filmer.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for shopping cart endpoints.
 * Handles cart viewing, adding items, updating quantities, and removing items.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping({ "/api/v1/cart", "/api/cart" })
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * Get the current shopping cart contents.
     *
     * <p>Returns all items in the customer's shopping cart with quantities,
     * prices, and total amounts. Requires authentication.</p>
     *
     * @param session The HTTP session containing customer authentication
     * @return ResponseEntity containing cart details
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Cart retrieved successfully</li>
     *   <li>401 Unauthorized - Not authenticated</li>
     * </ul>
     */
    @GetMapping
    public ResponseEntity<?> viewCart(HttpSession session) {
        try {
            CartResponse response = cartService.viewCart(session);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        }
    }

    /**
     * Add a movie to the shopping cart.
     *
     * <p>Adds the specified movie to the customer's cart. If the movie is already
     * in the cart, the quantity is increased. Requires authentication.</p>
     *
     * @param request The add to cart request containing movieId and quantity
     * @param session The HTTP session containing customer authentication
     * @return ResponseEntity containing updated cart details
     *
     * <p><b>Request Body:</b></p>
     * <ul>
     *   <li>movieId (required) - Movie ID to add, max 10 chars</li>
     *   <li>quantity (optional) - Quantity to add, min 1, max 10, default 1</li>
     * </ul>
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Item added successfully</li>
     *   <li>400 Bad Request - Validation error</li>
     *   <li>401 Unauthorized - Not authenticated</li>
     *   <li>404 Not Found - Movie not found</li>
     * </ul>
     */
    @PostMapping("/items")
    public ResponseEntity<?> addToCart(
            @RequestBody AddToCartRequest request,
            HttpSession session) {
        try {
            CartResponse cart = cartService.addToCart(request, session);
            Map<String, Object> response = Map.of(
                "message", "Item added to cart",
                "cart", cart);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            int status = e.getMessage().toLowerCase().contains("not found") ? 404 : 400;
            return ResponseEntity.status(status)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCartLegacy(
            @RequestBody AddToCartRequest request,
            HttpSession session) {
        return addToCart(request, session);
    }

    /**
     * Update the quantity of an item in the cart.
     *
     * <p>Updates the quantity of a specific movie in the customer's cart.
     * Requires authentication.</p>
     *
     * @param movieId The ID of the movie to update
     * @param request The update request containing the new quantity
     * @param session The HTTP session containing customer authentication
     * @return ResponseEntity containing updated cart details
     *
     * <p><b>Path Parameters:</b></p>
     * <ul>
     *   <li>movieId (required) - Movie ID, max 10 chars</li>
     * </ul>
     *
     * <p><b>Request Body:</b></p>
     * <ul>
     *   <li>quantity (required) - New quantity, min 1, max 10</li>
     * </ul>
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Quantity updated successfully</li>
     *   <li>400 Bad Request - Validation error</li>
     *   <li>401 Unauthorized - Not authenticated</li>
     *   <li>404 Not Found - Item not in cart</li>
     * </ul>
     */
    @PutMapping("/items/{movieId}")
    public ResponseEntity<?> updateCartItem(
            @PathVariable String movieId,
            @RequestBody UpdateCartItemRequest request,
            HttpSession session) {
        try {
            CartResponse cart = cartService.updateCartItem(movieId, request, session);
            Map<String, Object> response = Map.of(
                "message", "Cart updated",
                "cart", cart);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            int status = e.getMessage().toLowerCase().contains("not in cart") ? 404 : 400;
            return ResponseEntity.status(status)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateCartItemLegacy(
            @RequestBody LegacyUpdateCartRequest request,
            HttpSession session) {
        if (request == null) {
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", "Request body is required"));
        }
        UpdateCartItemRequest update = new UpdateCartItemRequest(request.getQuantity());
        return updateCartItem(request.getMovieId(), update, session);
    }

    /**
     * Remove a movie from the shopping cart.
     *
     * <p>Removes the specified movie from the customer's cart.
     * Requires authentication.</p>
     *
     * @param movieId The ID of the movie to remove
     * @param session The HTTP session containing customer authentication
     * @return ResponseEntity containing updated cart details
     *
     * <p><b>Path Parameters:</b></p>
     * <ul>
     *   <li>movieId (required) - Movie ID, max 10 chars</li>
     * </ul>
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Item removed successfully</li>
     *   <li>401 Unauthorized - Not authenticated</li>
     *   <li>404 Not Found - Item not in cart</li>
     * </ul>
     */
    @DeleteMapping("/items/{movieId}")
    public ResponseEntity<?> removeFromCart(
            @PathVariable String movieId,
            HttpSession session) {
        try {
            CartResponse cart = cartService.removeFromCart(movieId, session);
            Map<String, Object> response = Map.of(
                "message", "Item removed from cart",
                "cart", cart);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        } catch (IllegalArgumentException e) {
            int status = e.getMessage().toLowerCase().contains("not in cart") ? 404 : 400;
            return ResponseEntity.status(status)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removeFromCartLegacy(
            @RequestBody LegacyRemoveCartRequest request,
            HttpSession session) {
        if (request == null || request.getMovieId() == null || request.getMovieId().trim().isEmpty()) {
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", "movieId is required"));
        }
        return removeFromCart(request.getMovieId(), session);
    }

    /**
     * Clear all items from the shopping cart.
     *
     * <p>Removes all items from the customer's cart.
     * Requires authentication.</p>
     *
     * @param session The HTTP session containing customer authentication
     * @return ResponseEntity containing empty cart
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Cart cleared successfully</li>
     *   <li>401 Unauthorized - Not authenticated</li>
     * </ul>
     */
    @DeleteMapping
    public ResponseEntity<?> clearCart(HttpSession session) {
        try {
            CartResponse cart = cartService.clearCart(session);
            Map<String, Object> response = Map.of(
                    "message", "Cart cleared",
                    "cart", cart);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        }
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clearCartLegacy(HttpSession session) {
        return clearCart(session);
    }

    public static class LegacyUpdateCartRequest {
        private String movieId;
        private Integer quantity;

        public String getMovieId() {
            return movieId;
        }

        public void setMovieId(String movieId) {
            this.movieId = movieId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }

    public static class LegacyRemoveCartRequest {
        private String movieId;

        public String getMovieId() {
            return movieId;
        }

        public void setMovieId(String movieId) {
            this.movieId = movieId;
        }
    }
}
