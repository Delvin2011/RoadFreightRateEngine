package com.vantageit.road_freight_rate_engine.rateengine.currencyconversion;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.CurrencyExchangeRate;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.CurrencyExchangeRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Converts every non-ZAR {@link MonetaryLineItem} to ZAR. ZAR is hardcoded as the target currency
 * <b>here</b>, in this Phase 1 service logic — not in the {@code currency_exchange_rates} schema
 * itself, which models {@code to_currency} as a real column (see {@code V20}'s comment) so a
 * future non-ZAR target doesn't need a schema change, only a service change.
 */
@Service
@Transactional(readOnly = true)
public class CurrencyConversionService {

    private static final String TARGET_CURRENCY = "ZAR";

    private final CurrencyExchangeRateRepository currencyExchangeRateRepository;

    public CurrencyConversionService(CurrencyExchangeRateRepository currencyExchangeRateRepository) {
        this.currencyExchangeRateRepository = currencyExchangeRateRepository;
    }

    public CurrencyConversionResult convert(List<MonetaryLineItem> lineItems, LocalDate asOfDate) {
        List<LineItem> convertedLineItems = new ArrayList<>();
        Map<String, BigDecimal> exchangeRatesUsed = new LinkedHashMap<>();

        for (MonetaryLineItem item : lineItems) {
            if (TARGET_CURRENCY.equals(item.currency())) {
                convertedLineItems.add(new LineItem(item.code(), item.description(), item.buyAmount(), item.sellAmount()));
                continue;
            }

            String pairKey = item.currency() + "_" + TARGET_CURRENCY;
            BigDecimal rate = exchangeRatesUsed.computeIfAbsent(pairKey, key -> lookupRate(item.currency(), asOfDate));

            // Rounded here, at the point each converted amount becomes a final LineItem output —
            // same discipline as the Step 0 fix and Stage 7's rounding fix.
            BigDecimal buyZar = item.buyAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal sellZar = item.sellAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);
            convertedLineItems.add(new LineItem(item.code(), item.description(), buyZar, sellZar));
        }

        return new CurrencyConversionResult(convertedLineItems, exchangeRatesUsed);
    }

    private BigDecimal lookupRate(String fromCurrency, LocalDate asOfDate) {
        CurrencyExchangeRate rate = currencyExchangeRateRepository
                .findMostRecentRateOnOrBefore(fromCurrency, TARGET_CURRENCY, asOfDate)
                .orElseThrow(() -> new ExchangeRateNotFoundException(fromCurrency, TARGET_CURRENCY, asOfDate));
        return rate.getRate();
    }
}
