package com.engine.baraka.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
public class Order {
    @EqualsAndHashCode.Include
    private final long id;
    private final Instant timestamp;
    private final String asset;
    private final BigDecimal price;
    private final BigDecimal amount;
    private final Direction direction;
    private BigDecimal pendingAmount;
    @Getter(AccessLevel.NONE)
    private final List<Trade> trades;


    public static Order of(long id, Instant timestamp, String asset, BigDecimal price, 
                BigDecimal amount, Direction direction) {
        return new Order(id, timestamp, asset, price, amount, direction, amount, new ArrayList<>());
    }

    public static Order createFromRequest(long id, OrderRequest request) {
        return createFromRequest(id, request, Instant.now());
    }

    public static Order createFromRequest(long id, OrderRequest request, Instant timestamp) {
        return of(id, timestamp, request.asset(), request.price(), 
             request.amount(), request.direction());
    }

    public void addTrade(Trade trade) {
        trades.add(trade);
        pendingAmount = pendingAmount.subtract(trade.amount());
    }

    public boolean isOrderFullyFilled() {
        return pendingAmount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Returns a defensive copy of the trades list to prevent external modification.
     * 
     * @return A copy of the trades list
     */
    public List<Trade> getTrades() {
        return new ArrayList<>(trades);
    }

}
