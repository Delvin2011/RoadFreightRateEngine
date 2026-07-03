package com.vantageit.road_freight_rate_engine.rateengine.pipeline.common;

import java.math.BigDecimal;

/**
 * chargeable_weight_kg = max(gross_weight_kg, volume_cbm * ROAD_VOLUMETRIC_FACTOR). Shared by
 * Stage 1 (input validation), Stage 5 (vehicle selection) and Stage 6 (base freight) — don't
 * duplicate this calculation elsewhere.
 */
public final class ChargeableWeightCalculator {

    /** Road freight volumetric conversion factor (kg per m³), per the architecture spec. */
    public static final BigDecimal ROAD_VOLUMETRIC_FACTOR = new BigDecimal("333");

    private ChargeableWeightCalculator() {
    }

    public static BigDecimal compute(BigDecimal grossWeightKg, BigDecimal volumeCbm) {
        BigDecimal volumetricWeightKg = volumeCbm.multiply(ROAD_VOLUMETRIC_FACTOR);
        return grossWeightKg.max(volumetricWeightKg);
    }
}
