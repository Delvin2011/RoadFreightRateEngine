package com.vantageit.road_freight_rate_engine.items.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ItemResponse(
        Long id,
        String name,
        String description,
        Integer quantity,
        BigDecimal price,
        Instant createdAt,
        Instant updatedAt
) {
}
