package com.filmer.controller;

import com.filmer.dto.response.ApiResponse;
import com.filmer.dto.response.OrderDetailResponse;
import com.filmer.dto.response.OrderListItemResponse;
import com.filmer.dto.response.PaginatedResponse;
import com.filmer.dto.response.ApiErrorResponse;
import com.filmer.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Controller for order-related endpoints.
 * Handles order history and order detail retrieval.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Get order history for the authenticated customer.
     *
     * <p>Returns a paginated list of orders placed by the authenticated customer.
     * Orders are sorted by date descending (newest first).</p>
     *
     * @param page    Page number (1-indexed), defaults to 1
     * @param size    Number of items per page, defaults to 20, max 100
     * @param session The HTTP session containing customer authentication
     * @return ResponseEntity containing paginated order list
     *
     * <p><b>Query Parameters:</b></p>
     * <ul>
     *   <li>page (optional) - Page number, min 1, default 1</li>
     *   <li>size (optional) - Items per page, min 1, max 100, default 20</li>
     * </ul>
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Orders retrieved successfully</li>
     *   <li>401 Unauthorized - Not authenticated</li>
     * </ul>
     */
    @GetMapping
    public ResponseEntity<?> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpSession session) {
        try {
            PaginatedResponse<OrderListItemResponse> response = orderService.listOrders(page, size, session);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SecurityException e) {
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        }
    }

    /**
     * Get details of a completed order.
     *
     * <p>Returns detailed information about a specific order including all items.
     * The order must belong to the authenticated customer.</p>
     *
     * @param orderId The unique identifier of the order
     * @param session The HTTP session containing customer authentication
     * @return ResponseEntity containing order details
     *
     * <p><b>Path Parameters:</b></p>
     * <ul>
     *   <li>orderId (required) - Order ID, min 1</li>
     * </ul>
     *
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li>200 OK - Order details retrieved successfully</li>
     *   <li>401 Unauthorized - Not authenticated</li>
     *   <li>403 Forbidden - Order belongs to another customer</li>
     *   <li>404 Not Found - Order not found</li>
     * </ul>
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrderDetails(
            @PathVariable Long orderId,
            HttpSession session) {
        try {
            OrderDetailResponse response = orderService.getOrderDetails(orderId, session);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SecurityException e) {
            if ("Forbidden".equalsIgnoreCase(e.getMessage())) {
                return ResponseEntity.status(403)
                        .body(ApiErrorResponse.of("FORBIDDEN", "Order belongs to another customer"));
            }
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404)
                    .body(ApiErrorResponse.of("NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelOrder(
            @RequestBody(required = false) Map<String, String> request,
            HttpSession session) {
        try {
            if (request == null || request.get("orderId") == null) {
                return ResponseEntity.status(400)
                        .body(ApiErrorResponse.of("VALIDATION_ERROR", "orderId is required"));
            }

            Long orderId = Long.valueOf(request.get("orderId"));
            String reason = request.get("reason");
            Map<String, Object> response = orderService.cancelOrder(orderId, reason, session);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (NumberFormatException e) {
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", "orderId must be a valid number"));
        } catch (SecurityException e) {
            if ("Forbidden".equalsIgnoreCase(e.getMessage())) {
                return ResponseEntity.status(403)
                        .body(ApiErrorResponse.of("FORBIDDEN", "Order belongs to another customer"));
            }
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404)
                    .body(ApiErrorResponse.of("NOT_FOUND", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("INVALID_OPERATION", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/{orderId}/refund")
    public ResponseEntity<?> getRefundStatus(
            @PathVariable Long orderId,
            HttpSession session) {
        try {
            Map<String, Object> response = orderService.getRefundStatus(orderId, session);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SecurityException e) {
            if ("Forbidden".equalsIgnoreCase(e.getMessage())) {
                return ResponseEntity.status(403)
                        .body(ApiErrorResponse.of("FORBIDDEN", "Order belongs to another customer"));
            }
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404)
                    .body(ApiErrorResponse.of("NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", e.getMessage()));
        }
    }

    @GetMapping("/{orderId}/track")
    public ResponseEntity<?> trackOrder(
            @PathVariable Long orderId,
            HttpSession session) {
        try {
            Map<String, Object> response = orderService.trackOrder(orderId, session);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (SecurityException e) {
            if ("Forbidden".equalsIgnoreCase(e.getMessage())) {
                return ResponseEntity.status(403)
                        .body(ApiErrorResponse.of("FORBIDDEN", "Order belongs to another customer"));
            }
            return ResponseEntity.status(401)
                    .body(ApiErrorResponse.of("UNAUTHORIZED", e.getMessage()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404)
                    .body(ApiErrorResponse.of("NOT_FOUND", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400)
                    .body(ApiErrorResponse.of("VALIDATION_ERROR", e.getMessage()));
        }
    }
}
