package com.vantageit.road_freight_rate_engine.rateengine.reference.dto;

import java.util.UUID;

public record VehicleCategoryLoadTypeResponse(
        UUID id,
        UUID vehicleCategoryId,
        String loadType
) {
}
