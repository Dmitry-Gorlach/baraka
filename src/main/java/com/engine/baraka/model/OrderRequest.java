package com.engine.baraka.model;

import java.math.BigDecimal;

public record OrderRequest(
    String asset,
    BigDecimal price,
    BigDecimal amount,
    Direction direction
) {

    public boolean isValid() {
        return asset != null && !asset.isBlank() &&
               price != null && price.compareTo(BigDecimal.ZERO) > 0 &&
               amount != null && amount.compareTo(BigDecimal.ZERO) > 0 &&
               direction != null;
    }
}