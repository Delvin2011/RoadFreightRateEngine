package com.vantageit.road_freight_rate_engine.rateengine.currencyconversion;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @param convertedLineItems every input {@link MonetaryLineItem}, now all in ZAR, in Stage 1's
 *                           {@link LineItem} shape
 * @param exchangeRatesUsed  keyed like the API contract example (e.g. {@code "USD_ZAR" -> 18.52}) —
 *                           only populated for currency pairs actually used, not every rate in
 *                           {@code currency_exchange_rates}
 */
public record CurrencyConversionResult(
        List<LineItem> convertedLineItems,
        Map<String, BigDecimal> exchangeRatesUsed
) {
}
