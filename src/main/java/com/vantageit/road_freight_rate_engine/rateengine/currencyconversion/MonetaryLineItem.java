package com.vantageit.road_freight_rate_engine.rateengine.currencyconversion;

import java.math.BigDecimal;

/**
 * Generic input value object, deliberately decoupled from any specific upstream stage — same
 * reasoning as Stage 7's {@code PreMultiplierTotals}. This stage doesn't know or care whether a
 * given item came from Stage 6's base freight, Stage 7's accessorials, or any other source.
 */
public record MonetaryLineItem(
        String code,
        String description,
        BigDecimal buyAmount,
        BigDecimal sellAmount,
        String currency
) {
}
