package com.filmer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO for order list items.
 */
public class OrderListItemResponse {

    private Long orderId;
    private LocalDate orderDate;
    private Integer itemCount;
    private BigDecimal totalPrice;

    public OrderListItemResponse() {
    }

    // Getters and Setters

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }
}
