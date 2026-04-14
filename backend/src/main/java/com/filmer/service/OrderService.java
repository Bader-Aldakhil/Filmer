package com.filmer.service;

import com.filmer.dto.request.CheckoutRequest;
import com.filmer.dto.response.CheckoutResponse;
import com.filmer.dto.response.OrderDetailResponse;
import com.filmer.dto.response.OrderListItemResponse;
import com.filmer.dto.response.PaginatedResponse;
import com.filmer.entity.Movie;
import com.filmer.repository.MovieRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
public class OrderService {

    private static final BigDecimal MOVIE_RENTAL_PRICE = new BigDecimal("4.99");

    private final JdbcTemplate jdbcTemplate;
    private final AuthService authService;
    private final CartService cartService;
    private final MovieRepository movieRepository;

    public OrderService(JdbcTemplate jdbcTemplate, AuthService authService, CartService cartService,
            MovieRepository movieRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.authService = authService;
        this.cartService = cartService;
        this.movieRepository = movieRepository;
        ensureOrderTables();
    }

    public CheckoutResponse processCheckout(CheckoutRequest request, HttpSession session) {
        Long customerId = authService.requireAuthenticatedCustomerId(session);
        validateCheckoutRequest(request);
        validatePayment(request);

        Map<String, Integer> cart = cartService.getCartSnapshot(session);
        if (cart.isEmpty()) {
            throw new IllegalArgumentException("Cannot checkout with an empty cart");
        }

        KeyHolder orderKeyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                """
                    INSERT INTO customer_orders
                    (customer_id, status, payment_status, refund_status, refund_amount, tracking_number)
                    VALUES (?, 'COMPLETED', 'COMPLETED', 'NOT_REQUESTED', 0, ?)
                    """,
                new String[] { "id" });
            ps.setLong(1, customerId);
            ps.setString(2, "TRK-" + System.currentTimeMillis());
            return ps;
        }, orderKeyHolder);

        Number orderKey = orderKeyHolder.getKey();
        if (orderKey == null) {
            throw new IllegalStateException("Failed to create order record");
        }
        long orderId = orderKey.longValue();

        List<CheckoutResponse.CheckoutItemResponse> items = new ArrayList<>();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Movie movie = movieRepository.findById(entry.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Movie not found in cart: " + entry.getKey()));

            int quantity = entry.getValue();
            if (quantity <= 0) {
                continue;
            }

            for (int i = 0; i < quantity; i++) {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "INSERT INTO sales (customer_id, movie_id, sale_date) VALUES (?, ?, CURRENT_DATE)",
                            new String[] { "id" });
                    ps.setLong(1, customerId);
                    ps.setString(2, movie.getId());
                    return ps;
                }, keyHolder);
            }

            jdbcTemplate.update(
                    "INSERT INTO order_items (order_id, movie_id, quantity, price) VALUES (?, ?, ?, ?)",
                    orderId,
                    movie.getId(),
                    quantity,
                    MOVIE_RENTAL_PRICE);

            CheckoutResponse.CheckoutItemResponse item = new CheckoutResponse.CheckoutItemResponse();
            item.setMovieId(movie.getId());
            item.setTitle(movie.getTitle());
            item.setQuantity(quantity);
            item.setPrice(MOVIE_RENTAL_PRICE);
            items.add(item);

            totalPrice = totalPrice.add(MOVIE_RENTAL_PRICE.multiply(BigDecimal.valueOf(quantity)));
        }

        cartService.clearCartAfterCheckout(session);

        CheckoutResponse response = new CheckoutResponse();
        response.setOrderId(orderId);
        response.setMessage("Order placed successfully");
        response.setItems(items);
        response.setTotalPrice(totalPrice);
        response.setOrderDate(LocalDate.now());
        return response;
    }

    public PaginatedResponse<OrderListItemResponse> listOrders(int page, int size, HttpSession session) {
        Long customerId = authService.requireAuthenticatedCustomerId(session);
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        int offset = (safePage - 1) * safeSize;

        Long totalItems = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customer_orders WHERE customer_id = ?",
                Long.class,
                customerId);
        long total = totalItems == null ? 0 : totalItems;

        List<OrderListItemResponse> items = jdbcTemplate.query(
                """
                        SELECT o.id,
                               o.created_at,
                               COALESCE(SUM(oi.quantity), 0) AS item_count,
                               COALESCE(SUM(oi.quantity * oi.price), 0) AS total_price
                        FROM customer_orders o
                        LEFT JOIN order_items oi ON oi.order_id = o.id
                        WHERE o.customer_id = ?
                        GROUP BY o.id, o.created_at
                        ORDER BY o.id DESC
                        LIMIT ? OFFSET ?
                        """,
                (rs, rowNum) -> {
                    OrderListItemResponse item = new OrderListItemResponse();
                    item.setOrderId(rs.getLong("id"));
                    Timestamp createdAt = rs.getTimestamp("created_at");
                    item.setOrderDate(createdAt.toLocalDateTime().toLocalDate());
                    item.setItemCount(rs.getInt("item_count"));
                    item.setTotalPrice(rs.getBigDecimal("total_price"));
                    return item;
                },
                customerId, safeSize, offset);

        return new PaginatedResponse<>(items, safePage, safeSize, total);
    }

    public OrderDetailResponse getOrderDetails(Long orderId, HttpSession session) {
        Long customerId = authService.requireAuthenticatedCustomerId(session);
        if (orderId == null || orderId < 1) {
            throw new IllegalArgumentException("orderId must be a positive number");
        }

        List<Map<String, Object>> orderRows = jdbcTemplate.queryForList(
                "SELECT id, customer_id, created_at FROM customer_orders WHERE id = ?",
                orderId);
        if (orderRows.isEmpty()) {
            throw new NoSuchElementException("Order not found");
        }

        Map<String, Object> row = orderRows.get(0);
        Long orderCustomerId = ((Number) row.get("customer_id")).longValue();
        if (!orderCustomerId.equals(customerId)) {
            throw new SecurityException("Forbidden");
        }

        List<OrderDetailResponse.OrderItemResponse> items = jdbcTemplate.query(
                "SELECT id, movie_id, quantity, price FROM order_items WHERE order_id = ? ORDER BY id ASC",
                (rs, rowNum) -> {
                    String movieId = rs.getString("movie_id");
                    Movie movie = movieRepository.findById(movieId)
                            .orElseThrow(() -> new NoSuchElementException("Movie not found for order item"));

                    OrderDetailResponse.OrderItemResponse item = new OrderDetailResponse.OrderItemResponse();
                    item.setSaleId(rs.getLong("id"));
                    item.setMovieId(movieId);
                    item.setTitle(movie.getTitle());
                    item.setQuantity(rs.getInt("quantity"));
                    item.setPrice(rs.getBigDecimal("price"));
                    return item;
                },
                orderId);

        if (items.isEmpty()) {
            throw new NoSuchElementException("Order has no items");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (OrderDetailResponse.OrderItemResponse item : items) {
            total = total.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        Timestamp createdAt = (Timestamp) row.get("created_at");

        OrderDetailResponse response = new OrderDetailResponse();
        response.setOrderId(orderId);
        response.setCustomerId(customerId);
        response.setItems(items);
        response.setTotalPrice(total);
        response.setOrderDate(createdAt.toLocalDateTime().toLocalDate());
        return response;
    }

    public Map<String, Object> cancelOrder(Long orderId, String reason, HttpSession session) {
        Long customerId = authService.requireAuthenticatedCustomerId(session);
        Map<String, Object> order = getOrderRecord(orderId);

        Long orderCustomerId = ((Number) order.get("customer_id")).longValue();
        if (!orderCustomerId.equals(customerId)) {
            throw new SecurityException("Forbidden");
        }

        String currentStatus = String.valueOf(order.get("status"));
        if ("CANCELLED".equalsIgnoreCase(currentStatus)) {
            throw new IllegalStateException("Order is already cancelled");
        }

        BigDecimal total = getOrderTotal(orderId);
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                """
                        UPDATE customer_orders
                        SET status = 'CANCELLED',
                            refund_status = 'INITIATED',
                            refund_amount = ?,
                            cancelled_at = ?,
                            refund_initiated_at = ?
                        WHERE id = ?
                        """,
                total,
                Timestamp.valueOf(now),
                Timestamp.valueOf(now),
                orderId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orderId", orderId);
        response.put("previousStatus", currentStatus);
        response.put("newStatus", "CANCELLED");
        response.put("refundAmount", total);
        response.put("refundStatus", "INITIATED");
        response.put("reason", reason == null ? "No reason provided" : reason);
        response.put("cancelledAt", now.toString());
        return response;
    }

    public Map<String, Object> getRefundStatus(Long orderId, HttpSession session) {
        Long customerId = authService.requireAuthenticatedCustomerId(session);
        Map<String, Object> order = getOrderRecord(orderId);
        Long orderCustomerId = ((Number) order.get("customer_id")).longValue();
        if (!orderCustomerId.equals(customerId)) {
            throw new SecurityException("Forbidden");
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orderId", orderId);
        response.put("refundAmount", order.get("refund_amount"));
        response.put("refundStatus", order.get("refund_status"));
        response.put("originalPaymentMethod", "CREDIT_CARD");

        Timestamp initiatedAt = (Timestamp) order.get("refund_initiated_at");
        if (initiatedAt != null) {
            response.put("refundInitiatedAt", initiatedAt.toLocalDateTime().toString());
        }
        Timestamp completedAt = (Timestamp) order.get("refund_completed_at");
        if (completedAt != null) {
            response.put("refundCompletedAt", completedAt.toLocalDateTime().toString());
        }
        return response;
    }

    public Map<String, Object> trackOrder(Long orderId, HttpSession session) {
        Long customerId = authService.requireAuthenticatedCustomerId(session);
        Map<String, Object> order = getOrderRecord(orderId);
        Long orderCustomerId = ((Number) order.get("customer_id")).longValue();
        if (!orderCustomerId.equals(customerId)) {
            throw new SecurityException("Forbidden");
        }

        Timestamp createdAt = (Timestamp) order.get("created_at");
        Timestamp cancelledAt = (Timestamp) order.get("cancelled_at");
        String currentStatus = String.valueOf(order.get("status"));

        List<Map<String, String>> history = new ArrayList<>();
        history.add(Map.of("status", "COMPLETED", "timestamp", createdAt.toLocalDateTime().toString()));
        if (cancelledAt != null) {
            history.add(Map.of("status", "CANCELLED", "timestamp", cancelledAt.toLocalDateTime().toString()));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("orderId", orderId);
        response.put("currentStatus", currentStatus);
        response.put("statusHistory", history);
        response.put("trackingNumber", order.get("tracking_number"));
        response.put("carrier", "Digital Delivery");
        return response;
    }

    private void validateCheckoutRequest(CheckoutRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Checkout payload is required");
        }
        if (isBlank(request.getCreditCardId()) || isBlank(request.getFirstName()) || isBlank(request.getLastName())
                || isBlank(request.getExpiration())) {
            throw new IllegalArgumentException("creditCardId, firstName, lastName, and expiration are required");
        }

        try {
            YearMonth expiration = YearMonth.parse(request.getExpiration());
            YearMonth currentMonth = YearMonth.now();
            if (expiration.isBefore(currentMonth)) {
                throw new IllegalStateException("Credit card is expired");
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("expiration must be in YYYY-MM format");
        }
    }

    private void validatePayment(CheckoutRequest request) {
        Long matches = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM creditcards
                        WHERE id = ?
                          AND lower(first_name) = lower(?)
                          AND lower(last_name) = lower(?)
                          AND to_char(expiration, 'YYYY-MM') = ?
                          AND expiration >= CURRENT_DATE
                        """,
                Long.class,
                request.getCreditCardId().trim(),
                request.getFirstName().trim(),
                request.getLastName().trim(),
                request.getExpiration().trim());

        if (matches == null || matches == 0L) {
            throw new IllegalStateException("Credit card is invalid or expired");
        }
    }

    private Map<String, Object> getOrderRecord(Long orderId) {
        if (orderId == null || orderId < 1) {
            throw new IllegalArgumentException("orderId must be a positive number");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM customer_orders WHERE id = ?",
                orderId);
        if (rows.isEmpty()) {
            throw new NoSuchElementException("Order not found");
        }
        return rows.get(0);
    }

    private BigDecimal getOrderTotal(Long orderId) {
        BigDecimal total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(quantity * price), 0) FROM order_items WHERE order_id = ?",
                BigDecimal.class,
                orderId);
        return total == null ? BigDecimal.ZERO : total;
    }

    private void ensureOrderTables() {
        jdbcTemplate.execute(
                """
                        CREATE TABLE IF NOT EXISTS customer_orders (
                            id BIGSERIAL PRIMARY KEY,
                            customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
                            status VARCHAR(20) NOT NULL,
                            payment_status VARCHAR(20) NOT NULL,
                            refund_status VARCHAR(20),
                            refund_amount DECIMAL(10,2) DEFAULT 0,
                            tracking_number VARCHAR(64),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            cancelled_at TIMESTAMP,
                            refund_initiated_at TIMESTAMP,
                            refund_completed_at TIMESTAMP
                        )
                        """);

        jdbcTemplate.execute(
                """
                        CREATE TABLE IF NOT EXISTS order_items (
                            id BIGSERIAL PRIMARY KEY,
                            order_id BIGINT NOT NULL REFERENCES customer_orders(id) ON DELETE CASCADE,
                            movie_id VARCHAR(10) NOT NULL REFERENCES movies(id) ON DELETE CASCADE,
                            quantity INT NOT NULL,
                            price DECIMAL(10,2) NOT NULL
                        )
                        """);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
