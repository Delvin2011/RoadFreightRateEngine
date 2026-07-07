package com.vantageit.road_freight_rate_engine.rateengine.reference.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record VehicleCategoryLoadTypeRequest(

        @NotNull(message = "Vehicle category id is required")
        UUID vehicleCategoryId,

        /** Matches Stage 1's {@code LoadType} wire values (e.g. {@code "ftl"}). */
        @NotBlank(message = "Load type is required")
        @Size(max = 20, message = "Load type must be at most 20 characters")
        String loadType
) {
}
