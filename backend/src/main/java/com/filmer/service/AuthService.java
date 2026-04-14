package com.filmer.service;

import com.filmer.dto.request.LoginRequest;
import com.filmer.dto.request.RegisterRequest;
import com.filmer.dto.response.CustomerSessionResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    public static final String SESSION_CUSTOMER_ID = "AUTH_CUSTOMER_ID";
    public static final String SESSION_EMAIL = "AUTH_EMAIL";
    public static final String SESSION_FIRST_NAME = "AUTH_FIRST_NAME";
    public static final String SESSION_LAST_NAME = "AUTH_LAST_NAME";
    public static final String SESSION_CART = "CART_ITEMS";

    private final JdbcTemplate jdbcTemplate;

    public AuthService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CustomerSessionResponse login(LoginRequest request, HttpSession session) {
        if (request == null || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new IllegalArgumentException("Email and password are required");
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, email, first_name, last_name, password FROM customers WHERE lower(email) = lower(?) LIMIT 1",
                request.getEmail().trim());

        if (rows.isEmpty()) {
            throw new SecurityException("Invalid email or password");
        }

        Map<String, Object> customer = rows.get(0);
        String storedHash = String.valueOf(customer.get("password"));
        if (!BCrypt.checkpw(request.getPassword(), storedHash)) {
            throw new SecurityException("Invalid email or password");
        }

        Long customerId = ((Number) customer.get("id")).longValue();
        String email = String.valueOf(customer.get("email"));
        String firstName = String.valueOf(customer.get("first_name"));
        String lastName = String.valueOf(customer.get("last_name"));

        return createSession(session, customerId, email, firstName, lastName);
    }

    public CustomerSessionResponse register(RegisterRequest request, HttpSession session) {
        if (request == null || isBlank(request.getFirstName()) || isBlank(request.getLastName())
                || isBlank(request.getEmail()) || isBlank(request.getPassword())) {
            throw new IllegalArgumentException("firstName, lastName, email and password are required");
        }

        Long existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE lower(email) = lower(?)",
                Long.class,
                request.getEmail().trim());
        if (existing != null && existing > 0) {
            throw new IllegalArgumentException("Email already registered");
        }

        String hash = BCrypt.hashpw(request.getPassword(), BCrypt.gensalt());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(
                    "INSERT INTO customers (first_name, last_name, email, password) VALUES (?, ?, ?, ?)",
                    new String[] { "id" });
            ps.setString(1, request.getFirstName().trim());
            ps.setString(2, request.getLastName().trim());
            ps.setString(3, request.getEmail().trim());
            ps.setString(4, hash);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Could not create customer");
        }

        return createSession(
                session,
                key.longValue(),
                request.getEmail().trim(),
                request.getFirstName().trim(),
                request.getLastName().trim());
    }

    public void logout(HttpSession session) {
        requireAuthenticatedCustomerId(session);
        session.invalidate();
    }

    public CustomerSessionResponse getSession(HttpSession session) {
        Long customerId = requireAuthenticatedCustomerId(session);
        String email = String.valueOf(session.getAttribute(SESSION_EMAIL));
        String firstName = String.valueOf(session.getAttribute(SESSION_FIRST_NAME));
        String lastName = String.valueOf(session.getAttribute(SESSION_LAST_NAME));
        return new CustomerSessionResponse(customerId, email, firstName, lastName);
    }

    private CustomerSessionResponse createSession(HttpSession session, Long customerId, String email,
            String firstName, String lastName) {
        session.setAttribute(SESSION_CUSTOMER_ID, customerId);
        session.setAttribute(SESSION_EMAIL, email);
        session.setAttribute(SESSION_FIRST_NAME, firstName);
        session.setAttribute(SESSION_LAST_NAME, lastName);
        return new CustomerSessionResponse(customerId, email, firstName, lastName);
    }

    public Long requireAuthenticatedCustomerId(HttpSession session) {
        Object customerId = session.getAttribute(SESSION_CUSTOMER_ID);
        if (!(customerId instanceof Number number)) {
            throw new SecurityException("Authentication required");
        }
        return number.longValue();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
