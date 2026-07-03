package com.vantageit.road_freight_rate_engine.rateengine.pipeline.common;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RoadFreightRate;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estimates the cost of a {@link RoadFreightRate} for a given shipment, per its {@code rateBasis}
 * (per_km/per_ton/flat/per_pallet/per_cbm), applying {@code minimumCharge} as a floor when set.
 *
 * <p>Not the authoritative base-freight computation (that's Stage 6) — this exists so Stage 5 can
 * rank eligible vehicles by cost. Placed here, not in {@code ...vehicleselection}, because Stage 6
 * needs the identical per-rate_basis math and should reuse it rather than duplicate it.
 *
 * <p>Assumes same-currency comparison: no cross-currency conversion is applied here. All rates in
 * the current seed data are ZAR; if a lane ever has candidate rates in different currencies, this
 * will need FX conversion before comparing, which isn't implemented yet.
 */
public final class ShipmentCostEstimator {

    private static final BigDecimal KG_PER_TON = new BigDecimal("1000");

    private ShipmentCostEstimator() {
    }

    public static BigDecimal estimate(
            RoadFreightRate rate, BigDecimal chargeableWeightKg, BigDecimal volumeCbm, BigDecimal distanceKm, Integer palletCount) {
        BigDecimal raw = switch (rate.getRateBasis()) {
            case PER_KM -> rate.getRateValue().multiply(distanceKm);
            case PER_TON -> rate.getRateValue().multiply(chargeableWeightKg).divide(KG_PER_TON, 4, RoundingMode.HALF_UP);
            case FLAT -> rate.getRateValue();
            case PER_PALLET -> rate.getRateValue().multiply(BigDecimal.valueOf(palletCount == null ? 0 : palletCount));
            case PER_CBM -> rate.getRateValue().multiply(volumeCbm);
        };

        BigDecimal minimumCharge = rate.getMinimumCharge();
        return minimumCharge != null ? raw.max(minimumCharge) : raw;
    }
}
