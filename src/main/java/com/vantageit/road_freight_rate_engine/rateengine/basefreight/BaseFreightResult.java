package com.vantageit.road_freight_rate_engine.rateengine.basefreight;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RateBasis;
import java.math.BigDecimal;

/**
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
