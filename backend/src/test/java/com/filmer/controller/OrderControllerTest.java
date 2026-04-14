package com.filmer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filmer.config.GlobalExceptionHandler;
import com.filmer.dto.response.OrderDetailResponse;
import com.filmer.dto.response.OrderListItemResponse;
import com.filmer.dto.response.PaginatedResponse;
import com.filmer.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("OrderController Unit Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    @DisplayName("GET /api/v1/orders returns paginated orders")
    void listOrdersSuccess() throws Exception {
        OrderListItemResponse item = new OrderListItemResponse();
        item.setOrderId(1001L);
        item.setOrderDate(LocalDate.now());
        item.setItemCount(2);
        item.setTotalPrice(new BigDecimal("7.98"));
        PaginatedResponse<OrderListItemResponse> response = new PaginatedResponse<>(List.of(item), 1, 20, 1);

        when(orderService.listOrders(eq(1), eq(20), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.items[0].orderId", is(1001)))
                .andExpect(jsonPath("$.data.items[0].itemCount", is(2)));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} returns 403 for forbidden order")
    void getOrderDetailsForbidden() throws Exception {
        when(orderService.getOrderDetails(eq(1001L), any()))
                .thenThrow(new SecurityException("Forbidden"));

        mockMvc.perform(get("/api/v1/orders/1001").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("POST /api/v1/orders/cancel returns 400 when orderId is invalid")
    void cancelOrderInvalidOrderId() throws Exception {
        Map<String, String> payload = Map.of("orderId", "abc", "reason", "test");

        mockMvc.perform(post("/api/v1/orders/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId}/refund returns 401 when unauthenticated")
    void getRefundStatusUnauthorized() throws Exception {
        when(orderService.getRefundStatus(anyLong(), any()))
                .thenThrow(new SecurityException("Authentication required"));

        mockMvc.perform(get("/api/v1/orders/1001/refund").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId}/track returns 404 when order not found")
    void trackOrderNotFound() throws Exception {
        when(orderService.trackOrder(anyLong(), any()))
                .thenThrow(new NoSuchElementException("Order not found"));

        mockMvc.perform(get("/api/v1/orders/9999/track").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("NOT_FOUND")));
    }
}
