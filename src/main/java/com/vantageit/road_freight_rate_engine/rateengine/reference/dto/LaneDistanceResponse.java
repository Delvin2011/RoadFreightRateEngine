package com.vantageit.road_freight_rate_engine.rateengine.reference.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LaneDistanceResponse(
        UUID id,
        UUID originZoneId,
        UUID destinationZoneId,
        UUID borderPostId,
        BigDecimal distanceKm,
        boolean active,
        Instant createdAt
) {
}
