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

    /** @param minimumChargeApplied true when {@code minimumCharge} raised the raw computed amount */
    public record CostEstimate(BigDecimal amount, boolean minimumChargeApplied) {
    }

    public static BigDecimal estimate(
            RoadFreightRate rate, BigDecimal chargeableWeightKg, BigDecimal volumeCbm, BigDecimal distanceKm, Integer palletCount) {
        return estimateWithDetail(rate, chargeableWeightKg, volumeCbm, distanceKm, palletCount).amount();
    }

    /**
     * Same computation as {@link #estimate}, but also reports whether {@code minimumCharge}
     * changed the result — Stage 6 needs this to populate {@code minimumChargeApplied} and the
     * "Minimum charge applied." line-item comment without recomputing the raw amount separately.
     */
    public static CostEstimate estimateWithDetail(
            RoadFreightRate rate, BigDecimal chargeableWeightKg, BigDecimal volumeCbm, BigDecimal distanceKm, Integer palletCount) {
        BigDecimal raw = switch (rate.getRateBasis()) {
            // distance_km * rate_per_km * (chargeable_weight_kg / 1000) — the weight factor was
            // previously missing here entirely (bug found during Stage 6's audit of this formula
            // against the architecture spec; fixed rather than left, since this method was already
            // used by Stage 5's cost-efficient vehicle comparison).
            case PER_KM -> rate.getRateValue().multiply(distanceKm).multiply(chargeableWeightKg).divide(KG_PER_TON, 4, RoundingMode.HALF_UP);
            case PER_TON -> rate.getRateValue().multiply(chargeableWeightKg).divide(KG_PER_TON, 4, RoundingMode.HALF_UP);
            case FLAT -> rate.getRateValue();
            // Also the LTL formula (pallet_count * rate_per_pallet_per_lane) — in current seed
            // data only LTL lanes are seeded with a PER_PALLET row, so this same case ends up
            // handling both, with no separate LTL code path anywhere in the pipeline. This is
            // NOT a schema-enforced or documented invariant: rate_basis and load_type are two
            // fully independent CHECK constraints on road_freight_rates, so nothing stops a rate
            // admin from seeding a PER_PALLET row against a different load_type (e.g. a
            // palletized FLATBED load priced per pallet) — and the formula below is genuinely
            // load-type-agnostic, so that would compute correctly too. Don't read "LTL" as a
            // precondition of this case; it's just the only load_type that happens to use it today.
            case PER_PALLET -> rate.getRateValue().multiply(BigDecimal.valueOf(palletCount == null ? 0 : palletCount));
            case PER_CBM -> rate.getRateValue().multiply(volumeCbm);
        };

        BigDecimal minimumCharge = rate.getMinimumCharge();
        // Floor (max), not cap (min): the Business Rules tab's literal wording for the LTL minimum
        // charge says "min(...)", but that contradicts both the field's own name and the general
        // minimum-charge floor rule stated for every other rate_basis in the same document —
        // treated as a documentation error, not followed literally. Applied uniformly here for
        // every rate_basis, including PER_PALLET/LTL.
        if (minimumCharge != null && raw.compareTo(minimumCharge) < 0) {
            return new CostEstimate(minimumCharge, true);
        }
        return new CostEstimate(raw, false);
    }
}
