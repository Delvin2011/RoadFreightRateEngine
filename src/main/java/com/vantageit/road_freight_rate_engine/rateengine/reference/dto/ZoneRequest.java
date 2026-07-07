package com.vantageit.road_freight_rate_engine.rateengine.reference.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ZoneRequest(

        @NotBlank(message = "Code is required")
        @Size(max = 50, message = "Code must be at most 50 characters")
        String code,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotNull(message = "Tier is required")
        Integer tier,

        @NotBlank(message = "Country code is required")
        @Size(max = 2, message = "Country code must be at most 2 characters")
        String countryCode
) {
}
