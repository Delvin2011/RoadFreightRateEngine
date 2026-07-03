package com.vantageit.road_freight_rate_engine.rateengine.servicelevel;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import java.math.BigDecimal;
import java.util.List;

/**
 * @param multipliedSubtotal   {@code (baseFreight + surcharges + clearances) * multiplier}, rounded
 *                             to 2 decimal places
 * @param currency             carried through unconverted from {@link PreMultiplierTotals#currency()}
 *                             — every accessorial line item is confirmed to match this currency
 *                             (see {@link AccessorialChargeCalculator}) before this result is built
 * @param serviceLevelLineItem the multiplier's dollar impact as its own line item — {@code buy_zar
 *                             = 0}, {@code sell_zar} = the uplift amount, matching the API
 *                             contract's {@code SERVICE_MULTIPLIER} example
 * @param accessorialLineItems appended after the multiplier, never multiplied; empty (not null)
 *                             when no accessorial flags are set
 * @param runningTotal         {@code multipliedSubtotal + sum(accessorialLineItems.sell_zar)}
 */
public record ServiceLevelResult(
        BigDecimal multipliedSubtotal,
        String currency,
        LineItem serviceLevelLineItem,
        List<LineItem> accessorialLineItems,
        BigDecimal runningTotal
) {
}
