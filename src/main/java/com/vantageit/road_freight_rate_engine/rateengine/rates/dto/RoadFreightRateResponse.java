package com.vantageit.road_freight_rate_engine.rateengine.rates.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RoadFreightRateResponse(
        UUID id,
        String laneKey,
        UUID vehicleCategoryId,
        String loadType,
        String rateBasis,
        BigDecimal rateValue,
        String currency,
        BigDecimal minimumCharge,
        BigDecimal maximumWeightKg,
        UUID carrierId,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        UUID createdBy,
        Instant createdAt,
        String versionTag
) {
}
