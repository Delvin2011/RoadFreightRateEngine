package com.vantageit.road_freight_rate_engine.rateengine.currencyconversion;

import static org.assertj.core.api.Assertions.assertThat;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.Totals;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Verifies the Step 2 + Step 3 orchestration against the V21 seed fixtures (USD_ZAR = 18.52 as of 2025-06-15). */
@SpringBootTest
class TotalsComputationServiceTest {

    @Autowired
    private TotalsComputationService totalsComputationService;

    @Test
    void computesFullTotalsAcrossMixedCurrenciesWithMarginPctAlwaysNull() {
        // ZAR base freight (1000.00/1200.00) + USD charge (100.00/120.00, converts to
        // 1852.00/2222.40 at 18.52) -> subtotalBuy=2852.00, subtotalSell=3422.40,
        // vat=3422.40*0.15=513.36, total=3935.76.
        List<MonetaryLineItem> lineItems = List.of(
                new MonetaryLineItem("BASE_FREIGHT", "Base freight", new BigDecimal("1000.00"), new BigDecimal("1200.00"), "ZAR"),
                new MonetaryLineItem("USD_CHARGE", "USD charge", new BigDecimal("100.00"), new BigDecimal("120.00"), "USD"));

        TotalsComputationService.TotalsResult result = totalsComputationService.compute(lineItems, LocalDate.of(2025, 6, 15));

        Totals totals = result.totals();
        assertThat(totals.subtotalBuyZar()).isEqualByComparingTo(new BigDecimal("2852.00"));
        assertThat(totals.subtotalSellZar()).isEqualByComparingTo(new BigDecimal("3422.40"));
        assertThat(totals.vatZar()).isEqualByComparingTo(new BigDecimal("513.36"));
        assertThat(totals.totalSellInclVatZar()).isEqualByComparingTo(new BigDecimal("3935.76"));
        assertThat(totals.marginPct()).isNull();
        assertThat(result.exchangeRatesUsed()).containsOnlyKeys("USD_ZAR");
        assertThat(result.convertedLineItems()).hasSize(2);
    }

    @Test
    void marginPctIsNullEvenForAnAllZarSingleLineItemQuote() {
        List<MonetaryLineItem> lineItems = List.of(
                new MonetaryLineItem("BASE_FREIGHT", "Base freight", new BigDecimal("500.00"), new BigDecimal("500.00"), "ZAR"));

        TotalsComputationService.TotalsResult result = totalsComputationService.compute(lineItems, LocalDate.of(2025, 6, 15));

        assertThat(result.totals().marginPct()).isNull();
    }
}
