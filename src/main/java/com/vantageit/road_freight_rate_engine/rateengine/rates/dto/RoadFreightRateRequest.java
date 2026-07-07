package com.vantageit.road_freight_rate_engine.rateengine.rates.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Creates a new rate row. There is deliberately no update endpoint that mutates an existing row's
 * priced/dated fields — a rate change is always a new row (see {@link RoadFreightRateResponse});
 * closing out a superseded row is done via the dedicated expire endpoint instead.
 */
public record RoadFreightRateRequest(

        @NotBlank(message = "Lane key is required")
        @Size(max = 100, message = "Lane key must be at most 100 characters")
        String laneKey,

        @NotNull(message = "Vehicle category id is required")
        UUID vehicleCategoryId,

        @NotBlank(message = "Load type is required")
        @Size(max = 20, message = "Load type must be at most 20 characters")
        String loadType,

        /** Wire value, e.g. {@code "per_km"}. */
        @NotBlank(message = "Rate basis is required")
        String rateBasis,

        @NotNull(message = "Rate value is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Rate value must be greater than zero")
        BigDecimal rateValue,

        @NotBlank(message = "Currency is required")
        @Size(max = 3, message = "Currency must be at most 3 characters")
        String currency,

        BigDecimal minimumCharge,

        BigDecimal maximumWeightKg,

        /** Null means internal fleet. */
        UUID carrierId,

        @NotNull(message = "Effective-from date is required")
        LocalDate effectiveFrom,

        @NotNull(message = "Created-by id is required")
        UUID createdBy,

        @Size(max = 50, message = "Version tag must be at most 50 characters")
        String versionTag
) {
}
