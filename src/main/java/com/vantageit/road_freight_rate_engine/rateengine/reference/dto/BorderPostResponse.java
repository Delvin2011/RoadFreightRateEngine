package com.vantageit.road_freight_rate_engine.rateengine.reference.dto;

import java.util.UUID;

public record BorderPostResponse(
        UUID id,
        String code,
        String name,
        String originCountry,
        String destinationCountry
) {
}
