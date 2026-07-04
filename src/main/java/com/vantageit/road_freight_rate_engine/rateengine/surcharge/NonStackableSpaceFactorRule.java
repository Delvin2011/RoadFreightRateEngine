package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LoadType;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Applies when {@code cargo.stackable == false} AND {@code cargo.load_type == LTL}. Per the doc:
 * "1.5× floor space calculation on pallet rate" — a multiplier on the pallet-rate portion of
 * BASE_FREIGHT specifically, not a flat/pct surcharge computed the same way as the others.
 *
 * <p><b>Design decision, flagged rather than silently assumed</b>: for LTL, Stage 6 dispatches
 * exclusively via the {@code PER_PALLET} {@code rate_basis} (see {@code BaseFreightComputationService}'s
 * Javadoc) — there is no other component mixed into BASE_FREIGHT for an LTL shipment, so "the
 * pallet-rate portion of BASE_FREIGHT" and BASE_FREIGHT itself are the same number, including when
 * {@code minimum_charge} floored it. Treating the actually-charged (possibly floored) BASE_FREIGHT
 * as the base — rather than reaching back into Stage 6's {@code RateRowResolver} to recompute a
 * theoretical pre-floor pallet-rate figure — keeps this stage decoupled from Stage 6's services
 * (consuming only {@link com.vantageit.road_freight_rate_engine.rateengine.basefreight.BaseFreightResult},
 * an already-computed output) and matches what the customer is actually being charged for the
 * pallet-rate portion, floor included.
 *
 * <p>{@code rate_value} on the {@code NON_STACKABLE_SPACE_FACTOR} row is the *incremental* fraction
 * (0.50) needed to reach the documented 1.5× total — the base 1× is already represented by
 * BASE_FREIGHT itself, so this surcharge line item is the extra 0.5×, not the full 1.5×.
 */
@Component
public class NonStackableSpaceFactorRule implements SurchargeRule {

    @Override
    public String surchargeCode() {
        return "NON_STACKABLE_SPACE_FACTOR";
    }

    @Override
    public boolean isApplicable(SurchargeContext context) {
        CargoRequest cargo = context.request().cargo();
        return Boolean.FALSE.equals(cargo.stackable()) && cargo.loadType() == LoadType.LTL;
    }

    @Override
    public LineItem compute(SurchargeContext context, SurchargeRate rateRow) {
        BigDecimal amount = context.baseFreightResult().baseFreightAmount()
                .multiply(rateRow.getRateValue())
                .setScale(2, RoundingMode.HALF_UP);
        return new LineItem("NON_STACKABLE_SPACE_FACTOR", "Non-stackable space factor", amount, amount);
    }
}
