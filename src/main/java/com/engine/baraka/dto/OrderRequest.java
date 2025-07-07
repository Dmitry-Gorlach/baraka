package com.engine.baraka.dto;

import com.engine.baraka.model.Direction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record OrderRequest(
    @NotBlank(message = "Asset cannot be null or blank")
    String asset,

    @NotNull(message = "Price cannot be null")
    @Positive(message = "Price must be positive.")
    BigDecimal price,

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @NotNull(message = "Direction cannot be null")
    Direction direction
) {
}
