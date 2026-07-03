package com.vantageit.road_freight_rate_engine.rateengine.servicelevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceLevel;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceRequest;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.SurchargeRateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies Stage 7 end to end against the V15/V16/V17 seed fixtures (6 accessorial flat surcharge
 * rates, effective from 2025-01-01, all ZAR-denominated per V17's added {@code currency} column).
 * {@code baseFreightAmount=1000.00 + surchargesTotal=200.00 + clearancesTotal=100.00 =
 * preMultiplierSum=1300.00} is the fixed baseline for every multiplier test unless a case
 * specifically needs different totals (e.g. the zero-totals case).
 */
@SpringBootTest
class ServiceLevelComputationServiceTest {

    private static final LocalDate RATE_DATE = LocalDate.of(2025, 7, 15);
    private static final BigDecimal PRE_MULTIPLIER_SUM = new BigDecimal("1300.00");

    @Autowired
    private ServiceLevelComputationService serviceLevelComputationService;

    @Autowired
    private SurchargeRateRepository surchargeRateRepository;

    @Test
    void economyMultiplierLeavesSubtotalUnchanged() {
        ServiceLevelResult result = compute(totals(), ServiceLevel.ECONOMY);

        assertThat(result.multipliedSubtotal()).isEqualByComparingTo(PRE_MULTIPLIER_SUM);
        assertThat(result.serviceLevelLineItem().sellZar()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.serviceLevelLineItem().buyZar()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.currency()).isEqualTo("ZAR");
    }

    @Test
    void standardMultiplierAppliesExactUplift() {
        // 1300.00 * 1.15 = 1495.00, uplift = 195.00
        ServiceLevelResult result = compute(totals(), ServiceLevel.STANDARD);

        assertThat(result.multipliedSubtotal()).isEqualByComparingTo(new BigDecimal("1495.00"));
        assertThat(result.serviceLevelLineItem().sellZar()).isEqualByComparingTo(new BigDecimal("195.00"));
        assertThat(result.serviceLevelLineItem().buyZar()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void expressMultiplierAppliesExactUplift() {
        // 1300.00 * 1.40 = 1820.00, uplift = 520.00
        ServiceLevelResult result = compute(totals(), ServiceLevel.EXPRESS);

        assertThat(result.multipliedSubtotal()).isEqualByComparingTo(new BigDecimal("1820.00"));
        assertThat(result.serviceLevelLineItem().sellZar()).isEqualByComparingTo(new BigDecimal("520.00"));
        assertThat(result.serviceLevelLineItem().buyZar()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void dedicatedMultiplierAppliesExactUplift() {
        // 1300.00 * 1.65 = 2145.00, uplift = 845.00
        ServiceLevelResult result = compute(totals(), ServiceLevel.DEDICATED);

        assertThat(result.multipliedSubtotal()).isEqualByComparingTo(new BigDecimal("2145.00"));
        assertThat(result.serviceLevelLineItem().sellZar()).isEqualByComparingTo(new BigDecimal("845.00"));
        assertThat(result.serviceLevelLineItem().buyZar()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void multiplierArithmeticIsRoundedToTwoDecimalPlaces() {
        // 333.33 * 1.15 = 383.3295 raw (multiply() never rounds on its own) — 383.3295 is not a
        // valid ZAR amount (3rd decimal place), so this pins the *rounded* 2dp result (HALF_UP:
        // ...3295 -> ...33) and exact scale=2, proving rounding actually happens here, not that it
        // doesn't. uplift is computed from the two already-rounded totals (383.33 - 333.33), so it
        // comes out to a clean 50.00 rather than the unrounded 49.9995.
        PreMultiplierTotals totals = new PreMultiplierTotals(new BigDecimal("333.33"), BigDecimal.ZERO, BigDecimal.ZERO, "ZAR");

        ServiceLevelResult result = compute(totals, ServiceLevel.STANDARD);

        assertThat(result.multipliedSubtotal()).isEqualByComparingTo(new BigDecimal("383.33"));
        assertThat(result.multipliedSubtotal().scale()).isEqualTo(2);
        assertThat(result.serviceLevelLineItem().sellZar()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void zeroBaseFreightComputesCleanly() {
        PreMultiplierTotals totals = new PreMultiplierTotals(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, "ZAR");

        ServiceLevelResult result = compute(totals, ServiceLevel.STANDARD);

        assertThat(result.multipliedSubtotal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.runningTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void accessorialsAreExcludedFromTheMultiplier() {
        // Standard (1.15x) + after-hours collection (350.00) + tail lift both ends (450+450=900.00).
        // Correct: multipliedSubtotal(1495.00) + accessorials(1250.00) = 2745.00.
        // Wrong-if-multiplied: (1300.00 + 1250.00) * 1.15 = 2932.50 — must NOT equal this.
        RateComputeRequest request = request(ServiceLevel.STANDARD, true, false, true, false);

        ServiceLevelResult result = serviceLevelComputationService.compute(totals(), request);

        assertThat(result.multipliedSubtotal()).isEqualByComparingTo(new BigDecimal("1495.00"));
        assertThat(result.runningTotal()).isEqualByComparingTo(new BigDecimal("2745.00"));
        assertThat(result.runningTotal()).isNotEqualByComparingTo(new BigDecimal("2932.50"));
    }

    @Test
    void afterHoursCollectionAndDeliveryBothProduceDistinctLineItems() {
        RateComputeRequest request = request(ServiceLevel.ECONOMY, true, true, false, false);

        ServiceLevelResult result = serviceLevelComputationService.compute(totals(), request);

        assertThat(result.accessorialLineItems()).extracting(LineItem::code)
                .containsExactlyInAnyOrder("AFTER_HOURS_COLLECTION", "AFTER_HOURS_DELIVERY");
        // containsExactlyInAnyOrder uses BigDecimal.equals() (scale-sensitive, unlike compareTo) —
        // 2dp literals here match the rounded output from AccessorialChargeCalculator exactly.
        assertThat(result.accessorialLineItems()).extracting(LineItem::sellZar)
                .containsExactlyInAnyOrder(new BigDecimal("350.00"), new BigDecimal("350.00"));
    }

    @Test
    void tailLiftAndDriverAssistApplyBothCollectionAndDeliveryConsistently() {
        // Known model limitation (see accessorial_collection_delivery_ambiguity_deferred memory):
        // ServiceRequest can't distinguish which end needs it, so both ends are always applied.
        RateComputeRequest request = request(ServiceLevel.ECONOMY, false, false, true, true);

        ServiceLevelResult result = serviceLevelComputationService.compute(totals(), request);

        assertThat(result.accessorialLineItems()).extracting(LineItem::code)
                .containsExactlyInAnyOrder(
                        "TAIL_LIFT_COLLECTION", "TAIL_LIFT_DELIVERY",
                        "DRIVER_ASSIST_LOADING", "DRIVER_ASSIST_OFFLOADING");
    }

    @Test
    void allFourAccessorialFlagsSimultaneouslyDropsNothingAndDoubleCountsNothing() {
        RateComputeRequest request = request(ServiceLevel.ECONOMY, true, true, true, true);

        ServiceLevelResult result = serviceLevelComputationService.compute(totals(), request);

        assertThat(result.accessorialLineItems()).extracting(LineItem::code)
                .containsExactlyInAnyOrder(
                        "AFTER_HOURS_COLLECTION", "AFTER_HOURS_DELIVERY",
                        "TAIL_LIFT_COLLECTION", "TAIL_LIFT_DELIVERY",
                        "DRIVER_ASSIST_LOADING", "DRIVER_ASSIST_OFFLOADING");
        BigDecimal accessorialSum = result.accessorialLineItems().stream()
                .map(LineItem::sellZar)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 350 + 350 + 450 + 450 + 300 + 300 = 2200.00 — exactly 6 line items' worth, no duplicates.
        assertThat(accessorialSum).isEqualByComparingTo(new BigDecimal("2200.0000"));
    }

    @Test
    void accessorialRateAtEffectiveFromBoundaryResolvesInclusively() {
        // SL_TEST_DATE_BOUNDARY: effective_from=2025-06-01, effective_to=2025-06-30. Tests the
        // repository mechanism AccessorialChargeCalculator relies on directly, since its own
        // surcharge codes are fixed and can't be redirected to a synthetic test code.
        var rate = surchargeRateRepository.findActiveSurcharges("SL_TEST_DATE_BOUNDARY", LocalDate.of(2025, 6, 1));

        assertThat(rate).isPresent();
        assertThat(rate.get().getRateValue()).isEqualByComparingTo(new BigDecimal("100.0000"));
    }

    @Test
    void accessorialRateAtEffectiveToBoundaryResolvesInclusively() {
        var rate = surchargeRateRepository.findActiveSurcharges("SL_TEST_DATE_BOUNDARY", LocalDate.of(2025, 6, 30));

        assertThat(rate).isPresent();
    }

    @Test
    void nonZarPreMultiplierCurrencyAgainstZarAccessorialRateThrowsMismatch() {
        // FIXED, was a silent gap: surcharge_rates now has a currency column (V17), and
        // AccessorialChargeCalculator checks each resolved rate's currency against
        // PreMultiplierTotals.currency, rejecting a mismatch rather than silently summing
        // incompatible currencies into one running total. AFTER_HOURS_COLLECTION is seeded ZAR
        // (V15); requesting it against a EUR PreMultiplierTotals must be rejected, not computed.
        // Actually reconciling (converting) mismatched currencies remains Stage 8's job — this only
        // proves Stage 7 no longer proceeds blind to the mismatch.
        RateComputeRequest request = request(ServiceLevel.ECONOMY, true, false, false, false);
        PreMultiplierTotals eurTotals = new PreMultiplierTotals(new BigDecimal("1000.00"), new BigDecimal("200.00"), new BigDecimal("100.00"), "EUR");

        assertThatThrownBy(() -> serviceLevelComputationService.compute(eurTotals, request))
                .isInstanceOf(AccessorialCurrencyMismatchException.class)
                .satisfies(e -> {
                    AccessorialCurrencyMismatchException ex = (AccessorialCurrencyMismatchException) e;
                    assertThat(ex.getSurchargeCode()).isEqualTo("AFTER_HOURS_COLLECTION");
                    assertThat(ex.getExpectedCurrency()).isEqualTo("EUR");
                    assertThat(ex.getActualCurrency()).isEqualTo("ZAR");
                });
    }

    @Test
    void runningTotalExactlyMatchesIndependentlySummedTotal() {
        RateComputeRequest request = request(ServiceLevel.EXPRESS, true, true, true, false);

        ServiceLevelResult result = serviceLevelComputationService.compute(totals(), request);

        BigDecimal independentSum = result.multipliedSubtotal().add(
                result.accessorialLineItems().stream().map(LineItem::sellZar).reduce(BigDecimal.ZERO, BigDecimal::add));
        assertThat(result.runningTotal()).isEqualByComparingTo(independentSum);
    }

    @Test
    void noAccessorialFlagsProducesEmptyListNotNull() {
        RateComputeRequest request = request(ServiceLevel.STANDARD, false, false, false, false);

        ServiceLevelResult result = serviceLevelComputationService.compute(totals(), request);

        assertThat(result.accessorialLineItems()).isNotNull().isEmpty();
        assertThat(result.runningTotal()).isEqualByComparingTo(result.multipliedSubtotal());
    }

    @Test
    void missingAccessorialRateThrows() {
        // AFTER_HOURS_COLLECTION is only effective from 2025-01-01 — a rate_date before that
        // finds no active row.
        RateComputeRequest request = new RateComputeRequest(
                UUID.randomUUID(), LocalDate.of(2024, 1, 1), null, null,
                serviceRequest(ServiceLevel.ECONOMY, true, false, false, false, LocalDate.of(2024, 1, 1)));

        assertThatThrownBy(() -> serviceLevelComputationService.compute(totals(), request))
                .isInstanceOf(AccessorialRateNotFoundException.class)
                .satisfies(e -> {
                    AccessorialRateNotFoundException ex = (AccessorialRateNotFoundException) e;
                    assertThat(ex.getSurchargeCode()).isEqualTo("AFTER_HOURS_COLLECTION");
                });
    }

    @Test
    void zeroSurchargesAndClearancesTotalsWorkStandalone() {
        // Simulates today's actual reality: the surcharges/clearances pipeline stages don't exist
        // yet, so these are genuinely zero, not a special case this service needs to detect.
        PreMultiplierTotals totals = new PreMultiplierTotals(new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO, "ZAR");

        ServiceLevelResult result = compute(totals, ServiceLevel.STANDARD);

        assertThat(result.multipliedSubtotal()).isEqualByComparingTo(new BigDecimal("1150.00"));
    }

    private ServiceLevelResult compute(PreMultiplierTotals totals, ServiceLevel serviceLevel) {
        RateComputeRequest request = request(serviceLevel, false, false, false, false);
        return serviceLevelComputationService.compute(totals, request);
    }

    private static PreMultiplierTotals totals() {
        return new PreMultiplierTotals(new BigDecimal("1000.00"), new BigDecimal("200.00"), new BigDecimal("100.00"), "ZAR");
    }

    private static RateComputeRequest request(
            ServiceLevel serviceLevel, boolean afterHoursCollection, boolean afterHoursDelivery, boolean tailLift, boolean driverAssist) {
        return new RateComputeRequest(UUID.randomUUID(), RATE_DATE, null, null,
                serviceRequest(serviceLevel, afterHoursCollection, afterHoursDelivery, tailLift, driverAssist, RATE_DATE));
    }

    private static ServiceRequest serviceRequest(
            ServiceLevel serviceLevel, boolean afterHoursCollection, boolean afterHoursDelivery,
            boolean tailLift, boolean driverAssist, LocalDate collectionDate) {
        return new ServiceRequest(
                serviceLevel, collectionDate, null,
                afterHoursCollection, afterHoursDelivery, tailLift, driverAssist, false, false);
    }
}
