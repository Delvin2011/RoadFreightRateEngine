package com.vantageit.road_freight_rate_engine.rateengine.vehicleselection;

import java.util.List;
import lombok.Getter;

/**
 * At least one vehicle was Phase-1-eligible, but none of them has an active
 * {@code road_freight_rates} row for this lane/load type — so cost-efficient selection has
 * nothing to compare.
 */
@Getter
public class NoRateAvailableForLaneException extends RuntimeException {

    private final String laneKey;
    private final String loadType;
    private final List<String> vehicleCategoryCodesConsidered;

    public NoRateAvailableForLaneException(String laneKey, String loadType, List<String> vehicleCategoryCodesConsidered) {
        super("No active rate for lane %s, load_type=%s among eligible vehicles: %s"
                .formatted(laneKey, loadType, vehicleCategoryCodesConsidered));
        this.laneKey = laneKey;
        this.loadType = loadType;
        this.vehicleCategoryCodesConsidered = vehicleCategoryCodesConsidered;
    }
}
