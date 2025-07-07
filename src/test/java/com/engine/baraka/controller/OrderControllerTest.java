package com.engine.baraka.controller;

import com.engine.baraka.dto.OrderResponse;
import com.engine.baraka.mapper.OrderMapper;
import com.engine.baraka.model.Direction;
import com.engine.baraka.model.Order;
import com.engine.baraka.dto.OrderRequest;
import com.engine.baraka.service.MatchingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private MatchingService matchingService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderController orderController;

    @Test
    void createOrder_ShouldReturnCreatedOrderResponse() {
        OrderRequest orderRequest = new OrderRequest("BTC", new BigDecimal("50000"),
                new BigDecimal("1"), Direction.BUY);
        Order order = Order.createFromRequest(1L, orderRequest);
        OrderResponse orderResponse = new OrderResponse(1L, order.getTimestamp(), "BTC", 
                new BigDecimal("50000"), new BigDecimal("1"), Direction.BUY, 
                new BigDecimal("1"), new ArrayList<>());
        
        when(matchingService.processNewOrder(orderRequest)).thenReturn(order);
        when(orderMapper.orderToOrderResponse(order)).thenReturn(orderResponse);

        ResponseEntity<OrderResponse> response = orderController.createOrder(orderRequest);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(orderResponse, response.getBody());
        verify(matchingService).processNewOrder(orderRequest);
        verify(orderMapper).orderToOrderResponse(order);
    }

    @Test
    void getOrderById_WhenOrderExists_ShouldReturnOrderResponse() {
        long orderId = 1L;
        OrderRequest orderRequest = new OrderRequest("BTC", new BigDecimal("50000"),
                new BigDecimal("1"), Direction.BUY);
        Order order = Order.createFromRequest(orderId, orderRequest);
        OrderResponse orderResponse = new OrderResponse(orderId, order.getTimestamp(), "BTC", 
                new BigDecimal("50000"), new BigDecimal("1"), Direction.BUY, 
                new BigDecimal("1"), new ArrayList<>());
        
        when(matchingService.getOrderById(orderId)).thenReturn(order);
        when(orderMapper.orderToOrderResponse(order)).thenReturn(orderResponse);

        ResponseEntity<OrderResponse> response = orderController.getOrderById(orderId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(orderResponse, response.getBody());
        verify(matchingService).getOrderById(orderId);
        verify(orderMapper).orderToOrderResponse(order);
    }

    @Test
    void getOrderById_WhenOrderDoesNotExist_ShouldReturnNotFound() {
        long orderId = 321L;
        when(matchingService.getOrderById(orderId)).thenReturn(null);

        ResponseEntity<OrderResponse> response = orderController.getOrderById(orderId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(matchingService).getOrderById(orderId);
        verify(orderMapper, never()).orderToOrderResponse(any());
    }
}