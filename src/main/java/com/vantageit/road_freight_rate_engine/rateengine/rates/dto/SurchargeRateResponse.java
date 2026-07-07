package com.vantageit.road_freight_rate_engine.rateengine.rates.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SurchargeRateResponse(
        UUID id,
        String surchargeCode,
        String surchargeType,
        BigDecimal rateValue,
        String currency,
        String appliesToVehicleCategories,
        String appliesToCargoClasses,
        String appliesToRouteTypes,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        boolean active,
        UUID createdBy,
        Instant createdAt
) {
}
