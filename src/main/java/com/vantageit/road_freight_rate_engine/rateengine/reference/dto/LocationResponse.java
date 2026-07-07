package com.vantageit.road_freight_rate_engine.rateengine.reference.dto;

import java.time.Instant;
import java.util.UUID;

public record LocationResponse(
        UUID id,
        UUID zoneId,
        String name,
        String address,
        String locationType,
        Instant createdAt
) {
}
