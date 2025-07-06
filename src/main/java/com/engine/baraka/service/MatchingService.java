package com.engine.baraka.service;

import com.engine.baraka.model.Direction;
import com.engine.baraka.model.Order;
import com.engine.baraka.model.OrderRequest;
import com.engine.baraka.model.Trade;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
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
        return orderBooksByAsset.computeIfAbsent(asset,
                key -> new OrderBook());
    }

    public Order getOrderById(long orderId) {
        return ordersById.get(orderId);
    }

    public Order processNewOrder(OrderRequest request, Instant timestamp) {
        validateOrderRequest(request);

        long orderId = orderIdGenerator.incrementAndGet();
        Order order = (timestamp == null)
                ? Order.createFromRequest(orderId, request)
                : Order.createFromRequest(orderId, request, timestamp);
        ordersById.put(orderId, order);

        OrderBook orderBook = getOrCreateOrderBook(request.asset());

        if (order.getDirection() == Direction.BUY) {
            matchBuyOrder(order, orderBook);
        } else {
            matchSellOrder(order, orderBook);
        }

        if (!order.isOrderFullyFilled()) {
            orderBook.addOrder(order);
        }

        return order;
    }

    public Order processNewOrder(OrderRequest request) {
        return processNewOrder(request, null);
    }

    private void validateOrderRequest(OrderRequest request) {
        Objects.requireNonNull(request.asset(), "Asset cannot be null");
        Objects.requireNonNull(request.price(), "Price cannot be null");
        Objects.requireNonNull(request.amount(), "Amount cannot be null");
        Objects.requireNonNull(request.direction(), "Direction cannot be null");

        if (request.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive.");
        }
        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }
    }

    /**
     * Matches a BUY order against existing SELL orders in the order book.
     * Follows the Price/Time priority algorithm:
     * 1. Matches against SELL orders with the lowest price first
     * 2. For orders at the same price, matches against the oldest order first
     * 3. Continues matching until the BUY order is fully filled or there are no more matching SELL orders
     *
     * @param buyOrder  the BUY order to match
     * @param orderBook the order book containing SELL orders
     */
    private void matchBuyOrder(Order buyOrder, OrderBook orderBook) {
        while (!buyOrder.isOrderFullyFilled()) {
            Map.Entry<BigDecimal, ConcurrentLinkedQueue<Order>> bestAskEntry = orderBook.getAsks().firstEntry();

            if (bestAskEntry == null || bestAskEntry.getKey().compareTo(buyOrder.getPrice()) > 0) {
                break;
            }

            ConcurrentLinkedQueue<Order> ordersAtPrice = bestAskEntry.getValue();
            matchIncomingOrderAtPriceLevel(buyOrder, ordersAtPrice, bestAskEntry.getKey());

            if (ordersAtPrice.isEmpty()) {
                orderBook.getAsks().remove(bestAskEntry.getKey());
            }
        }
    }

    private void matchSellOrder(Order sellOrder, OrderBook orderBook) {
        while (!sellOrder.isOrderFullyFilled()) {
            Map.Entry<BigDecimal, ConcurrentLinkedQueue<Order>> bestBidEntry = orderBook.getBids().firstEntry();

            if (bestBidEntry == null || bestBidEntry.getKey().compareTo(sellOrder.getPrice()) < 0) {
                break;
            }

            ConcurrentLinkedQueue<Order> ordersAtPrice = bestBidEntry.getValue();
            matchIncomingOrderAtPriceLevel(sellOrder, ordersAtPrice, bestBidEntry.getKey());

            if (ordersAtPrice.isEmpty()) {
                orderBook.getBids().remove(bestBidEntry.getKey());
            }
        }
    }

    private void matchIncomingOrderAtPriceLevel(Order incomingOrder, ConcurrentLinkedQueue<Order> ordersAtPrice,
                                                BigDecimal tradePrice) {
        while (!incomingOrder.isOrderFullyFilled() && !ordersAtPrice.isEmpty()) {
            Order bookOrder = ordersAtPrice.peek();

            if (bookOrder == null) {
                // This is necessary because the queue might have been cleared by another thread, we try again.
                continue;
            }

            // This ensures atomic read and update of its pending amount.
            synchronized (bookOrder) {
                // Double-check if the order was already filled by another thread while this thread was waiting for the lock.
                if (bookOrder.isOrderFullyFilled()) {
                    ordersAtPrice.poll();
                    continue;
                }

                createTrade(incomingOrder, bookOrder, tradePrice);
            }

            if (bookOrder.isOrderFullyFilled()) {
                ordersAtPrice.poll();
            }

            if (incomingOrder.isOrderFullyFilled()) {
                break;
            }
        }
    }

    /**
     * Creates a trade between two orders and updates their pending amounts.
     */
    private void createTrade(Order incomingOrder, Order bookOrder, BigDecimal price) {
        BigDecimal tradeAmount = incomingOrder.getPendingAmount().min(bookOrder.getPendingAmount());

        if (tradeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        Trade incomingOrderTrade = new Trade(bookOrder.getId(), tradeAmount, price);
        Trade bookOrderTrade = new Trade(incomingOrder.getId(), tradeAmount, price);

        incomingOrder.addTrade(incomingOrderTrade);
        bookOrder.addTrade(bookOrderTrade);
    }
}
