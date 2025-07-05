package com.engine.baraka.service;

import com.engine.baraka.model.Direction;
import com.engine.baraka.model.Order;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.jupiter.api.Assertions.*;

class OrderBookTest {

    @Test
    void testAddBuyOrder() {
        OrderBook orderBook = new OrderBook();
        Order buyOrder = Order.of(1L, Instant.now(), "BTC", 
                BigDecimal.valueOf(50000), BigDecimal.valueOf(1), Direction.BUY);

        orderBook.addOrder(buyOrder);

        assertAll("Buy order should be added to bids map",
            () -> assertEquals(1, orderBook.getBids().size(), "Bids map should have one entry"),
            () -> assertTrue(orderBook.getBids().containsKey(buyOrder.getPrice()), "Bids map should contain the order price"),
            () -> assertEquals(1, orderBook.getBids().get(buyOrder.getPrice()).size(), "Queue at price level should have one order"),
            () -> assertEquals(buyOrder, orderBook.getBids().get(buyOrder.getPrice()).peek(), "Order in queue should be the added order"),
            () -> assertEquals(0, orderBook.getAsks().size(), "Asks map should be empty")
        );
    }
    
    @Test
    void testAddSellOrder() {
        OrderBook orderBook = new OrderBook();
        Order sellOrder = Order.of(1L, Instant.now(), "BTC", 
                BigDecimal.valueOf(50000), BigDecimal.valueOf(1), Direction.SELL);

        orderBook.addOrder(sellOrder);

        assertAll("Sell order should be added to asks map",
            () -> assertEquals(1, orderBook.getAsks().size(), "Asks map should have one entry"),
            () -> assertTrue(orderBook.getAsks().containsKey(sellOrder.getPrice()), "Asks map should contain the order price"),
            () -> assertEquals(1, orderBook.getAsks().get(sellOrder.getPrice()).size(), "Queue at price level should have one order"),
            () -> assertEquals(sellOrder, orderBook.getAsks().get(sellOrder.getPrice()).peek(), "Order in queue should be the added order"),
            () -> assertEquals(0, orderBook.getBids().size(), "Bids map should be empty")
        );
    }
    
    @Test
    void testBidsSortedByPriceDescending() {

        OrderBook orderBook = new OrderBook();
        Order buyOrder1 = Order.of(1L, Instant.now(), "BTC", 
                BigDecimal.valueOf(50000), BigDecimal.valueOf(1), Direction.BUY);
        Order buyOrder2 = Order.of(2L, Instant.now(), "BTC", 
                BigDecimal.valueOf(51000), BigDecimal.valueOf(1), Direction.BUY);
        Order buyOrder3 = Order.of(3L, Instant.now(), "BTC", 
                BigDecimal.valueOf(49000), BigDecimal.valueOf(1), Direction.BUY);

        orderBook.addOrder(buyOrder1);
        orderBook.addOrder(buyOrder2);
        orderBook.addOrder(buyOrder3);
        
        // Assert - Verify that the bids are sorted by price in descending order
        Iterator<Map.Entry<BigDecimal, ConcurrentLinkedQueue<Order>>> iterator = 
                orderBook.getBids().entrySet().iterator();
        
        Map.Entry<BigDecimal, ConcurrentLinkedQueue<Order>> entry1 = iterator.next();
        Map.Entry<BigDecimal, ConcurrentLinkedQueue<Order>> entry2 = iterator.next();
        Map.Entry<BigDecimal, ConcurrentLinkedQueue<Order>> entry3 = iterator.next();
        
        assertAll("Bids should be sorted by price in descending order",
            () -> assertEquals(BigDecimal.valueOf(51000), entry1.getKey(), "First entry should have highest price"),
            () -> assertEquals(BigDecimal.valueOf(50000), entry2.getKey(), "Second entry should have middle price"),
            () -> assertEquals(BigDecimal.valueOf(49000), entry3.getKey(), "Third entry should have lowest price")
        );
    }
    
    @Test
    void testAsksSortedByPriceAscending() {
        OrderBook orderBook = new OrderBook();
        Order sellOrder1 = Order.of(1L, Instant.now(), "BTC", 
                BigDecimal.valueOf(50000), BigDecimal.valueOf(1), Direction.SELL);
        Order sellOrder2 = Order.of(2L, Instant.now(), "BTC", 
                BigDecimal.valueOf(51000), BigDecimal.valueOf(1), Direction.SELL);
        Order sellOrder3 = Order.of(3L, Instant.now(), "BTC", 
                BigDecimal.valueOf(49000), BigDecimal.valueOf(1), Direction.SELL);

        orderBook.addOrder(sellOrder1);
        orderBook.addOrder(sellOrder2);
        orderBook.addOrder(sellOrder3);

        Iterator<Map.Entry<BigDecimal, ConcurrentLinkedQueue<Order>>> iterator = 
                orderBook.getAsks().entrySet().iterator();
        
        Map.Entry<BigDecimal, ConcurrentLinkedQueue<Order>> entry1 = iterator.next();
        Map.Entry<BigDecimal, ConcurrentLinkedQueue<Order>> entry2 = iterator.next();
        Map.Entry<BigDecimal, ConcurrentLinkedQueue<Order>> entry3 = iterator.next();
        
        assertAll("Asks should be sorted by price in ascending order",
            () -> assertEquals(BigDecimal.valueOf(49000), entry1.getKey(), "First entry should have lowest price"),
            () -> assertEquals(BigDecimal.valueOf(50000), entry2.getKey(), "Second entry should have middle price"),
            () -> assertEquals(BigDecimal.valueOf(51000), entry3.getKey(), "Third entry should have highest price")
        );
    }
    
    @Test
    void testRemoveOrder() {
        OrderBook orderBook = new OrderBook();
        Order buyOrder = Order.of(1L, Instant.now(), "BTC", 
                BigDecimal.valueOf(50000), BigDecimal.valueOf(1), Direction.BUY);
        orderBook.addOrder(buyOrder);

        boolean removed = orderBook.removeOrder(buyOrder);

        assertAll("Order should be removed from the book",
            () -> assertTrue(removed, "removeOrder should return true"),
            () -> assertEquals(0, orderBook.getBids().size(), "Bids map should be empty after removal")
        );
    }
    
    @Test
    void testRemoveNonExistentOrder() {
        OrderBook orderBook = new OrderBook();
        Order buyOrder = Order.of(1L, Instant.now(), "BTC", 
                BigDecimal.valueOf(50000), BigDecimal.valueOf(1), Direction.BUY);

        boolean removed = orderBook.removeOrder(buyOrder);

        assertFalse(removed, "removeOrder should return false for non-existent order");
    }
}