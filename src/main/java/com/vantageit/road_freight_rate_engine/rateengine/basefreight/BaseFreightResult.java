package com.vantageit.road_freight_rate_engine.rateengine.basefreight;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RateBasis;
import java.math.BigDecimal;

/**
 * @param baseFreightAmount always rounded to exactly 2 decimal places ({@link
 *                           java.math.RoundingMode#HALF_UP}) — {@code ShipmentCostEstimator}'s
 *                           divide()-based formulas carry 4dp internally (needed for correct
 *                           minimum_charge floor comparison against the raw value), but this is
 *                           the point that value becomes a final output
 * @param currency          carried through unconverted from the matched rate row — FX conversion,
 *                           if any, is a later pipeline stage's job, not this one's
 * @param lineItemComment   {@code "Minimum charge applied."} exactly when {@code minimumChargeApplied},
 *                           per the Business Rules tab's audit requirement; null otherwise
 */
public record BaseFreightResult(
        BigDecimal baseFreightAmount,
        String currency,
        RateBasis rateBasisUsed,
        boolean minimumChargeApplied,
        String lineItemComment
) {
}
