package com.vantageit.road_freight_rate_engine.rateengine.reference.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record LocationRequest(

        @NotNull(message = "Zone id is required")
        UUID zoneId,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Size(max = 500, message = "Address must be at most 500 characters")
        String address,

        @Size(max = 30, message = "Location type must be at most 30 characters")
        String locationType
) {
}
