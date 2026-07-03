package com.vantageit.road_freight_rate_engine.rateengine.servicelevel;

import lombok.Getter;

/**
 * A flagged accessorial's {@code surcharge_rates} row is denominated in a different currency than
 * {@link PreMultiplierTotals#currency()}. Thrown rather than silently summed together — mixing
 * currencies within this stage's own running total is wrong regardless of what happens downstream;
 * {@code CURRENCY_FACTOR} (pipeline Stage 8) is a single multiplier applied to the whole final
 * total, not a mechanism for reconciling a mixture of currencies within one stage's inputs.
 * Actually converting between valid currencies elsewhere in the pipeline remains Stage 8's job —
 * this exception only refuses to silently proceed with an internally inconsistent sum.
 */
@Getter
public class AccessorialCurrencyMismatchException extends RuntimeException {

    private final String surchargeCode;
    private final String expectedCurrency;
    private final String actualCurrency;

    public AccessorialCurrencyMismatchException(String surchargeCode, String expectedCurrency, String actualCurrency) {
        super("Accessorial surcharge %s is denominated in %s, but expected %s (from PreMultiplierTotals.currency)"
                .formatted(surchargeCode, actualCurrency, expectedCurrency));
        this.surchargeCode = surchargeCode;
        this.expectedCurrency = expectedCurrency;
        this.actualCurrency = actualCurrency;
    }
}
