package com.engine.baraka.controller;

import com.engine.baraka.dto.OrderResponse;
import com.engine.baraka.mapper.OrderMapper;
import com.engine.baraka.model.Order;
import com.engine.baraka.model.OrderRequest;
import com.engine.baraka.service.MatchingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final MatchingService matchingService;
    private final OrderMapper orderMapper;

    public OrderController(MatchingService matchingService, OrderMapper orderMapper) {
        this.matchingService = matchingService;
        this.orderMapper = orderMapper;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest orderRequest) {
        if (orderRequest == null || !orderRequest.isValid()) {
            String errorMessage = "Invalid order request";
            if (orderRequest != null) {
                if (orderRequest.asset() == null || orderRequest.asset().isBlank()) {
                    errorMessage = "Asset cannot be null or blank";
                } else if (orderRequest.price() == null || orderRequest.price().compareTo(BigDecimal.ZERO) <= 0) {
                    errorMessage = "Price must be positive.";
                } else if (orderRequest.amount() == null || orderRequest.amount().compareTo(BigDecimal.ZERO) <= 0) {
                    errorMessage = "Amount must be positive";
                } else if (orderRequest.direction() == null) {
                    errorMessage = "Direction cannot be null";
                }
            }
            throw new IllegalArgumentException(errorMessage);
        }

        Order order = matchingService.processNewOrder(orderRequest);
        OrderResponse response = orderMapper.orderToOrderResponse(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable long orderId) {
        Order order = matchingService.getOrderById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        OrderResponse response = orderMapper.orderToOrderResponse(order);
        return ResponseEntity.ok(response);
    }
}
