package com.filmer.exception;

import lombok.Getter;

/**
 * Exception thrown when an order is not found in the system.
 */
@Getter
public class OrderNotFoundException extends RuntimeException {
    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super("Order with ID " + orderId + " does not exist");
        this.orderId = orderId;
    }
}
