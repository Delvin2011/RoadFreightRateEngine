package com.vantageit.road_freight_rate_engine.rateengine.vehicleselection;

import java.math.BigDecimal;
import lombok.Getter;

/**
 * Phase 3 defensive check: the selected vehicle's actual capacity is exceeded by the chargeable
 * weight. Should be unreachable given Phase 1's capacity filter already excludes vehicles that
 * can't carry the load — this exists as a final invariant check, not a normal control-flow path.
 */
@Getter
public class VehicleCapacityExceededException extends RuntimeException {

    private final String vehicleCategoryCode;
    private final BigDecimal maxWeightKg;
    private final BigDecimal chargeableWeightKg;

    public VehicleCapacityExceededException(String vehicleCategoryCode, BigDecimal maxWeightKg, BigDecimal chargeableWeightKg) {
        super("Vehicle %s capacity %s kg exceeded by chargeable weight %s kg"
                .formatted(vehicleCategoryCode, maxWeightKg, chargeableWeightKg));
        this.vehicleCategoryCode = vehicleCategoryCode;
        this.maxWeightKg = maxWeightKg;
        this.chargeableWeightKg = chargeableWeightKg;
    }
}
