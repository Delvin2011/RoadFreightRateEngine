package com.vantageit.road_freight_rate_engine.rateengine.reference.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VehicleCategoryResponse(
        UUID id,
        String code,
        String name,
        BigDecimal maxWeightKg,
        BigDecimal maxVolumeCbm,
        String description,
        String zoneRestriction,
        boolean requiresPermit
) {
}
