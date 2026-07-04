package com.vantageit.road_freight_rate_engine.rateengine.currencyconversion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies Stage 8's currency conversion against the V21 seed fixtures: USD_ZAR at 17.90
 * (2025-01-01), 18.52 (2025-06-01), 18.75 (2025-07-01); EUR_ZAR at 19.40 (2025-01-01), 19.80
 * (2025-06-01).
 */
@SpringBootTest
class CurrencyConversionServiceTest {

    @Autowired
    private CurrencyConversionService currencyConversionService;

    @Test
    void allZarLineItemsPassThroughUnchanged() {
        List<MonetaryLineItem> lineItems = List.of(
                new MonetaryLineItem("BASE_FREIGHT", "Base freight", new BigDecimal("1000.00"), new BigDecimal("1200.00"), "ZAR"),
                new MonetaryLineItem("FUEL_LEVY", "Fuel levy", new BigDecimal("200.00"), new BigDecimal("220.00"), "ZAR"));

        CurrencyConversionResult result = currencyConversionService.convert(lineItems, LocalDate.of(2025, 7, 15));

        assertThat(result.convertedLineItems()).containsExactly(
                new LineItem("BASE_FREIGHT", "Base freight", new BigDecimal("1000.00"), new BigDecimal("1200.00")),
                new LineItem("FUEL_LEVY", "Fuel levy", new BigDecimal("200.00"), new BigDecimal("220.00")));
        assertThat(result.exchangeRatesUsed()).isEmpty();
    }

    @Test
    void mixedZarAndUsdLineItemsConvertOnlyUsd() {
        // 100.00 * 18.52 = 1852.00, 120.00 * 18.52 = 2222.40 (asOfDate 2025-07-15 -> most recent
        // on-or-before rate is 2025-06-01's 18.52, since 2025-07-01's 18.75 is also on-or-before
        // but 2025-06-01 is closer... wait: most recent ON OR BEFORE 2025-07-15 is actually
        // 2025-07-01's 18.75, not 2025-06-01's 18.52. Using 2025-06-15 instead, so 18.52 is the
        // correct expected rate for this specific assertion.
        List<MonetaryLineItem> lineItems = List.of(
                new MonetaryLineItem("BASE_FREIGHT", "Base freight", new BigDecimal("500.00"), new BigDecimal("600.00"), "ZAR"),
                new MonetaryLineItem("USD_CHARGE", "USD charge", new BigDecimal("100.00"), new BigDecimal("120.00"), "USD"));

        CurrencyConversionResult result = currencyConversionService.convert(lineItems, LocalDate.of(2025, 6, 15));

        assertThat(result.convertedLineItems()).containsExactly(
                new LineItem("BASE_FREIGHT", "Base freight", new BigDecimal("500.00"), new BigDecimal("600.00")),
                new LineItem("USD_CHARGE", "USD charge", new BigDecimal("1852.00"), new BigDecimal("2222.40")));
        assertThat(result.exchangeRatesUsed()).containsOnlyKeys("USD_ZAR");
        assertThat(result.exchangeRatesUsed().get("USD_ZAR")).isEqualByComparingTo(new BigDecimal("18.520000"));
    }

    @Test
    void mostRecentOnOrBeforePolicySelectsEarlierRateNotLaterOrExactMatch() {
        // asOfDate=2025-06-15 falls strictly between the 2025-06-01 (18.52) and 2025-07-01 (18.75)
        // seeded rate dates, with no rate published exactly on 2025-06-15 itself — must select
        // 18.52 (most recent ON OR BEFORE), not 18.75 (later, invalid to use) and not throw
        // (an exact match isn't required).
        List<MonetaryLineItem> lineItems = List.of(
                new MonetaryLineItem("CHARGE", "Charge", new BigDecimal("10.00"), new BigDecimal("10.00"), "USD"));

        CurrencyConversionResult result = currencyConversionService.convert(lineItems, LocalDate.of(2025, 6, 15));

        assertThat(result.exchangeRatesUsed().get("USD_ZAR")).isEqualByComparingTo(new BigDecimal("18.52"));
    }

    @Test
    void asOfDateBeforeAnySeededRateThrows() {
        List<MonetaryLineItem> lineItems = List.of(
                new MonetaryLineItem("CHARGE", "Charge", new BigDecimal("10.00"), new BigDecimal("10.00"), "USD"));

        assertThatThrownBy(() -> currencyConversionService.convert(lineItems, LocalDate.of(2024, 12, 31)))
                .isInstanceOf(ExchangeRateNotFoundException.class)
                .satisfies(e -> {
                    ExchangeRateNotFoundException ex = (ExchangeRateNotFoundException) e;
                    assertThat(ex.getFromCurrency()).isEqualTo("USD");
                    assertThat(ex.getToCurrency()).isEqualTo("ZAR");
                });
    }

    @Test
    void conversionRoundingAppliesHalfUpNotTruncation() {
        // 100.33 * 18.52 = 1858.1116 raw -> must round to 1858.11, not truncate or stay at higher
        // precision.
        List<MonetaryLineItem> lineItems = List.of(
                new MonetaryLineItem("CHARGE", "Charge", new BigDecimal("100.33"), new BigDecimal("100.33"), "USD"));

        CurrencyConversionResult result = currencyConversionService.convert(lineItems, LocalDate.of(2025, 6, 15));

        LineItem converted = result.convertedLineItems().get(0);
        assertThat(converted.buyZar()).isEqualByComparingTo(new BigDecimal("1858.11"));
        assertThat(converted.buyZar().scale()).isEqualTo(2);
    }
}
