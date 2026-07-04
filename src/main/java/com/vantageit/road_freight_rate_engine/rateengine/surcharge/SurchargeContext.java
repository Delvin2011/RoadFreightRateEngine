package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.basefreight.BaseFreightResult;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionResult;

/**
 * Bundles everything {@link SurchargeRule}s need, consuming Stage 4/6's already-computed outputs
 * rather than reaching back into their services — same decoupling discipline as Stage 7's
 * {@code PreMultiplierTotals} and Stage 8's {@code MonetaryLineItem}.
 */
public record SurchargeContext(
        RateComputeRequest request,
        BaseFreightResult baseFreightResult,
        LaneResolutionResult lane
) {
}
