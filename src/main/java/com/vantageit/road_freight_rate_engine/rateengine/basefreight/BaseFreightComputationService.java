package com.vantageit.road_freight_rate_engine.rateengine.basefreight;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RoadFreightRate;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionResult;
import com.vantageit.road_freight_rate_engine.rateengine.pipeline.common.ChargeableWeightCalculator;
import com.vantageit.road_freight_rate_engine.rateengine.pipeline.common.ShipmentCostEstimator;
import com.vantageit.road_freight_rate_engine.rateengine.vehicleselection.VehicleSelectionResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pipeline Stage 4: base freight rate computation.
 *
 * <p><b>Precondition: the request must have already passed Stage 3 validation, Stage 4 lane
 * resolution, and Stage 5 vehicle selection.</b> This service does not re-run any of those.
 *
 * <p>There is no separate LTL code path here: dispatch is entirely by the resolved
 * {@link RoadFreightRate}'s {@code rateBasis} (via {@link ShipmentCostEstimator}), not by
 * {@code cargo.load_type}. LTL's spec formula ({@code pallet_count * rate_per_pallet_per_lane}) is
 * exactly {@code ShipmentCostEstimator}'s existing {@code PER_PALLET} case, so LTL lanes just need
 * to be seeded with a {@code PER_PALLET} row — this isn't a schema-enforced or documented
 * exclusivity rule, though: {@code rate_basis} and {@code load_type} are independent CHECK
 * constraints on {@code road_freight_rates}, so a {@code PER_PALLET} row could legitimately be
 * seeded against a different load type in future (the formula is load-type-agnostic and would
 * compute correctly). Trusting the DB row's {@code rate_basis} as authoritative — rather than
 * forcing per-pallet math whenever {@code load_type == LTL} — avoids silently mispricing a lane
 * whose rate row was seeded with a different basis than expected.
 */
@Service
@Transactional(readOnly = true)
public class BaseFreightComputationService {

    private final RateRowResolver rateRowResolver;

    public BaseFreightComputationService(RateRowResolver rateRowResolver) {
        this.rateRowResolver = rateRowResolver;
    }

    public BaseFreightResult compute(RateComputeRequest request, LaneResolutionResult lane, VehicleSelectionResult vehicle) {
        RoadFreightRate rate = rateRowResolver.resolve(request, lane, vehicle);
        CargoRequest cargo = request.cargo();
        BigDecimal chargeableWeightKg = ChargeableWeightCalculator.compute(cargo.grossWeightKg(), cargo.volumeCbm());

        ShipmentCostEstimator.CostEstimate estimate = ShipmentCostEstimator.estimateWithDetail(
                rate, chargeableWeightKg, cargo.volumeCbm(), lane.distanceKm(), cargo.palletCount());

        String lineItemComment = estimate.minimumChargeApplied() ? "Minimum charge applied." : null;

        // Rounded here, at the point the value becomes a final output (BaseFreightResult leaves
        // this service and ends up in a LineItem) — not inside ShipmentCostEstimator itself, since
        // Stage 5 also uses that class's estimate() for internal cost-efficient vehicle comparison,
        // which isn't a final output and shouldn't have this stage's rounding policy imposed on it.
        // Same class of bug as Stage 7's: PER_KM/PER_TON divide() already rounds to 4dp internally
        // (needed there for accurate minimum_charge floor comparison against the raw, unrounded
        // value — rounding before that comparison could change whether flooring applies at all),
        // but nothing previously rounded the *final* amount down to a valid 2dp currency value.
        BigDecimal baseFreightAmount = estimate.amount().setScale(2, RoundingMode.HALF_UP);

        return new BaseFreightResult(baseFreightAmount, rate.getCurrency(), rate.getRateBasis(), estimate.minimumChargeApplied(), lineItemComment);
    }
}
