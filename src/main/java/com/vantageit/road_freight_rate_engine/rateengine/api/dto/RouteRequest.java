package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * @param borderPostId only relevant when {@code routeType} is {@link RouteType#CROSS_BORDER}
 * @param distanceKm   when null, the engine falls back to its distance matrix
 */
public record RouteRequest(
        @JsonProperty("origin_location_id") UUID originLocationId,
        @JsonProperty("destination_location_id") UUID destinationLocationId,
        @JsonProperty("route_type") RouteType routeType,
        @JsonProperty("border_post_id") UUID borderPostId,
        @JsonProperty("distance_km") BigDecimal distanceKm,
        @JsonProperty("collection_address_type") AddressType collectionAddressType,
        @JsonProperty("delivery_address_type") AddressType deliveryAddressType
) {
}
