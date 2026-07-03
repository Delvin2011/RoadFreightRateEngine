package com.vantageit.road_freight_rate_engine.rateengine.basefreight;

import lombok.Getter;

/**
 * No active {@code road_freight_rates} row matches the lane/vehicle/load type. Should be rare for
 * a non-dedicated selection (Stage 5 already excludes rate-less vehicles there), but
 * {@code dedicated_vehicle = true} bypasses that check, so this path is genuinely reachable.
 */
@Getter
public class RateNotFoundException extends RuntimeException {

    private final String laneKey;
    private final String vehicleCategoryCode;
    private final String loadType;

    public RateNotFoundException(String laneKey, String vehicleCategoryCode, String loadType) {
        super("No active rate for lane %s, vehicle_category=%s, load_type=%s".formatted(laneKey, vehicleCategoryCode, loadType));
        this.laneKey = laneKey;
        this.vehicleCategoryCode = vehicleCategoryCode;
        this.loadType = loadType;
    }
}
