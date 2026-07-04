package com.vantageit.road_freight_rate_engine.rateengine.currencyconversion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class VatCalculationServiceTest {

    private final VatCalculationService vatCalculationService = new VatCalculationService();

    @Test
    void standardCaseAppliesExactlyFifteenPercent() {
        VatCalculationService.VatResult result = vatCalculationService.computeVat(new BigDecimal("10000.00"), false);

        assertThat(result.vatAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(result.totalInclVat()).isEqualByComparingTo(new BigDecimal("11500.00"));
    }

    @Test
    void standardCaseRoundsHalfUpNotTruncated() {
        // 333.33 * 0.15 = 49.9995 raw -> must round to 50.00.
        VatCalculationService.VatResult result = vatCalculationService.computeVat(new BigDecimal("333.33"), false);

        assertThat(result.vatAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(result.totalInclVat()).isEqualByComparingTo(new BigDecimal("383.33"));
    }

    @Test
    void zeroRatedProducesZeroVatAndUnchangedTotal() {
        VatCalculationService.VatResult result = vatCalculationService.computeVat(new BigDecimal("10000.00"), true);

        assertThat(result.vatAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalInclVat()).isEqualByComparingTo(new BigDecimal("10000.00"));
    }
}
