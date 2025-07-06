package com.engine.baraka.util;

import com.engine.baraka.model.Direction;
import com.engine.baraka.model.Order;
import com.engine.baraka.model.OrderRequest;
import com.engine.baraka.service.MatchingService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class TestUtils {

    private TestUtils() {}

    public static class OrderRequestBuilder {
        private String asset = "BTC";
        private BigDecimal price = new BigDecimal("50000");
        private BigDecimal amount = BigDecimal.ONE;
        private Direction direction = Direction.BUY;

        public OrderRequestBuilder withAsset(String asset) {
            this.asset = asset;
            return this;
        }

        public OrderRequestBuilder withPrice(double price) {
            return withPrice(BigDecimal.valueOf(price));
        }

        public OrderRequestBuilder withPrice(BigDecimal price) {
            this.price = price;
            return this;
        }

        public OrderRequestBuilder withAmount(double amount) {
            return withAmount(BigDecimal.valueOf(amount));
        }

        public OrderRequestBuilder withAmount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public OrderRequestBuilder buy() {
            this.direction = Direction.BUY;
            return this;
        }

        public OrderRequestBuilder sell() {
            this.direction = Direction.SELL;
            return this;
        }

        public OrderRequestBuilder withDirection(Direction direction) {
            this.direction = direction;
            return this;
        }

        public OrderRequest build() {
            return new OrderRequest(asset, price, amount, direction);
        }
    }

    public static OrderRequestBuilder orderRequest() {
        return new OrderRequestBuilder();
    }

    public static OrderRequest createBtcBuyRequest(BigDecimal price, BigDecimal amount) {
        return orderRequest().withAsset("BTC").withPrice(price).withAmount(amount).buy().build();
    }

    public static OrderRequest createBtcSellRequest(BigDecimal price, BigDecimal amount) {
        return orderRequest().withAsset("BTC").withPrice(price).withAmount(amount).sell().build();
    }

    public static OrderRequest createOrderRequest(String asset, BigDecimal price, BigDecimal amount,
                                                  Direction direction) {
        return orderRequest().withAsset(asset).withPrice(price).withAmount(amount).withDirection(direction).build();
    }

    public static void assertOrderFullyFilled(Order order, int expectedTradeCount) {
        assertAll("Order " + order.getId() + " should be fully filled",
                () -> assertEquals(0, order.getPendingAmount().compareTo(BigDecimal.ZERO),
                        "Pending amount should be zero"),
                () -> assertEquals(expectedTradeCount, order.getTrades().size(),
                        "Should have the expected number of trades")
        );
    }

    public static void assertOrderPartiallyFilled(Order order, BigDecimal expectedPendingAmount,
                                                  int expectedTradeCount) {
        assertAll("Order " + order.getId() + " should be partially filled",
                () -> assertEquals(0, order.getPendingAmount().compareTo(expectedPendingAmount),
                        "Pending amount mismatch"),
                () -> assertFalse(order.isOrderFullyFilled(), "isOrderFullyFilled() should be false"),
                () -> assertEquals(expectedTradeCount, order.getTrades().size(),
                        "Should have the expected number of trades")
        );
    }

    public static void assertOrderNotFilled(Order order) {
        assertAll("Order " + order.getId() + " should not be filled",
                () -> assertEquals(0, order.getPendingAmount().compareTo(order.getAmount()),
                        "Pending amount should equal the original amount"),
                () -> assertTrue(order.getTrades().isEmpty(),
                        "Order should have no trades")
        );
    }

    public static void assertTradeExists(Order order, long counterpartyOrderId, BigDecimal amount, BigDecimal price) {
        boolean tradeFound = order.getTrades().stream().anyMatch(trade ->
                trade.orderId() == counterpartyOrderId &&
                        trade.amount().compareTo(amount) == 0 &&
                        trade.price().compareTo(price) == 0
        );
        assertTrue(tradeFound, "Order " + order.getId() + " should have a trade with counterparty "
                + counterpartyOrderId + " for amount " + amount + " at price " + price);
    }

    public static void assertTradeExistsWithCounterparty(Order order, long counterpartyOrderId) {
        boolean tradeFound = order.getTrades().stream().anyMatch(
                trade -> trade.orderId() == counterpartyOrderId);
        assertTrue(tradeFound, "Order " + order.getId() + " should have a trade with counterparty "
                + counterpartyOrderId);
    }

    public static void assertTradeNotExistsWithCounterparty(Order order, long counterpartyOrderId) {
        boolean tradeFound = order.getTrades().stream().anyMatch(
                trade -> trade.orderId() == counterpartyOrderId);
        assertFalse(tradeFound, "Order " + order.getId() + " should NOT have a trade with counterparty "
                + counterpartyOrderId);
    }

    public static class TimeOrderedOrdersBuilder {
        private final MatchingService service;
        private String asset = "BTC";
        private Direction direction = Direction.SELL;
        private BigDecimal price = new BigDecimal("50000");
        private BigDecimal amount = BigDecimal.ONE;
        private int count = 3;
        private long timeIncrementMillis = 10;
        private Instant startTime = Instant.now();

        public TimeOrderedOrdersBuilder(MatchingService service) {
            this.service = service;
        }

        public TimeOrderedOrdersBuilder withAsset(String asset) { this.asset = asset; return this; }
        public TimeOrderedOrdersBuilder buy() { this.direction = Direction.BUY; return this; }
        public TimeOrderedOrdersBuilder sell() { this.direction = Direction.SELL; return this; }
        public TimeOrderedOrdersBuilder withDirection(Direction direction) { this.direction = direction; return this; }
        public TimeOrderedOrdersBuilder withPrice(BigDecimal price) { this.price = price; return this; }
        public TimeOrderedOrdersBuilder withAmount(BigDecimal amount) { this.amount = amount; return this; }
        public TimeOrderedOrdersBuilder withCount(int count) { this.count = count; return this; }
        public TimeOrderedOrdersBuilder withTimeIncrement(long timeIncrementMillis) {
            this.timeIncrementMillis = timeIncrementMillis; return this; }
        public TimeOrderedOrdersBuilder withStartTime(Instant startTime) { this.startTime = startTime; return this; }

        public List<Order> build() {
            List<Order> orders = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                OrderRequest request = orderRequest()
                        .withAsset(asset)
                        .withPrice(price)
                        .withAmount(amount)
                        .withDirection(direction)
                        .build();

                orders.add(service.processNewOrder(request, startTime.plusMillis(i * timeIncrementMillis)));
            }
            return orders;
        }
    }

    public static TimeOrderedOrdersBuilder timeOrderedOrders(MatchingService service) {
        return new TimeOrderedOrdersBuilder(service);
    }
}