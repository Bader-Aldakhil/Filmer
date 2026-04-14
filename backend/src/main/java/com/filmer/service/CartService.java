package com.filmer.service;

import com.filmer.dto.request.AddToCartRequest;
import com.filmer.dto.request.UpdateCartItemRequest;
import com.filmer.dto.response.CartResponse;
import com.filmer.entity.Movie;
import com.filmer.repository.MovieRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private static final BigDecimal MOVIE_RENTAL_PRICE = new BigDecimal("4.99");

    private final MovieRepository movieRepository;
    private final AuthService authService;

    public CartService(MovieRepository movieRepository, AuthService authService) {
        this.movieRepository = movieRepository;
        this.authService = authService;
    }

    public CartResponse viewCart(HttpSession session) {
        authService.requireAuthenticatedCustomerId(session);
        return buildCartResponse(getCart(session, true));
    }

    public CartResponse addToCart(AddToCartRequest request, HttpSession session) {
        authService.requireAuthenticatedCustomerId(session);
        validateAddRequest(request);
        ensureCatalogEntry(request);

        Map<String, Integer> cart = getCart(session, true);
        // Rental cart allows only one copy per title.
        cart.put(request.getMovieId(), 1);
        session.setAttribute(AuthService.SESSION_CART, cart);

        return buildCartResponse(cart);
    }

    public CartResponse updateCartItem(String movieId, UpdateCartItemRequest request, HttpSession session) {
        authService.requireAuthenticatedCustomerId(session);
        if (movieId == null || movieId.trim().isEmpty()) {
            throw new IllegalArgumentException("movieId is required");
        }
        if (request == null || request.getQuantity() == null || request.getQuantity() != 1) {
            throw new IllegalArgumentException("quantity must be 1");
        }

        Map<String, Integer> cart = getCart(session, true);
        if (!cart.containsKey(movieId)) {
            throw new IllegalArgumentException("Item not in cart");
        }

        cart.put(movieId, request.getQuantity());
        session.setAttribute(AuthService.SESSION_CART, cart);
        return buildCartResponse(cart);
    }

    public CartResponse removeFromCart(String movieId, HttpSession session) {
        authService.requireAuthenticatedCustomerId(session);
        if (movieId == null || movieId.trim().isEmpty()) {
            throw new IllegalArgumentException("movieId is required");
        }

        Map<String, Integer> cart = getCart(session, true);
        if (!cart.containsKey(movieId)) {
            throw new IllegalArgumentException("Item not in cart");
        }

        cart.remove(movieId);
        session.setAttribute(AuthService.SESSION_CART, cart);
        return buildCartResponse(cart);
    }

    public CartResponse clearCart(HttpSession session) {
        authService.requireAuthenticatedCustomerId(session);
        Map<String, Integer> cart = new LinkedHashMap<>();
        session.setAttribute(AuthService.SESSION_CART, cart);
        return buildCartResponse(cart);
    }

    public Map<String, Integer> getCartSnapshot(HttpSession session) {
        authService.requireAuthenticatedCustomerId(session);
        return new LinkedHashMap<>(getCart(session, true));
    }

    public void clearCartAfterCheckout(HttpSession session) {
        session.setAttribute(AuthService.SESSION_CART, new LinkedHashMap<String, Integer>());
    }

    private void validateAddRequest(AddToCartRequest request) {
        if (request == null || request.getMovieId() == null || request.getMovieId().trim().isEmpty()) {
            throw new IllegalArgumentException("movieId is required");
        }
        Integer quantity = request.getQuantity() == null ? 1 : request.getQuantity();
        if (quantity != 1) {
            throw new IllegalArgumentException("quantity must be 1");
        }
    }

    private void ensureCatalogEntry(AddToCartRequest request) {
        if (movieRepository.findById(request.getMovieId()).isPresent()) {
            return;
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Movie not found");
        }

        Movie movie = new Movie();
        movie.setId(request.getMovieId().trim());
        movie.setTitle(request.getTitle().trim());
        movie.setYear(request.getYear());
        movie.setDirector("Unknown");
        movie.setTitleType(request.getTitleType() == null || request.getTitleType().trim().isEmpty()
                ? "external"
                : request.getTitleType().trim());
        if (request.getRating() != null) {
            movie.setRating(BigDecimal.valueOf(request.getRating()));
        }
        movie.setNumVotes(request.getNumVotes() == null ? 0 : request.getNumVotes());
        movieRepository.save(movie);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> getCart(HttpSession session, boolean createIfMissing) {
        Object existing = session.getAttribute(AuthService.SESSION_CART);
        if (existing instanceof Map<?, ?> map) {
            Map<String, Integer> cart = new LinkedHashMap<>();
            map.forEach((k, v) -> cart.put(String.valueOf(k), ((Number) v).intValue()));
            return cart;
        }

        if (!createIfMissing) {
            return new LinkedHashMap<>();
        }

        Map<String, Integer> cart = new LinkedHashMap<>();
        session.setAttribute(AuthService.SESSION_CART, cart);
        return cart;
    }

    private CartResponse buildCartResponse(Map<String, Integer> cart) {
        List<CartResponse.CartItemResponse> items = new ArrayList<>();
        int itemCount = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            int quantity = 1;
            if (quantity <= 0) {
                continue;
            }

            Movie movie = movieRepository.findById(entry.getKey()).orElse(null);
            if (movie == null) {
                continue;
            }

            CartResponse.CartItemResponse item = new CartResponse.CartItemResponse();
            item.setMovieId(movie.getId());
            item.setTitle(movie.getTitle());
            item.setYear(movie.getYear());
            item.setQuantity(quantity);
            item.setPrice(MOVIE_RENTAL_PRICE);
            item.setSubtotal(MOVIE_RENTAL_PRICE.multiply(BigDecimal.valueOf(quantity)));

            items.add(item);
            itemCount += quantity;
            totalPrice = totalPrice.add(item.getSubtotal());
        }

        CartResponse response = new CartResponse();
        response.setItems(items);
        response.setItemCount(itemCount);
        response.setTotalPrice(totalPrice);
        return response;
    }
}
