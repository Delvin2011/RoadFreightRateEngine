package com.vantageit.road_freight_rate_engine.rateengine.servicelevel;

import java.time.LocalDate;
import lombok.Getter;

/**
 * A flagged accessorial (after-hours, tail lift, driver assist) has no active
 * {@code surcharge_rates} row for the given date. Never silently skipped or defaulted to zero —
 * that would produce an incomplete quote without any indication something was missing.
 */
@Getter
public class AccessorialRateNotFoundException extends RuntimeException {

    private final String surchargeCode;
    private final LocalDate asOfDate;

    public AccessorialRateNotFoundException(String surchargeCode, LocalDate asOfDate) {
        super("No active accessorial surcharge rate for code=%s as of %s".formatted(surchargeCode, asOfDate));
        this.surchargeCode = surchargeCode;
        this.asOfDate = asOfDate;
    }
}
