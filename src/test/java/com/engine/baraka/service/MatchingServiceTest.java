package com.engine.baraka.service;

import com.engine.baraka.model.Direction;
import com.engine.baraka.model.Order;
import com.engine.baraka.model.OrderRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MatchingServiceTest {

    @Test
    void testProcessOrder() {
        var matchingService = new MatchingService();
        var request = new OrderRequest(
            "BTC", 
            BigDecimal.valueOf(50000), 
            BigDecimal.valueOf(1), 
            Direction.BUY
        );

        var order = matchingService.processNewOrder(request);

        assertAll("Order should be processed correctly",
            () -> assertNotNull(order, "Processed order should not be null"),
            () -> assertEquals(1L, order.getId(), "Order ID should be 1"),
            () -> assertEquals("BTC", order.getAsset(), "Asset should match request"),
            () -> assertEquals(BigDecimal.valueOf(50000), order.getPrice(), "Price should match request"),
            () -> assertEquals(BigDecimal.valueOf(1), order.getAmount(), "Amount should match request"),
            () -> assertEquals(Direction.BUY, order.getDirection(), "Direction should match request"),
            () -> assertEquals(BigDecimal.valueOf(1), order.getPendingAmount(), "Pending amount should be full amount"),
            () -> assertTrue(order.getTrades().isEmpty(), "Trades list should be empty")
        );
    }
    
    @Test
    void testGetOrderById() {

        var matchingService = new MatchingService();
        var request = new OrderRequest(
            "BTC", 
            BigDecimal.valueOf(50000), 
            BigDecimal.valueOf(1), 
            Direction.BUY
        );

        var createdOrder = matchingService.processNewOrder(request);
        var retrievedOrder = matchingService.getOrderById(createdOrder.getId());

        assertAll("Retrieved order should match created order",
            () -> assertNotNull(retrievedOrder, "Retrieved order should not be null"),
            () -> assertEquals(createdOrder.getId(), retrievedOrder.getId(), "Order ID should match"),
            () -> assertEquals(createdOrder.getAsset(), retrievedOrder.getAsset(), "Asset should match"),
            () -> assertEquals(createdOrder.getPrice(), retrievedOrder.getPrice(), "Price should match"),
            () -> assertEquals(createdOrder.getAmount(), retrievedOrder.getAmount(), "Amount should match"),
            () -> assertEquals(createdOrder.getDirection(), retrievedOrder.getDirection(), "Direction should match")
        );
    }
    
    @Test
    void testGetNonExistentOrderById() {
        var matchingService = new MatchingService();
        var retrievedOrder = matchingService.getOrderById(999L);

        assertNull(retrievedOrder, "Retrieved order should be null for non-existent id");
    }
    
    @Test
    void testProcessMultipleOrders() {

        var matchingService = new MatchingService();
        OrderRequest buyRequest = new OrderRequest(
            "BTC", 
            BigDecimal.valueOf(33500),
            BigDecimal.valueOf(1), 
            Direction.BUY
        );
        OrderRequest sellRequest = new OrderRequest(
            "ETH", 
            BigDecimal.valueOf(2000),
            BigDecimal.valueOf(10), 
            Direction.SELL
        );

        Order order1 = matchingService.processNewOrder(buyRequest);
        Order order2 = matchingService.processNewOrder(sellRequest);

        assertAll("Multiple orders should be processed correctly",
            () -> assertNotNull(order1, "First order should not be null"),
            () -> assertNotNull(order2, "Second order should not be null"),
            () -> assertEquals(1L, order1.getId(), "First order id should be 1."),
            () -> assertEquals(2L, order2.getId(), "Second order id should be 2."),
            () -> assertEquals("BTC", order1.getAsset(), "First order asset should be BTC"),
            () -> assertEquals("ETH", order2.getAsset(), "Second order asset should be ETH"),
            () -> assertEquals(Direction.BUY, order1.getDirection(), "First order direction should be BUY"),
            () -> assertEquals(Direction.SELL, order2.getDirection(), "Second order direction should be SELL")
        );
    }


}