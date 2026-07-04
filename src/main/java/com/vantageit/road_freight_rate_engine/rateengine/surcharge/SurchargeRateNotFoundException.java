package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import java.time.LocalDate;
import lombok.Getter;

/**
 * A {@link SurchargeRule} was applicable but has no active {@code surcharge_rates} row for the
 * given date. Never silently skipped — that would produce an incomplete quote without indicating
 * a surcharge that should have applied was missing its rate.
 */
@Getter
public class SurchargeRateNotFoundException extends RuntimeException {

    private final String surchargeCode;
    private final LocalDate asOfDate;

    public SurchargeRateNotFoundException(String surchargeCode, LocalDate asOfDate) {
        super("No active surcharge rate for code=%s as of %s".formatted(surchargeCode, asOfDate));
        this.surchargeCode = surchargeCode;
        this.asOfDate = asOfDate;
    }
}
