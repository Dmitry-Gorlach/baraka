package com.engine.baraka.service;

import com.engine.baraka.dto.OrderRequest;
import com.engine.baraka.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.engine.baraka.util.TestUtils.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

class MatchingServiceTest {

    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        matchingService = new MatchingService();
    }

    @Test
    @DisplayName("Process a single order that doesn't match")
    void testProcessNewOrder_NoMatch() {
        OrderRequest request = orderRequest().build();
        Order order = matchingService.processNewOrder(request);

        assertAll("Order should be processed correctly",
                () -> assertNotNull(order, "Processed order should not be null"),
                () -> assertEquals(1L, order.getId(), "Order ID should be 1"),
                () -> assertEquals("BTC", order.getAsset()),
                () -> assertOrderNotFilled(order)
        );
    }

    @Test
    @DisplayName("Process multiple orders for different assets")
    void testProcessMultipleOrders_DifferentAssets() {
        OrderRequest buyRequest = orderRequest().withAsset("BTC").build();
        OrderRequest sellRequest = orderRequest().withAsset("ETH").sell().withPrice(2000).withAmount(10).build();

        Order order1 = matchingService.processNewOrder(buyRequest);
        Order order2 = matchingService.processNewOrder(sellRequest);

        assertAll("Multiple orders should be processed correctly",
                () -> assertEquals(1L, order1.getId(), "First order id should be 1"),
                () -> assertEquals(2L, order2.getId(), "Second order id should be 2"),
                () -> assertEquals("BTC", order1.getAsset()),
                () -> assertEquals("ETH", order2.getAsset())
        );
    }

    @Test
    @DisplayName("Get an existing order by its ID")
    void testGetOrderById() {
        OrderRequest request = orderRequest().build();
        Order createdOrder = matchingService.processNewOrder(request);

        Order retrievedOrder = matchingService.getOrderById(createdOrder.getId());

        assertNotNull(retrievedOrder, "Retrieved order should not be null");
        assertEquals(createdOrder.getId(), retrievedOrder.getId(), "Order ID should match");
    }

    @Test
    @DisplayName("Get a non-existent order by ID should return null")
    void testGetNonExistentOrderById() {
        Order retrievedOrder = matchingService.getOrderById(999L);
        assertNull(retrievedOrder, "Retrieved order should be null for non-existent id");
    }

    @Test
    @DisplayName("A simple exact match between a buy and a sell order")
    void testBasicExactMatch() {
        Order sellOrder = matchingService.processNewOrder(orderRequest().sell().build());
        Order buyOrder = matchingService.processNewOrder(orderRequest().buy().build());

        assertAll("Basic matching should work correctly",
                () -> assertOrderFullyFilled(buyOrder, 1),
                () -> assertTradeExists(buyOrder, sellOrder.getId(), BigDecimal.ONE, new BigDecimal("50000")),
                () -> assertOrderFullyFilled(sellOrder, 1),
                () -> assertTradeExists(sellOrder, buyOrder.getId(), BigDecimal.ONE, new BigDecimal("50000"))
        );
    }

    @Test
    @DisplayName("Partial fill when buy order amount is less than sell order amount")
    void testPartialFill() {
        Order sellOrder = matchingService.processNewOrder(orderRequest().sell().withAmount(2).build());
        Order buyOrder = matchingService.processNewOrder(orderRequest().buy().withAmount(1).build());

        assertAll("Partial fill should work correctly",
                () -> assertOrderFullyFilled(buyOrder, 1),
                () -> assertOrderPartiallyFilled(sellOrder, BigDecimal.ONE, 1),
                () -> assertTradeExists(buyOrder, sellOrder.getId(), BigDecimal.ONE, new BigDecimal("50000"))
        );
    }

    private record PricePriorityTestCase(String description, List<BigDecimal> sellPrices, BigDecimal buyPrice,
                                         BigDecimal buyAmount, int expectedTradeCount,
                                         List<BigDecimal> expectedMatchedSellPrices) {}

    static Stream<PricePriorityTestCase> pricePriorityTestCases() {
        return Stream.of(
                new PricePriorityTestCase(
                        "Buy price matches two lowest sell prices",
                        List.of(new BigDecimal("50000"), new BigDecimal("49000"), new BigDecimal("51000")),
                        new BigDecimal("50500"),
                        new BigDecimal("2"),
                        2,
                        List.of(new BigDecimal("49000"), new BigDecimal("50000"))),
                new PricePriorityTestCase(
                        "Buy price matches only the lowest sell price",
                        List.of(new BigDecimal("50000"), new BigDecimal("49000"), new BigDecimal("51000")),
                        new BigDecimal("49000"),
                        BigDecimal.ONE,
                        1,
                        List.of(new BigDecimal("49000"))),
                new PricePriorityTestCase(
                        "Buy price is high enough to match all sell prices",
                        List.of(new BigDecimal("50000"), new BigDecimal("49000"), new BigDecimal("51000")),
                        new BigDecimal("52000"),
                        new BigDecimal("3"),
                        3,
                        List.of(new BigDecimal("49000"), new BigDecimal("50000"), new BigDecimal("51000"))),
                new PricePriorityTestCase(
                        "Buy price is too low to match any sell price",
                        List.of(new BigDecimal("50000"), new BigDecimal("49000"), new BigDecimal("51000")),
                        new BigDecimal("48000"),
                        BigDecimal.ONE, 0,
                        List.of())
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("pricePriorityTestCases")
    @DisplayName("Price priority is respected when matching orders")
    void testPricePriority(PricePriorityTestCase testCase) {
        Map<BigDecimal, Order> sellOrdersByPrice = new HashMap<>();
        for (BigDecimal price : testCase.sellPrices()) {
            Order sellOrder = matchingService.processNewOrder(
                    orderRequest().sell().withPrice(price).withAmount(1).build());
            sellOrdersByPrice.put(price, sellOrder);
        }

        Order buyOrder = matchingService.processNewOrder(
                orderRequest().buy().withPrice(testCase.buyPrice()).withAmount(testCase.buyAmount()).build());

        assertEquals(testCase.expectedTradeCount(), buyOrder.getTrades().size(),
                "Should have correct number of trades");
        Set<BigDecimal> actualTradedPrices = buyOrder.getTrades().stream()
                .map(Trade::price).collect(Collectors.toSet());
        assertEquals(new HashSet<>(testCase.expectedMatchedSellPrices()),
                actualTradedPrices, "Should match trades at the correct price levels");
    }


    @Test
    @DisplayName("Time priority is respected when matching orders at the same price, ")
    void testTimePriority() {
        List<Order> sellOrders = timeOrderedOrders(matchingService)
                .sell()
                .withPrice(new BigDecimal(50000))
                .withAmount(new BigDecimal(1))
                .withCount(3)
                .build();

        Order buyOrder = matchingService.processNewOrder(orderRequest().buy().withPrice(50000).withAmount(2).build());

        assertAll("Ensure that when multiple orders have the same price, the matching" +
                        "logic fills the oldest first, e.g. BUY order can match with only 2 of the 3 sell orders",
                () -> assertOrderFullyFilled(buyOrder, 2),
                () -> assertTradeExistsWithCounterparty(buyOrder, sellOrders.get(0).getId()),
                () -> assertTradeExistsWithCounterparty(buyOrder, sellOrders.get(1).getId()),
                () -> assertTradeNotExistsWithCounterparty(buyOrder, sellOrders.get(2).getId()),
                () -> assertOrderFullyFilled(sellOrders.get(0), 1),
                () -> assertOrderFullyFilled(sellOrders.get(1), 1),
                () -> assertOrderNotFilled(sellOrders.get(2))
        );
    }

    @ParameterizedTest(name = "Edge case test with no match when {2}")
    @CsvSource({"50000, 49999, buy price too low", "50000, 50000, assets are different"})
    void testNoMatchingOrders(double sellPriceVal, double buyPriceVal, String scenario) {
        String sellAsset = "BTC";
        String buyAsset = "BTC";
        if (scenario.contains("different")) {
            buyAsset = "ETH";
        }

        matchingService.processNewOrder(
                orderRequest().withAsset(sellAsset).sell().withPrice(sellPriceVal).withAmount(1).build());
        Order buyOrder = matchingService.processNewOrder(
                orderRequest().withAsset(buyAsset).buy().withPrice(buyPriceVal).withAmount(1).build());

        assertOrderNotFilled(buyOrder);
    }

    @Test
    @DisplayName("Concurrent order processing does not throw exceptions and processes all orders")
    void testConcurrentOrderProcessing_NoExceptions() throws InterruptedException {
        int numThreads = 10;
        int numOrdersPerThread = 20;
        int totalOrders = numThreads * numOrdersPerThread;
        try (ExecutorService executor = Executors.newFixedThreadPool(numThreads)) {

            for (int i = 0; i < totalOrders; i++) {
                final int orderIndex = i;
                executor.submit(() -> {
                    if (orderIndex % 2 == 0) {
                        matchingService.processNewOrder(orderRequest().buy().withPrice(50000 + orderIndex).build());
                    } else {
                        matchingService.processNewOrder(orderRequest().sell().withPrice(50000 - orderIndex).build());
                    }
                });
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "Tasks should complete in time");
        }

        await().atMost(5, TimeUnit.SECONDS).until(() -> matchingService.getOrderById(totalOrders) != null);
        assertNotNull(matchingService.getOrderById(totalOrders), "Last order should exist");
    }
}