package com.engine.baraka.model;

import java.math.BigDecimal;

public record Trade(
    long orderId,
    BigDecimal amount,
    BigDecimal price
) {}