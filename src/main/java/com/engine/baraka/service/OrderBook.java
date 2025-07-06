package com.engine.baraka.service;

import com.engine.baraka.model.Direction;
import com.engine.baraka.model.Order;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.concurrent.*;

/**
 * Represents an order book for a specific asset.
 * Contains two maps: one for bids (BUY orders) and one for asks (SELL orders).
 * Bids (BUY orders) sorted by price in descending order (highest price first)
 * Asks (SELL orders) sorted by price in ascending order (lowest price first)
 */
public class OrderBook {

    private final ConcurrentSkipListMap<BigDecimal, ConcurrentLinkedQueue<Order>> bids;
    private final ConcurrentSkipListMap<BigDecimal, ConcurrentLinkedQueue<Order>> asks;

    public OrderBook() {
        this.bids = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
        this.asks = new ConcurrentSkipListMap<>();
    }

    public void addOrder(Order order) {
        ConcurrentSkipListMap<BigDecimal, ConcurrentLinkedQueue<Order>> orderMap = 
            (order.getDirection() == Direction.BUY) ? bids : asks;
        
        BigDecimal price = order.getPrice();

        ConcurrentLinkedQueue<Order> ordersAtPrice = orderMap.computeIfAbsent(
            price, key -> new ConcurrentLinkedQueue<>());

        ordersAtPrice.add(order);
    }

    public ConcurrentSkipListMap<BigDecimal, ConcurrentLinkedQueue<Order>> getBids() {
        return bids;
    }
    public ConcurrentSkipListMap<BigDecimal, ConcurrentLinkedQueue<Order>> getAsks() {
        return asks;
    }

    public boolean removeOrder(Order order) {
        ConcurrentSkipListMap<BigDecimal, ConcurrentLinkedQueue<Order>> orderMap = 
            (order.getDirection() == Direction.BUY) ? bids : asks;
        
        BigDecimal price = order.getPrice();
        ConcurrentLinkedQueue<Order> ordersAtPrice = orderMap.get(price);
        
        if (ordersAtPrice == null) {
            return false;
        }

        boolean removed = ordersAtPrice.remove(order);

        // Remove the price level (the price entry in the map) if no orders remain at this price point.
        if (removed && ordersAtPrice.isEmpty()) {
            orderMap.remove(price);
        }

        return removed;
    }
}