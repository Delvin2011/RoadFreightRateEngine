package com.vantageit.road_freight_rate_engine.rateengine.vehicleselection;

import java.math.BigDecimal;
import lombok.Getter;

/** No vehicle category passed Phase 1 filtering (load type / capacity / zone restriction). */
@Getter
public class NoEligibleVehicleException extends RuntimeException {

    private final BigDecimal chargeableWeightKg;
    private final BigDecimal volumeCbm;
    private final String loadType;
    private final String laneKey;

    public NoEligibleVehicleException(BigDecimal chargeableWeightKg, BigDecimal volumeCbm, String loadType, String laneKey) {
        super("No eligible vehicle for load_type=%s, chargeable_weight_kg=%s, volume_cbm=%s on lane %s"
                .formatted(loadType, chargeableWeightKg, volumeCbm, laneKey));
        this.chargeableWeightKg = chargeableWeightKg;
        this.volumeCbm = volumeCbm;
        this.loadType = loadType;
        this.laneKey = laneKey;
    }
}
