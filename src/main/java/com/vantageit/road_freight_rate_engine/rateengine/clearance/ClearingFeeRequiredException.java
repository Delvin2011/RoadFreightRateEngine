package com.vantageit.road_freight_rate_engine.rateengine.clearance;

import java.time.LocalDate;
import lombok.Getter;

/**
 * Per the Business Rules tab: a cross-border quote without a {@code BORDER_CLEARING_AGENT_FEE}
 * line item is itself an engine error, not just a missing optional charge. Deliberately a distinct,
 * more severe exception type from {@link com.vantageit.road_freight_rate_engine.rateengine.surcharge.SurchargeRateNotFoundException}
 * (reused from Stage 9's generic "rate not found" case) — this represents a business-rule
 * violation, not just a data gap.
 */
@Getter
public class ClearingFeeRequiredException extends RuntimeException {

    private final LocalDate asOfDate;

    public ClearingFeeRequiredException(LocalDate asOfDate) {
        super("Cross-border quote is missing the mandatory BORDER_CLEARING_AGENT_FEE line item for collection_date=" + asOfDate);
        this.asOfDate = asOfDate;
    }
}
