package com.vantageit.road_freight_rate_engine.rateengine.reference.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BorderPostRequest(

        @NotBlank(message = "Code is required")
        @Size(max = 50, message = "Code must be at most 50 characters")
        String code,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "Origin country is required")
        @Size(max = 2, message = "Origin country must be at most 2 characters")
        String originCountry,

        @NotBlank(message = "Destination country is required")
        @Size(max = 2, message = "Destination country must be at most 2 characters")
        String destinationCountry
) {
}
