package com.vantageit.road_freight_rate_engine.rateengine.laneresolution;

import java.util.UUID;
import lombok.Getter;

/** No matching row in {@code lane_distances} for the resolved zone pair (and border post, if cross-border). */
@Getter
public class DistanceNotFoundException extends RuntimeException {

    private final String laneKey;
    private final UUID borderPostId;

    public DistanceNotFoundException(String laneKey, UUID borderPostId) {
        super("No distance found for lane %s%s".formatted(
                laneKey, borderPostId == null ? " (domestic)" : " via border post " + borderPostId));
        this.laneKey = laneKey;
        this.borderPostId = borderPostId;
    }
}
