package com.vantageit.road_freight_rate_engine.rateengine.rates.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Shared by both rate tables' expire endpoint: closes out an existing row as of {@code expiryDate}. */
public record ExpireRequest(

        @NotNull(message = "Expiry date is required")
        LocalDate expiryDate
) {
}
