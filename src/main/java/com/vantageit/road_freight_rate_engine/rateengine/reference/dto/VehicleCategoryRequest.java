package com.vantageit.road_freight_rate_engine.rateengine.reference.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record VehicleCategoryRequest(

        @NotBlank(message = "Code is required")
        @Size(max = 50, message = "Code must be at most 50 characters")
        String code,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotNull(message = "Max weight (kg) is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Max weight (kg) must be greater than zero")
        BigDecimal maxWeightKg,

        @NotNull(message = "Max volume (cbm) is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Max volume (cbm) must be greater than zero")
        BigDecimal maxVolumeCbm,

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        /** Wire value, e.g. {@code "METRO_ONLY"}; null means no restriction. */
        String zoneRestriction,

        @NotNull(message = "Requires-permit flag is required")
        Boolean requiresPermit
) {
}
