package com.engine.baraka.mapper;

import com.engine.baraka.dto.OrderResponse;
import com.engine.baraka.model.Direction;
import com.engine.baraka.model.Order;
import com.engine.baraka.model.Trade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class OrderMapperTest {

    @Autowired
    private OrderMapper orderMapper;

    @Test
    void testOrderToOrderResponse() {
        Order order = Order.of(
            1L,
            Instant.now(),
            "BTC",
            BigDecimal.valueOf(43251.00),
            BigDecimal.valueOf(1),
            Direction.BUY
        );

        order.addTrade(new Trade(2L, BigDecimal.valueOf(0.5), BigDecimal.valueOf(49000)));

        OrderResponse response = orderMapper.orderToOrderResponse(order);

        assertNotNull(response);
        assertAll("Order to OrderResponse mapping",
            () -> assertEquals(order.getId(), response.id(), "ID should match"),
            () -> assertEquals(order.getTimestamp(), response.timestamp(), "Timestamp should match"),
            () -> assertEquals(order.getAsset(), response.asset(), "Asset should match"),
            () -> assertEquals(order.getPrice(), response.price(), "Price should match"),
            () -> assertEquals(order.getAmount(), response.amount(), "Amount should match"),
            () -> assertEquals(order.getDirection(), response.direction(), "Direction should match"),
            () -> assertEquals(order.getPendingAmount(), response.pendingAmount(), "Pending amount should match"),
            () -> assertEquals(order.getTrades().size(), response.trades().size(), "Trade list size should match"),
            () -> assertEquals(order.getTrades().getFirst().orderId(), response.trades().getFirst().orderId(), "Trade order ID should match")
        );
    }

    @Test
    void testOrdersToOrderResponses() {

        List<Order> orders = new ArrayList<>();

        Order order1 = Order.of(
            1L,
            Instant.now(),
            "BTC",
            BigDecimal.valueOf(50000),
            BigDecimal.valueOf(1),
            Direction.BUY
        );

        Order order2 = Order.of(
            2L,
            Instant.now(),
            "ETH",
            BigDecimal.valueOf(3000),
            BigDecimal.valueOf(10),
            Direction.SELL
        );

        orders.add(order1);
        orders.add(order2);

        List<OrderResponse> responses = orderMapper.ordersToOrderResponses(orders);

        assertNotNull(responses, "Response list should not be null");
        assertAll("Orders to OrderResponses mapping",
            () -> assertEquals(2, responses.size(), "Should convert both orders"),
            () -> assertEquals(order1.getId(), responses.getFirst().id(), "First order id should match"),
            () -> assertEquals(order2.getId(), responses.get(1).id(), "Second order id should match")
        );
    }
}
