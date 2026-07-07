package com.vantageit.road_freight_rate_engine.rateengine.rates.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Creates a new surcharge rate row. Same "no in-place update" invariant as
 * {@link RoadFreightRateRequest} — see {@code SurchargeRateController}'s expire endpoint.
 */
public record SurchargeRateRequest(

        @NotBlank(message = "Surcharge code is required")
        @Size(max = 50, message = "Surcharge code must be at most 50 characters")
        String surchargeCode,

        /** Wire value, e.g. {@code "pct_of_base"}. */
        @NotBlank(message = "Surcharge type is required")
        String surchargeType,

        /** Percentage as decimal (0.2200 = 22%) when surchargeType is pct_of_base; a flat/unit amount otherwise. */
        @NotNull(message = "Rate value is required")
        BigDecimal rateValue,

        @NotBlank(message = "Currency is required")
        @Size(max = 3, message = "Currency must be at most 3 characters")
        String currency,

        /** Comma-delimited vehicle_categories.code list. Null means all vehicle categories. */
        String appliesToVehicleCategories,

        /** Comma-delimited Stage 1 CargoClass wire values. Null means all cargo classes. */
        String appliesToCargoClasses,

        /** Wire value, e.g. {@code "both"}. */
        @NotBlank(message = "Applies-to-route-types is required")
        String appliesToRouteTypes,

        @NotNull(message = "Effective-from date is required")
        LocalDate effectiveFrom,

        @NotNull(message = "Created-by id is required")
        UUID createdBy
) {
}
