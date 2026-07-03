package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload for {@code POST /api/v1/rate-engine/compute}.
 *
 * @param quoteContextId correlates this request with the originating quote
 */
public record RateComputeRequest(
        @JsonProperty("quote_context_id") UUID quoteContextId,
        @JsonProperty("rate_date") LocalDate rateDate,
        RouteRequest route,
        CargoRequest cargo,
        ServiceRequest service
) {
}
