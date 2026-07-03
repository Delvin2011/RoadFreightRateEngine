package com.vantageit.road_freight_rate_engine.rateengine.currencyconversion;

import java.time.LocalDate;
import lombok.Getter;

/**
 * No {@code currency_exchange_rates} row exists with {@code rate_date} on or before
 * {@code asOfDate} for the requested currency pair — never silently skipped or defaulted to an
 * assumed rate, since that would produce an incorrect converted amount.
 */
@Getter
public class ExchangeRateNotFoundException extends RuntimeException {

    private final String fromCurrency;
    private final String toCurrency;
    private final LocalDate asOfDate;

    public ExchangeRateNotFoundException(String fromCurrency, String toCurrency, LocalDate asOfDate) {
        super("No exchange rate found for %s->%s on or before %s".formatted(fromCurrency, toCurrency, asOfDate));
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.asOfDate = asOfDate;
    }
}
