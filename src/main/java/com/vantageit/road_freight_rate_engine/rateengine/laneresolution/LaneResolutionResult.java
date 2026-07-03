package com.vantageit.road_freight_rate_engine.rateengine.laneresolution;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * @param laneKey                 {@code ZONE_ORIGIN:ZONE_DEST}, built from the resolved zone codes
 * @param distanceOverrideApplied true when {@code distanceKm} came from the operator-supplied
 *                                 {@code route.distance_km} rather than the distance matrix
 */
public record LaneResolutionResult(
        String laneKey,
        BigDecimal distanceKm,
        UUID originZoneId,
        UUID destinationZoneId,
        boolean distanceOverrideApplied
) {
}
