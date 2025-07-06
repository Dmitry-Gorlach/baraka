package com.engine.baraka.controller;

import com.engine.baraka.dto.OrderResponse;
import com.engine.baraka.mapper.OrderMapper;
import com.engine.baraka.model.Order;
import com.engine.baraka.model.OrderRequest;
import com.engine.baraka.service.MatchingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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