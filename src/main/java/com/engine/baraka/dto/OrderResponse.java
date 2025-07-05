package com.engine.baraka.dto;

import com.engine.baraka.model.Direction;
import com.engine.baraka.model.Trade;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


public record OrderResponse(
    long id,
    Instant timestamp,
    String asset,
    BigDecimal price,
    BigDecimal amount,
    Direction direction,
    BigDecimal pendingAmount,
    List<Trade> trades
) {}