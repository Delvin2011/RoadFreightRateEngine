package com.vantageit.road_freight_rate_engine.rateengine.reference.dto;

import java.util.UUID;

public record ZoneResponse(
        UUID id,
        String code,
        String name,
        Integer tier,
        String countryCode
) {
}
