package com.engine.baraka.controller;

import com.engine.baraka.model.Direction;
import com.engine.baraka.model.OrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

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
    void createOrder_ShouldReturnCreatedOrderResponse() throws Exception {
        OrderRequest orderRequest = new OrderRequest("BTC", new BigDecimal("50000"), new BigDecimal("1"), Direction.BUY);

        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.asset").value("BTC"))
                .andExpect(jsonPath("$.price").value(50000))
                .andExpect(jsonPath("$.amount").value(1))
                .andExpect(jsonPath("$.direction").value("BUY"))
                .andExpect(jsonPath("$.pendingAmount").value(1))
                .andExpect(jsonPath("$.trades").isArray())
                .andExpect(jsonPath("$.trades").isEmpty());
    }

    @Test
    void getOrderById_WhenOrderExists_ShouldReturnOrderResponse() throws Exception {
        OrderRequest orderRequest = new OrderRequest("BTC", new BigDecimal("50000"), new BigDecimal("1"), Direction.BUY);
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/orders/{orderId}", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.asset").value("BTC"))
                .andExpect(jsonPath("$.price").value(50000))
                .andExpect(jsonPath("$.amount").value(1))
                .andExpect(jsonPath("$.direction").value("BUY"))
                .andExpect(jsonPath("$.pendingAmount").value(1))
                .andExpect(jsonPath("$.trades").isArray())
                .andExpect(jsonPath("$.trades").isEmpty());
    }

    @Test
    void getOrderById_WhenOrderDoesNotExist_ShouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/orders/{orderId}", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
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
}
