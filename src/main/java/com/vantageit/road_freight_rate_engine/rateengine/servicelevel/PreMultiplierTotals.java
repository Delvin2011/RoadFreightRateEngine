package com.vantageit.road_freight_rate_engine.rateengine.servicelevel;

import java.math.BigDecimal;

/**
 * The sums this stage depends on but doesn't compute itself. {@code surchargesTotal} and
 * {@code clearancesTotal} will be {@link BigDecimal#ZERO} until the surcharges (pipeline Stage 5)
 * and clearances (pipeline Stage 6) stages exist in this codebase — this record makes no
 * distinction between "genuinely zero" and "not built yet"; both are just valid zero input.
 *
 * @param currency single-currency assumption — conversion is Stage 8's job, not this one's
 */
public record PreMultiplierTotals(
        BigDecimal baseFreightAmount,
        BigDecimal surchargesTotal,
        BigDecimal clearancesTotal,
        String currency
) {
    public BigDecimal sum() {
        return baseFreightAmount.add(surchargesTotal).add(clearancesTotal);
    }
}
