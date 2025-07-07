package com.engine.baraka.controller;

import com.engine.baraka.model.Direction;
import com.engine.baraka.dto.OrderRequest;
import com.engine.baraka.util.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Test  POST /orders - Create a new orders")
    void createNewSellOrder() throws Exception {
        OrderRequest sellOrderRequest = TestUtils.orderRequest()
                .withAsset("TST")
                .withPrice(10.0)
                .withAmount(100.0)
                .sell()
                .build();

        OrderRequest buyOrderRequest = TestUtils.orderRequest()
                .withAsset("TST")
                .withPrice(10.0)
                .withAmount(10.0)
                .buy()
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.asset").value("TST"))
                .andExpect(jsonPath("$.price").value(10.0))
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.pendingAmount").value(90.0))
                .andExpect(jsonPath("$.trades", hasSize(1)))
                .andExpect(jsonPath("$.trades[0].orderId").value(1))
                .andExpect(jsonPath("$.trades[0].amount").value(10.0))
                .andExpect(jsonPath("$.trades[0].price").value(10.0));
    }

    @Test
    @DisplayName("Test GET /orders/{orderId} - Retrieve partially filled SELL order")
    void retrieveOrderById() throws Exception {
        OrderRequest sellOrderRequest = TestUtils.orderRequest()
                .withAsset("TST")
                .withPrice(10.0)
                .withAmount(100.0)
                .sell()
                .build();

        OrderRequest buyOrderRequest = TestUtils.orderRequest()
                .withAsset("TST")
                .withPrice(10.0)
                .withAmount(10.0)
                .buy()
                .build();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellOrderRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/orders/{orderId}", 2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.asset").value("TST"))
                .andExpect(jsonPath("$.price").value(10.0))
                .andExpect(jsonPath("$.amount").value(100.0))
                .andExpect(jsonPath("$.pendingAmount").value(90.0))
                .andExpect(jsonPath("$.trades", hasSize(1)))
                .andExpect(jsonPath("$.trades[0].orderId").value(1))
                .andExpect(jsonPath("$.trades[0].amount").value(10.0))
                .andExpect(jsonPath("$.trades[0].price").value(10.0));
    }

    @Test
    @DisplayName("Getting a non-existent order by ID should return not found")
    void getOrderById_WhenOrderDoesNotExist_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/orders/{orderId}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Creating an order with invalid request should return bad request")
    void createOrder_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        OrderRequest orderRequest = new OrderRequest("BTC", new BigDecimal("-50000"), new BigDecimal("1"), Direction.BUY);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Price must be positive."));
    }

    @Test
    @DisplayName("A simple exact match between a buy and a sell order")
    void testBasicExactMatch() throws Exception {
        OrderRequest sellOrderRequest = TestUtils.orderRequest().sell().build();
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.direction").value("SELL"));

        OrderRequest buyOrderRequest = TestUtils.orderRequest().buy().build();
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.direction").value("BUY"))
                .andExpect(jsonPath("$.pendingAmount").value(0))
                .andExpect(jsonPath("$.trades", hasSize(1)))
                .andExpect(jsonPath("$.trades[0].orderId").value(1))
                .andExpect(jsonPath("$.trades[0].amount").value(1))
                .andExpect(jsonPath("$.trades[0].price").value(50000));

        mockMvc.perform(get("/orders/{orderId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAmount").value(0))
                .andExpect(jsonPath("$.trades", hasSize(1)))
                .andExpect(jsonPath("$.trades[0].orderId").value(2));
    }

    @Test
    @DisplayName("Partial fill when buy order amount is less than sell order amount")
    void testPartialFill() throws Exception {

        OrderRequest sellOrderRequest = TestUtils.orderRequest().sell().withAmount(2).build();
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(2));

        OrderRequest buyOrderRequest = TestUtils.orderRequest().buy().withAmount(1).build();
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.amount").value(1))
                .andExpect(jsonPath("$.pendingAmount").value(0))
                .andExpect(jsonPath("$.trades", hasSize(1)));

        mockMvc.perform(get("/orders/{orderId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAmount").value(1))
                .andExpect(jsonPath("$.trades", hasSize(1)));
    }

    @Test
    @DisplayName("Price priority is respected when matching orders")
    void testPricePriority() throws Exception {
        OrderRequest sellOrder1 = TestUtils.orderRequest().sell().withPrice(50000).withAmount(1).build();
        OrderRequest sellOrder2 = TestUtils.orderRequest().sell().withPrice(49000).withAmount(1).build();
        OrderRequest sellOrder3 = TestUtils.orderRequest().sell().withPrice(51000).withAmount(1).build();

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellOrder1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellOrder2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellOrder3)))
                .andExpect(status().isCreated());

        // Create a buy order that should match with the two lowest-priced sell orders
        OrderRequest buyOrderRequest = TestUtils.orderRequest().buy().withPrice(50500).withAmount(2).build();
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pendingAmount").value(0))
                .andExpect(jsonPath("$.trades", hasSize(2)));

        mockMvc.perform(get("/orders/{orderId}", 2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAmount").value(0));

        mockMvc.perform(get("/orders/{orderId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAmount").value(0));

        mockMvc.perform(get("/orders/{orderId}", 3)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAmount").value(1))
                .andExpect(jsonPath("$.trades", hasSize(0)));
    }

    @Test
    @DisplayName("Time priority is respected when matching orders at the same price")
    void testTimePriority() throws Exception {
        OrderRequest sellOrderRequest = TestUtils.orderRequest().sell().withPrice(50000).withAmount(1).build();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sellOrderRequest)))
                    .andExpect(status().isCreated());
        }

        // Create a buy order that can only match with 2 of the 3 sell orders
        OrderRequest buyOrderRequest = TestUtils.orderRequest().buy().withPrice(50000).withAmount(2).build();
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pendingAmount").value(0))
                .andExpect(jsonPath("$.trades", hasSize(2)));

        mockMvc.perform(get("/orders/{orderId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAmount").value(0));

        mockMvc.perform(get("/orders/{orderId}", 2)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAmount").value(0));

        mockMvc.perform(get("/orders/{orderId}", 3)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAmount").value(1))
                .andExpect(jsonPath("$.trades", hasSize(0)));
    }

    @ParameterizedTest(name = "No matching orders when {2}")
    @CsvSource({
        "50000, 49999, buy price too low",
        "50000, 50000, assets are different"
    })
    @DisplayName("Edge case tests with no matching orders")
    void testNoMatchingOrders(double sellPriceVal, double buyPriceVal, String scenario) throws Exception {
        String sellAsset = "BTC";
        String buyAsset = "BTC";
        if (scenario.contains("different")) {
            buyAsset = "ETH";
        }

        OrderRequest sellOrderRequest = TestUtils.orderRequest()
                .withAsset(sellAsset)
                .sell()
                .withPrice(sellPriceVal)
                .withAmount(1)
                .build();

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellOrderRequest)))
                .andExpect(status().isCreated());

        // Create a buy order that should not match with the sell order
        OrderRequest buyOrderRequest = TestUtils.orderRequest()
                .withAsset(buyAsset)
                .buy()
                .withPrice(buyPriceVal)
                .withAmount(1)
                .build();

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pendingAmount").value(1))
                .andExpect(jsonPath("$.trades", hasSize(0)));
    }

    @Test
    @DisplayName("Edge case: Creating an order with zero amount should return bad request")
    void createOrder_WithZeroAmount_ShouldReturnBadRequest() throws Exception {
        OrderRequest orderRequest = new OrderRequest("BTC", new BigDecimal("50000"), BigDecimal.ZERO, Direction.BUY);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Edge case: Creating an order with null asset should return bad request")
    void createOrder_WithNullAsset_ShouldReturnBadRequest() throws Exception {
        OrderRequest orderRequest = new OrderRequest(null, new BigDecimal("50000"), new BigDecimal("1"), Direction.BUY);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Edge case: Creating an order with blank asset should return bad request")
    void createOrder_WithBlankAsset_ShouldReturnBadRequest() throws Exception {
        OrderRequest orderRequest = new OrderRequest("", new BigDecimal("50000"), new BigDecimal("1"), Direction.BUY);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Edge case: Creating an order with null price should return bad request")
    void createOrder_WithNullPrice_ShouldReturnBadRequest() throws Exception {
        OrderRequest orderRequest = new OrderRequest("BTC", null, new BigDecimal("1"), Direction.BUY);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Edge case: Creating an order with null amount should return bad request")
    void createOrder_WithNullAmount_ShouldReturnBadRequest() throws Exception {
        OrderRequest orderRequest = new OrderRequest("BTC", new BigDecimal("50000"), null, Direction.BUY);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Edge case: Creating an order with null direction should return bad request")
    void createOrder_WithNullDirection_ShouldReturnBadRequest() throws Exception {
        OrderRequest orderRequest = new OrderRequest("BTC", new BigDecimal("50000"), new BigDecimal("1"), null);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Multiple partial fills: A sell order being filled by multiple buy orders")
    void testMultiplePartialFills() throws Exception {
        OrderRequest sellOrderRequest = TestUtils.orderRequest().sell().withAmount(3).build();
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(3));

        OrderRequest buyOrderRequest1 = TestUtils.orderRequest().buy().withAmount(1).build();
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.amount").value(1))
                .andExpect(jsonPath("$.pendingAmount").value(0))
                .andExpect(jsonPath("$.trades", hasSize(1)));

        // Check if  the sell order was partially filled
        mockMvc.perform(get("/orders/{orderId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAmount").value(2))
                .andExpect(jsonPath("$.trades", hasSize(1)));

        OrderRequest buyOrderRequest2 = TestUtils.orderRequest().buy().withAmount(2).build();
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyOrderRequest2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.amount").value(2))
                .andExpect(jsonPath("$.pendingAmount").value(0))
                .andExpect(jsonPath("$.trades", hasSize(1)));

        // Check if  the sell order is now fully filled
        mockMvc.perform(get("/orders/{orderId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingAmount").value(0))
                .andExpect(jsonPath("$.trades", hasSize(2)))
                .andExpect(jsonPath("$.trades[0].orderId").value(2))
                .andExpect(jsonPath("$.trades[1].orderId").value(3));
    }

    @Test
    @DisplayName("Edge case: Creating an order with very large price and amount")
    void testVeryLargeNumbers() throws Exception {
        OrderRequest orderRequest = TestUtils.orderRequest()
                .withPrice(new BigDecimal("9999999999999.99"))
                .withAmount(new BigDecimal("9999999999999.99"))
                .build();

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(9999999999999.99))
                .andExpect(jsonPath("$.amount").value(9999999999999.99));
    }

    @Test
    @DisplayName("Edge case: Creating an order with very small decimal amount")
    void testVerySmallDecimalAmount() throws Exception {
        OrderRequest orderRequest = TestUtils.orderRequest()
                .withAmount(new BigDecimal("0.0000001"))
                .build();

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(0.0000001));
    }


    @Nested
    @DisplayName("Complex Matching (Price/Time Priority)")
    class ComplexMatchingPriceTimePriority {

        @Test
        @DisplayName("Complex matching with price/time priority")
        void complexMatchingWithPriceTimePriority() throws Exception {

            OrderRequest sellOrder1 = TestUtils.orderRequest()
                    .withAsset("TST")
                    .withPrice(10.05)
                    .withAmount(20)
                    .sell()
                    .build();
            mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sellOrder1)))
                    .andExpect(status().isCreated());

            // Order 2: lower price, should be matched first
            OrderRequest sellOrder2 = TestUtils.orderRequest()
                    .withAsset("TST")
                    .withPrice(10.04)
                    .withAmount(20)
                    .sell()
                    .build();
            mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sellOrder2)))
                    .andExpect(status().isCreated());

            // Order 3: same price as Order 1, but later timestamp
            OrderRequest sellOrder3 = TestUtils.orderRequest()
                    .withAsset("TST")
                    .withPrice(10.05)
                    .withAmount(40)
                    .sell()
                    .build();
            mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(sellOrder3)))
                    .andExpect(status().isCreated());

            // Order 4: no match with any sell order
            OrderRequest buyOrder1 = TestUtils.orderRequest()
                    .withAsset("TST")
                    .withPrice(10.00)
                    .withAmount(20)
                    .buy()
                    .build();
            mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buyOrder1)))
                    .andExpect(status().isCreated());

            // Order 5: no match with any sell order
            OrderRequest buyOrder2 = TestUtils.orderRequest()
                    .withAsset("TST")
                    .withPrice(10.02)
                    .withAmount(40)
                    .buy()
                    .build();
            mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buyOrder2)))
                    .andExpect(status().isCreated());

            // Order 6: no match with any sell order
            OrderRequest buyOrder3 = TestUtils.orderRequest()
                    .withAsset("TST")
                    .withPrice(10.00)
                    .withAmount(40)
                    .buy()
                    .build();
            mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(buyOrder3)))
                    .andExpect(status().isCreated());

            // Order 7: should match with orders 2, 1, and 3 in that order
            OrderRequest matchingBuyOrder = TestUtils.orderRequest()
                    .withAsset("TST")
                    .withPrice(10.06)
                    .withAmount(55)
                    .buy()
                    .build();

            // This should match with orders 2, 1, and 3 in that order: price, then time priority
            mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(matchingBuyOrder)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(7))
                    .andExpect(jsonPath("$.pendingAmount").value(0.0))
                    .andExpect(jsonPath("$.trades", hasSize(3)))
                    .andExpect(jsonPath("$.trades[0].orderId").value(2))
                    .andExpect(jsonPath("$.trades[0].amount").value(20))
                    .andExpect(jsonPath("$.trades[0].price").value(10.04))
                    .andExpect(jsonPath("$.trades[1].orderId").value(1))
                    .andExpect(jsonPath("$.trades[1].amount").value(20))
                    .andExpect(jsonPath("$.trades[1].price").value(10.05))
                    .andExpect(jsonPath("$.trades[2].orderId").value(3))
                    .andExpect(jsonPath("$.trades[2].amount").value(15))
                    .andExpect(jsonPath("$.trades[2].price").value(10.05));

            // Order 3 should be partially filled: 25 remaining out of 40
            mockMvc.perform(get("/orders/{orderId}", 3)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(3))
                    .andExpect(jsonPath("$.pendingAmount").value(25))
                    .andExpect(jsonPath("$.trades", hasSize(1)))
                    .andExpect(jsonPath("$.trades[0].orderId").value(7))
                    .andExpect(jsonPath("$.trades[0].amount").value(15))
                    .andExpect(jsonPath("$.trades[0].price").value(10.05));

            // Orders 1 and 2 should be fully filled
            mockMvc.perform(get("/orders/{orderId}", 1)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingAmount").value(0.0));

            mockMvc.perform(get("/orders/{orderId}", 2)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pendingAmount").value(0.0));
        }
    }

    @Nested
    @DisplayName("Multi-Step Bitcoin Trade Lifecycle")
    class MultiStepBitcoinTradeLifecycle {

        @Test
        @DisplayName("Multi-step Bitcoin trade lifecycle")
        void multiStepBitcoinTradeLifecycle() throws Exception {

            OrderRequest initialSellOrder = TestUtils.orderRequest()
                    .withAsset("BTC")
                    .withPrice(43251.00)
                    .withAmount(1.0)
                    .sell()
                    .build();

            mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(initialSellOrder)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.pendingAmount").value(1.0))
                    .andExpect(jsonPath("$.trades", hasSize(0)));

            // Place non-matching BUY order
            OrderRequest nonMatchingBuyOrder = TestUtils.orderRequest()
                    .withAsset("BTC")
                    .withPrice(43250.00)
                    .withAmount(0.25)
                    .buy()
                    .build();

            mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(nonMatchingBuyOrder)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.pendingAmount").value(0.25))
                    .andExpect(jsonPath("$.trades", hasSize(0)));

            // Place first matching BUY order
            OrderRequest firstMatchingBuyOrder = TestUtils.orderRequest()
                    .withAsset("BTC")
                    .withPrice(43253.00)
                    .withAmount(0.35)
                    .buy()
                    .build();

            mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(firstMatchingBuyOrder)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(3))
                    .andExpect(jsonPath("$.pendingAmount").value(0.0))
                    .andExpect(jsonPath("$.trades", hasSize(1)))
                    .andExpect(jsonPath("$.trades[0].orderId").value(1))
                    .andExpect(jsonPath("$.trades[0].amount").value(0.35))
                    .andExpect(jsonPath("$.trades[0].price").value(43251.00));

            // Verify state of original SELL order. It should be partially filled.
            mockMvc.perform(get("/orders/{orderId}", 1)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.pendingAmount").value(0.65))
                    .andExpect(jsonPath("$.trades", hasSize(1)))
                    .andExpect(jsonPath("$.trades[0].orderId").value(3))
                    .andExpect(jsonPath("$.trades[0].amount").value(0.35))
                    .andExpect(jsonPath("$.trades[0].price").value(43251.00));

            // Place final BUY order to complete the match
            OrderRequest finalBuyOrder = TestUtils.orderRequest()
                    .withAsset("BTC")
                    .withPrice(43251.00)
                    .withAmount(0.65)
                    .buy()
                    .build();

            mockMvc.perform(post("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(finalBuyOrder)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(4))
                    .andExpect(jsonPath("$.pendingAmount").value(0.0))
                    .andExpect(jsonPath("$.trades", hasSize(1)))
                    .andExpect(jsonPath("$.trades[0].orderId").value(1))
                    .andExpect(jsonPath("$.trades[0].amount").value(0.65))
                    .andExpect(jsonPath("$.trades[0].price").value(43251.00));

            // Verify final state of original SELL order. Now it should be fully filled
            mockMvc.perform(get("/orders/{orderId}", 1)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.pendingAmount").value(0.0))
                    .andExpect(jsonPath("$.trades", hasSize(2)))
                    .andExpect(jsonPath("$.trades[0].orderId").value(3))
                    .andExpect(jsonPath("$.trades[0].amount").value(0.35))
                    .andExpect(jsonPath("$.trades[0].price").value(43251.00))
                    .andExpect(jsonPath("$.trades[1].orderId").value(4))
                    .andExpect(jsonPath("$.trades[1].amount").value(0.65))
                    .andExpect(jsonPath("$.trades[1].price").value(43251.00));
        }
    }
}
