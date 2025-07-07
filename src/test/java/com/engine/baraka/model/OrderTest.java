package com.engine.baraka.model;

import com.engine.baraka.dto.OrderRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void testOfFactoryMethod() {

        long id = 1L;
        Instant timestamp = Instant.now();
        String asset = "BTC";
        BigDecimal price = BigDecimal.valueOf(43251.00);
        BigDecimal amount = BigDecimal.valueOf(2);
        Direction direction = Direction.BUY;

        Order order = Order.of(id, timestamp, asset, price, amount, direction);

        assertAll("Order factory method should create order with correct properties",
            () -> assertEquals(id, order.getId(), "ID should match"),
            () -> assertEquals(timestamp, order.getTimestamp(), "Timestamp should match"),
            () -> assertEquals(asset, order.getAsset(), "Asset should match"),
            () -> assertEquals(price, order.getPrice(), "Price should match"),
            () -> assertEquals(amount, order.getAmount(), "Amount should match"),
            () -> assertEquals(direction, order.getDirection(), "Direction should match"),
            () -> assertEquals(amount, order.getPendingAmount(), "Pending amount should be initialized to full amount"),
            () -> assertTrue(order.getTrades().isEmpty(), "Trades list should be empty")
        );
    }

    @Test
    void testFromRequestFactoryMethod() {

        long id = 1L;
        String asset = "ETH";
        BigDecimal price = BigDecimal.valueOf(3000);
        BigDecimal amount = BigDecimal.valueOf(5);
        Direction direction = Direction.SELL;
        OrderRequest request = new OrderRequest(asset, price, amount, direction);

        Order order = Order.createFromRequest(id, request);

        assertAll("Order from request should create order with correct properties",
            () -> assertEquals(id, order.getId(), "ID should match"),
            () -> assertNotNull(order.getTimestamp(), "Timestamp should not be null"),
            () -> assertEquals(asset, order.getAsset(), "Asset should match request"),
            () -> assertEquals(price, order.getPrice(), "Price should match request"),
            () -> assertEquals(amount, order.getAmount(), "Amount should match request"),
            () -> assertEquals(direction, order.getDirection(), "Direction should match request"),
            () -> assertEquals(amount, order.getPendingAmount(), "Pending amount should be initialized to full amount"),
            () -> assertTrue(order.getTrades().isEmpty(), "Trades list should be empty")
        );
    }

    @Test
    void testAddTrade() {

        Order order = Order.of(1L, Instant.now(), "BTC", 
                BigDecimal.valueOf(50000), BigDecimal.valueOf(2), Direction.BUY);
        Trade trade = new Trade(2L, BigDecimal.valueOf(1), BigDecimal.valueOf(49000));

        order.addTrade(trade);

        assertAll("Adding a trade should update order correctly",
            () -> assertEquals(1, order.getTrades().size(), "Trade list should contain one trade"),
            () -> assertEquals(trade, order.getTrades().getFirst(), "Trade in list should match added trade"),
            () -> assertEquals(BigDecimal.valueOf(1), order.getPendingAmount(), "Pending amount should be reduced by trade amount")
        );
    }

    @Test
    void testAddMultipleTrades() {

        Order order = Order.of(1L, Instant.now(), "BTC", 
                BigDecimal.valueOf(50000), BigDecimal.valueOf(5), Direction.BUY);
        Trade trade1 = new Trade(2L, BigDecimal.valueOf(2), BigDecimal.valueOf(49000));
        Trade trade2 = new Trade(3L, BigDecimal.valueOf(1.5), BigDecimal.valueOf(49500));

        order.addTrade(trade1);
        order.addTrade(trade2);

        assertAll("Adding multiple trades should update order correctly",
            () -> assertEquals(2, order.getTrades().size(), "Trade list should contain two trades"),
            () -> assertEquals(trade1, order.getTrades().getFirst(), "First trade in list should match first added trade"),
            () -> assertEquals(trade2, order.getTrades().get(1), "Second trade in list should match second added trade"),
            () -> assertEquals(BigDecimal.valueOf(1.5), order.getPendingAmount(), "Pending amount should be reduced by sum of trade amounts")
        );
    }

    @Test
    void testIsFullyFilled() {

        Order order = Order.of(1L, Instant.now(), "BTC", 
                BigDecimal.valueOf(50000), BigDecimal.valueOf(2), Direction.BUY);

        assertAll("Order fill status should be correctly tracked",
            // Initially not fully filled
            () -> assertFalse(order.isOrderFullyFilled(), "New order should not be fully filled"),

            // After adding first trade
            () -> {
                order.addTrade(new Trade(2L, BigDecimal.valueOf(1), BigDecimal.valueOf(49000)));
                assertFalse(order.isOrderFullyFilled(), "Partially filled order should not be fully filled");
            },

            // After adding second trade that completes the order
            () -> {
                order.addTrade(new Trade(3L, BigDecimal.valueOf(1), BigDecimal.valueOf(49500)));
                assertTrue(order.isOrderFullyFilled(), "Order with no pending amount should be fully filled");
            }
        );
    }

    @Test
    void testGetTradesReturnsDefensiveCopy() {

        Order order = Order.of(1L, Instant.now(), "BTC", 
                BigDecimal.valueOf(50000), BigDecimal.valueOf(2), Direction.BUY);
        order.addTrade(new Trade(2L, BigDecimal.valueOf(1), BigDecimal.valueOf(49000)));

        List<Trade> trades = order.getTrades();

        assertAll("getTrades should return a defensive copy",

            () -> assertEquals(1, trades.size(), "Returned list should contain one trade"),

            () -> {
                // Try to modify the returned list
                trades.add(new Trade(3L, BigDecimal.valueOf(1), BigDecimal.valueOf(49500)));

                // Verify that the original list in the order is unchanged
                assertEquals(1, order.getTrades().size(), "Original trades list should remain unchanged after modifying the returned copy");
            }
        );
    }
}
