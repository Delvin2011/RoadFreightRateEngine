package com.vantageit.road_freight_rate_engine.rateengine.reference.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record LaneDistanceRequest(

        @NotNull(message = "Origin zone id is required")
        UUID originZoneId,

        @NotNull(message = "Destination zone id is required")
        UUID destinationZoneId,

        /** Null means a domestic lane. */
        UUID borderPostId,

        @NotNull(message = "Distance (km) is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Distance (km) must be greater than zero")
        BigDecimal distanceKm,

        @NotNull(message = "Active flag is required")
        Boolean active
) {
}
