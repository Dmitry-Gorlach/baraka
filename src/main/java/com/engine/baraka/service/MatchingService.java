package com.engine.baraka.service;

import com.engine.baraka.model.Order;
import com.engine.baraka.model.OrderRequest;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Matching service maintains order books for different assets and processes new orders.
 */
@Service
public class MatchingService {

    private final Map<String, OrderBook> orderBooksByAsset = new ConcurrentHashMap<>();
    private final Map<Long, Order> ordersById = new ConcurrentHashMap<>();
    private final AtomicLong orderIdGenerator = new AtomicLong(0);

    private OrderBook getOrCreateOrderBook(String asset) {
        return orderBooksByAsset.computeIfAbsent(asset, key -> new OrderBook());
    }

    public Order getOrderById(long orderId) {
        return ordersById.get(orderId);
    }

    public Order processNewOrder(OrderRequest request) {

        long orderId = orderIdGenerator.incrementAndGet();
        Order order = Order.createFromRequest(orderId, request);
        ordersById.put(orderId, order);

        OrderBook orderBook = getOrCreateOrderBook(request.asset());
        orderBook.addOrder(order);

        return order;
    }
}
