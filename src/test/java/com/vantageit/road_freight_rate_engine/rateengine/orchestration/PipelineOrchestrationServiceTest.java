package com.vantageit.road_freight_rate_engine.rateengine.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.AddressType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoClass;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ContainerType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.Dimensions;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LoadType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.PackageType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.PackingGroup;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeResponse;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceLevel;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.TemperatureRange;
import com.vantageit.road_freight_rate_engine.rateengine.basefreight.AmbiguousRateConfigurationException;
import com.vantageit.road_freight_rate_engine.rateengine.basefreight.BaseFreightComputationService;
import com.vantageit.road_freight_rate_engine.rateengine.clearance.ClearanceComputationService;
import com.vantageit.road_freight_rate_engine.rateengine.currencyconversion.CurrencyConversionService;
import com.vantageit.road_freight_rate_engine.rateengine.currencyconversion.TotalsComputationService;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.SurchargeRateRepository;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.DistanceNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionService;
import com.vantageit.road_freight_rate_engine.rateengine.pipeline.common.ChargeableWeightCalculator;
import com.vantageit.road_freight_rate_engine.rateengine.servicelevel.ServiceLevelComputationService;
import com.vantageit.road_freight_rate_engine.rateengine.surcharge.SurchargeStackComputationService;
import com.vantageit.road_freight_rate_engine.rateengine.validation.InputValidationService;
import com.vantageit.road_freight_rate_engine.rateengine.vehicleselection.VehicleSelectionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies pipeline orchestration end to end against real seeded fixtures — the established
 * JHB_METRO:HARARE cross-border fixture (V7/V8/V12: BEIT_BRIDGE, 8T_RIGID, ftl/per_km 10.0000 ZAR)
 * for cross-border scenarios, and JHB_METRO:BFN_METRO (V5: 34T_SEMI, ftl/per_km 18.5000 ZAR) for
 * domestic ones, plus V24's dedicated orchestration-only fixtures (EUR-rated vehicle, a
 * dedicated-vs-cost-efficient divergent pair, and a genuine rate ambiguity) added specifically for
 * this expanded test round. LIMPOPO_RURAL -> HARARE (V8) is reused for the DistanceNotFoundException
 * case — it's already a deliberately-inactive-with-no-alternative fixture from Stage 4's own tests.
 */
@SpringBootTest
class PipelineOrchestrationServiceTest {

    private static final UUID JHB_LOCATION_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final UUID BFN_LOCATION_ID = UUID.fromString("60000000-0000-0000-0000-000000000002");
    private static final UUID HARARE_LOCATION_ID = UUID.fromString("60000000-0000-0000-0000-000000000003");
    private static final UUID LIMPOPO_RURAL_LOCATION_ID = UUID.fromString("60000000-0000-0000-0000-000000000004");
    private static final UUID BEIT_BRIDGE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID TEST_BORDER_2_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final LocalDate COLLECTION_DATE = LocalDate.of(2025, 7, 15);

    @Autowired
    private PipelineOrchestrationService pipelineOrchestrationService;

    @Test
    void fullHappyPathProducesCompleteResponseWithCorrectTotals() {
        RateComputeResponse response = pipelineOrchestrationService.compute(crossBorderHazmatRequest());

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.rateSnapshotId()).isNotNull();
        assertThat(response.vehicleSelected()).isEqualTo("8T_RIGID");
        assertThat(response.distanceKm()).isEqualByComparingTo(new BigDecimal("1225.00"));
        assertThat(response.chargeableWeightKg()).isEqualByComparingTo(new BigDecimal("5000"));
        assertThat(response.requiresManualReview()).isFalse();

        assertThat(response.lineItems()).extracting(LineItem::code).containsExactlyInAnyOrder(
                "BASE_FREIGHT", "FUEL_LEVY", "HAZMAT_PG1_UPLIFT",
                "BORDER_CLEARING_AGENT_FEE", "COMESA_LIABILITY_INSURANCE", "SARS_CPF",
                "ZINARA_ROAD_ACCESS_FEE", "CARBON_TAX_LEVY", "ADG_TRANSPORT_PERMIT",
                "SERVICE_MULTIPLIER");
        // 10.0000 * 1225.00 * 5000 / 1000, rounded 2dp.
        assertThat(lineItemFor(response, "BASE_FREIGHT").sellZar()).isEqualByComparingTo(new BigDecimal("61250.00"));
        // 15% of 61250.00.
        assertThat(lineItemFor(response, "HAZMAT_PG1_UPLIFT").sellZar()).isEqualByComparingTo(new BigDecimal("9187.50"));

        assertThat(response.exchangeRatesUsed()).containsOnlyKeys("USD_ZAR");
        // 100902.88 * 0.15
        assertThat(response.totals().vatZar()).isEqualByComparingTo(new BigDecimal("15135.43"));
        assertThat(response.totals().totalSellInclVatZar()).isEqualByComparingTo(new BigDecimal("116038.31"));
        assertThat(response.totals().marginPct()).isNull();
    }

    @Test
    void validationFailureShortCircuitsEveryLaterStage() {
        RateComputeRequest invalidRequest = crossBorderRequestMissingBorderPost();

        LaneResolutionService laneResolutionService = mock(LaneResolutionService.class);
        VehicleSelectionService vehicleSelectionService = mock(VehicleSelectionService.class);
        BaseFreightComputationService baseFreightComputationService = mock(BaseFreightComputationService.class);
        SurchargeStackComputationService surchargeStackComputationService = mock(SurchargeStackComputationService.class);
        ClearanceComputationService clearanceComputationService = mock(ClearanceComputationService.class);
        ServiceLevelComputationService serviceLevelComputationService = mock(ServiceLevelComputationService.class);
        CurrencyConversionService currencyConversionService = mock(CurrencyConversionService.class);
        TotalsComputationService totalsComputationService = mock(TotalsComputationService.class);
        SurchargeRateRepository surchargeRateRepository = mock(SurchargeRateRepository.class);

        PipelineOrchestrationService orchestrator = new PipelineOrchestrationService(
                new InputValidationService(),
                laneResolutionService,
                vehicleSelectionService,
                baseFreightComputationService,
                surchargeStackComputationService,
                clearanceComputationService,
                serviceLevelComputationService,
                currencyConversionService,
                totalsComputationService,
                surchargeRateRepository,
                new PipelineExceptionTranslator());

        assertThatThrownBy(() -> orchestrator.compute(invalidRequest))
                .isInstanceOf(PipelineValidationException.class)
                .satisfies(e -> {
                    PipelineValidationException ex = (PipelineValidationException) e;
                    assertThat(ex.getErrorResponse().status()).isEqualTo("error");
                    assertThat(ex.getErrorResponse().errors()).extracting(err -> err.code())
                            .containsExactly("REQUIRED_FOR_CROSS_BORDER");
                });

        verifyNoInteractions(
                laneResolutionService, vehicleSelectionService, baseFreightComputationService,
                surchargeStackComputationService, clearanceComputationService, serviceLevelComputationService,
                currencyConversionService, totalsComputationService);
    }

    @Test
    void currencyReconciliationHappensExactlyOnceAtTheEndNotAsRawMixedCurrencySums() {
        RateComputeResponse response = pipelineOrchestrationService.compute(crossBorderHazmatRequest());

        // Correct (fully-converted-to-ZAR): 100902.88. If USD amounts were summed unconverted as
        // if they were ZAR, the wrong result would be 99305.38 -- proving conversion actually
        // multiplied by the exchange rate rather than passing raw numbers through.
        assertThat(response.totals().subtotalSellZar()).isEqualByComparingTo(new BigDecimal("100902.88"));
        assertThat(response.totals().subtotalSellZar()).isNotEqualByComparingTo(new BigDecimal("99305.38"));

        BigDecimal usdRate = response.exchangeRatesUsed().get("USD_ZAR");
        assertThat(usdRate).isNotNull();
        // COMESA_LIABILITY_INSURANCE: 45.00 USD * rate, rounded 2dp -- proves this specific line
        // item was genuinely converted, not left as a raw 45.00 "ZAR" value.
        BigDecimal expectedComesaZar = new BigDecimal("45.00").multiply(usdRate).setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(lineItemFor(response, "COMESA_LIABILITY_INSURANCE").sellZar()).isEqualByComparingTo(expectedComesaZar);
        assertThat(lineItemFor(response, "COMESA_LIABILITY_INSURANCE").sellZar()).isNotEqualByComparingTo(new BigDecimal("45.00"));
    }

    @Test
    void flagsIncludeDistanceOverrideAndLegalLimitFlagsTogether() {
        RateComputeRequest request = domesticRequestWithDistanceOverrideAndAbnormalWidth();

        RateComputeResponse response = pipelineOrchestrationService.compute(request);

        assertThat(response.flags()).containsExactlyInAnyOrder("DISTANCE_OVERRIDE", "ABNORMAL_WIDTH");
        // The override value (500.00) is used verbatim, not the lane_distances lookup (398.00).
        assertThat(response.distanceKm()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void domesticNonHazmatNonFlaggedShipmentHasEmptyFlagsAndNoManualReview() {
        RateComputeResponse response = pipelineOrchestrationService.compute(domesticGeneralCargoRequest());

        assertThat(response.flags()).isEmpty();
        assertThat(response.requiresManualReview()).isFalse();
        assertThat(response.lineItems()).extracting(LineItem::code).containsExactlyInAnyOrder("BASE_FREIGHT", "FUEL_LEVY", "SERVICE_MULTIPLIER");
    }

    @Test
    void validationFailureIsNeverReachedByAnyDownstreamStageMockVerified() {
        // Dedicated, minimal test focused purely on the mock-interaction proof -- separate from
        // validationFailureShortCircuitsEveryLaterStage, which also asserts the error response
        // shape. This one asserts nothing about the exception's content, only that every
        // downstream stage bean was never touched.
        RateComputeRequest invalidRequest = crossBorderRequestMissingBorderPost();

        LaneResolutionService laneResolutionService = mock(LaneResolutionService.class);
        VehicleSelectionService vehicleSelectionService = mock(VehicleSelectionService.class);
        BaseFreightComputationService baseFreightComputationService = mock(BaseFreightComputationService.class);
        SurchargeStackComputationService surchargeStackComputationService = mock(SurchargeStackComputationService.class);
        ClearanceComputationService clearanceComputationService = mock(ClearanceComputationService.class);
        ServiceLevelComputationService serviceLevelComputationService = mock(ServiceLevelComputationService.class);
        CurrencyConversionService currencyConversionService = mock(CurrencyConversionService.class);
        TotalsComputationService totalsComputationService = mock(TotalsComputationService.class);
        SurchargeRateRepository surchargeRateRepository = mock(SurchargeRateRepository.class);

        PipelineOrchestrationService orchestrator = new PipelineOrchestrationService(
                new InputValidationService(), laneResolutionService, vehicleSelectionService, baseFreightComputationService,
                surchargeStackComputationService, clearanceComputationService, serviceLevelComputationService,
                currencyConversionService, totalsComputationService, surchargeRateRepository, new PipelineExceptionTranslator());

        assertThatThrownBy(() -> orchestrator.compute(invalidRequest)).isInstanceOf(PipelineValidationException.class);

        verifyNoInteractions(
                laneResolutionService, vehicleSelectionService, baseFreightComputationService,
                surchargeStackComputationService, clearanceComputationService, serviceLevelComputationService,
                currencyConversionService, totalsComputationService, surchargeRateRepository);
    }

    @Test
    void threeCurrencyReconciliationAcrossBaseFreightSurchargeAndClearanceSimultaneously() {
        // 9T_EUR_TEST (V24): EUR base freight, on LIMPOPO_RURAL:HARARE via TEST_BORDER_2 (a
        // dedicated, otherwise-unused lane -- see V24's comment for why). FUEL_LEVY inherits base
        // freight's currency (EUR) -- exactly the subtlety documented on
        // PipelineOrchestrationService's Javadoc. Clearance fees (still cross-border, still
        // ZIM-destined) supply ZAR (BORDER_CLEARING_AGENT_FEE, SARS_CPF) and USD
        // (COMESA/ZINARA/CARBON_TAX) simultaneously -- three currencies in one run.
        RateComputeResponse response = pipelineOrchestrationService.compute(euroBaseFreightCrossBorderRequest());

        assertThat(response.vehicleSelected()).isEqualTo("9T_EUR_TEST");
        assertThat(response.exchangeRatesUsed()).containsOnlyKeys("EUR_ZAR", "USD_ZAR");

        BigDecimal eurRate = response.exchangeRatesUsed().get("EUR_ZAR");
        BigDecimal usdRate = response.exchangeRatesUsed().get("USD_ZAR");

        // 8.0000 * 1400.00 * 8500 / 1000, rounded 2dp -> 95200.00 EUR, converted at eurRate.
        BigDecimal expectedBaseFreightZar = new BigDecimal("95200.00").multiply(eurRate).setScale(2, RoundingMode.HALF_UP);
        assertThat(lineItemFor(response, "BASE_FREIGHT").sellZar()).isEqualByComparingTo(expectedBaseFreightZar);

        // 22% of 95200.00 EUR = 20944.00 EUR (not ZAR, despite FUEL_LEVY's own surcharge_rates row
        // being ZAR) -- proves the base-freight-currency-inheritance fix, not just plain conversion.
        BigDecimal expectedFuelLevyZar = new BigDecimal("20944.00").multiply(eurRate).setScale(2, RoundingMode.HALF_UP);
        assertThat(lineItemFor(response, "FUEL_LEVY").sellZar()).isEqualByComparingTo(expectedFuelLevyZar);

        // ZAR clearance fees pass through unconverted.
        assertThat(lineItemFor(response, "BORDER_CLEARING_AGENT_FEE").sellZar()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(lineItemFor(response, "SARS_CPF").sellZar()).isEqualByComparingTo(new BigDecimal("250.00"));

        // USD clearance fees converted at usdRate.
        assertThat(lineItemFor(response, "COMESA_LIABILITY_INSURANCE").sellZar())
                .isEqualByComparingTo(new BigDecimal("45.00").multiply(usdRate).setScale(2, RoundingMode.HALF_UP));
        assertThat(lineItemFor(response, "ZINARA_ROAD_ACCESS_FEE").sellZar())
                .isEqualByComparingTo(new BigDecimal("30.00").multiply(usdRate).setScale(2, RoundingMode.HALF_UP));
        assertThat(lineItemFor(response, "CARBON_TAX_LEVY").sellZar())
                .isEqualByComparingTo(new BigDecimal("15.00").multiply(usdRate).setScale(2, RoundingMode.HALF_UP));

        // Deliberately not asserting totals.totalSellInclVatZar() exactly here: the SERVICE_MULTIPLIER
        // uplift is computed against PreMultiplierTotals.sum(), which itself is a raw mixed-currency
        // sum (baseFreightAmount + surchargesTotal + clearancesTotal) -- a separate, already-accepted
        // Stage 7 simplification ("single currency assumption") that this fix doesn't touch. This
        // test's job is to prove the per-line-item conversion is correct, not the multiplier math.
    }

    @Test
    void identicalRequestsProduceIdenticalOutputExceptSnapshotIdAndComputedAt() {
        RateComputeRequest request = domesticGeneralCargoRequest();

        RateComputeResponse first = pipelineOrchestrationService.compute(request);
        RateComputeResponse second = pipelineOrchestrationService.compute(request);

        assertThat(first.status()).isEqualTo(second.status());
        assertThat(first.quoteContextId()).isEqualTo(second.quoteContextId());
        assertThat(first.vehicleSelected()).isEqualTo(second.vehicleSelected());
        assertThat(first.distanceKm()).isEqualByComparingTo(second.distanceKm());
        assertThat(first.chargeableWeightKg()).isEqualByComparingTo(second.chargeableWeightKg());
        assertThat(first.requiresManualReview()).isEqualTo(second.requiresManualReview());
        assertThat(first.flags()).isEqualTo(second.flags());
        assertThat(first.lineItems()).isEqualTo(second.lineItems());
        assertThat(first.totals()).isEqualTo(second.totals());
        assertThat(first.exchangeRatesUsed()).isEqualTo(second.exchangeRatesUsed());
    }

    @Test
    void rateSnapshotIdIsUniqueAcrossTwoCallsWithIdenticalInput() {
        RateComputeRequest request = domesticGeneralCargoRequest();

        RateComputeResponse first = pipelineOrchestrationService.compute(request);
        RateComputeResponse second = pipelineOrchestrationService.compute(request);

        assertThat(first.rateSnapshotId()).isNotEqualTo(second.rateSnapshotId());
    }

    @Test
    void dedicatedVehiclePathSelectsSmallestNotCheapestVehicle() {
        // SMALL_EXPENSIVE_TEST (1000kg cap, flat 9000.00 ZAR) vs LARGE_CHEAP_TEST (4000kg cap,
        // flat 500.00 ZAR) on JHB_METRO:HARARE (V24) -- deliberately sized so cost-efficient
        // (default) would pick LARGE_CHEAP_TEST (cheaper), while dedicated_vehicle=true must pick
        // SMALL_EXPENSIVE_TEST instead (smallest capacity, cost ignored entirely).
        RateComputeResponse response = pipelineOrchestrationService.compute(dedicatedVehicleRequest());

        assertThat(response.vehicleSelected()).isEqualTo("SMALL_EXPENSIVE_TEST");
    }

    @Test
    void domesticShipmentWithRealSurchargesHasZeroClearances() {
        // FRAGILE cargo class triggers FRAGILE_HANDLING (a real, non-trivial surcharge) without
        // triggering any clearance charge (unlike hazmat/live-animals, which have compliance
        // clearance charges regardless of route type) -- isolates "surcharges present, clearances
        // genuinely absent" cleanly.
        RateComputeResponse response = pipelineOrchestrationService.compute(domesticFragileCargoRequest());

        assertThat(response.lineItems()).extracting(LineItem::code)
                .containsExactlyInAnyOrder("BASE_FREIGHT", "FUEL_LEVY", "FRAGILE_HANDLING", "SERVICE_MULTIPLIER");
        assertThat(lineItemFor(response, "FRAGILE_HANDLING").sellZar()).isNotEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void responseFieldsAreTracedToExactSourceValuesNotJustPresence() {
        RateComputeRequest request = domesticGeneralCargoRequest();

        RateComputeResponse response = pipelineOrchestrationService.compute(request);

        // vehicle_selected: of the two FTL-rated vehicles on JHB_METRO:BFN_METRO at this
        // chargeable weight (8T_RIGID at 12.00/km from V10, 34T_SEMI at 18.50/km from V5),
        // 8T_RIGID is genuinely cheaper (23880.00 vs 36815.00) and wins cost-efficient selection.
        assertThat(response.vehicleSelected()).isEqualTo("8T_RIGID");
        // distance_km: the exact lane_distances seed value for JHB_METRO -> BFN_METRO (V7), not a
        // coincidentally-matching literal.
        assertThat(response.distanceKm()).isEqualByComparingTo(new BigDecimal("398.00"));
        // chargeable_weight_kg: computed independently via the real ChargeableWeightCalculator,
        // not hardcoded, so this genuinely traces to the request's own gross_weight_kg/volume_cbm.
        BigDecimal expectedChargeableWeight = ChargeableWeightCalculator.compute(request.cargo().grossWeightKg(), request.cargo().volumeCbm());
        assertThat(response.chargeableWeightKg()).isEqualByComparingTo(expectedChargeableWeight);
    }

    @Test
    void distanceOverrideAndAbnormalHeightAndLengthFlagsAppearTogether() {
        // A different flag combination from flagsIncludeDistanceOverrideAndLegalLimitFlagsTogether
        // (which uses width) -- genuinely new coverage: two simultaneous legal-limit flags
        // (height, length) alongside DISTANCE_OVERRIDE, not just one.
        RateComputeRequest request = domesticRequestWithDistanceOverrideAndAbnormalHeightAndLength();

        RateComputeResponse response = pipelineOrchestrationService.compute(request);

        assertThat(response.flags()).containsExactlyInAnyOrder("DISTANCE_OVERRIDE", "ABNORMAL_HEIGHT", "ABNORMAL_LENGTH");
    }

    @Test
    void ambiguousRateConfigurationExceptionMessageContainsStructuredDetail() {
        RateComputeRequest request = ambiguousRateReeferRequest();

        assertThatThrownBy(() -> pipelineOrchestrationService.compute(request))
                .isInstanceOf(PipelineValidationException.class)
                .satisfies(e -> {
                    PipelineValidationException ex = (PipelineValidationException) e;
                    assertThat(ex.getErrorResponse().errors()).hasSize(1);
                    String code = ex.getErrorResponse().errors().get(0).code();
                    String message = ex.getErrorResponse().errors().get(0).message();
                    assertThat(code).isEqualTo("AMBIGUOUS_RATE_CONFIGURATION");
                    // AmbiguousRateConfigurationException's own message embeds the lane key,
                    // vehicle category code, load type, and both conflicting rows' rate bases --
                    // confirms the translator preserves that detail rather than genericizing it.
                    assertThat(message).contains("JHB_METRO:HARARE", "AMBIGUOUS_TEST_VEHICLE", "reefer", "PER_TON", "PER_CBM");
                });
    }

    @Test
    void distanceNotFoundExceptionMessageContainsStructuredDetail() {
        RateComputeRequest request = distanceNotFoundRequest();

        assertThatThrownBy(() -> pipelineOrchestrationService.compute(request))
                .isInstanceOf(PipelineValidationException.class)
                .satisfies(e -> {
                    PipelineValidationException ex = (PipelineValidationException) e;
                    assertThat(ex.getErrorResponse().errors()).hasSize(1);
                    String code = ex.getErrorResponse().errors().get(0).code();
                    String message = ex.getErrorResponse().errors().get(0).message();
                    assertThat(code).isEqualTo("DISTANCE_NOT_FOUND");
                    // DistanceNotFoundException's message embeds the lane key and border post id.
                    assertThat(message).contains("LIMPOPO_RURAL:HARARE", BEIT_BRIDGE_ID.toString());
                });
    }

    @Test
    void requiresManualReviewPinnedFalseEvenForHazmatLiveAnimalsHighValueShipment() {
        // KNOWN GAP regression test, not a correctness claim: per the doc's Business Rules, a
        // shipment combining hazmat + live animals + high-value declared arguably SHOULD trigger
        // manual review, but that logic was never built by any stage (see
        // PipelineOrchestrationService's Javadoc) -- requires_manual_review is unconditionally
        // false today. This test exists to catch it if that ever silently changes, not to endorse
        // the current behavior as correct.
        RateComputeResponse response = pipelineOrchestrationService.compute(domesticHazmatLiveAnimalsHighValueRequest());

        assertThat(response.requiresManualReview()).isFalse();
    }

    @Test
    void vatIsAlwaysAppliedZeroRatedNeverReachableInPractice() {
        RateComputeResponse response = pipelineOrchestrationService.compute(domesticGeneralCargoRequest());

        // VatCalculationService's zeroRated path is hardcoded false at TotalsComputationService's
        // only call site (see the vat_zero_rating_deferred project memory) -- VAT is always
        // charged today, regardless of shipment content, since there's no request field to ever
        // set zeroRated=true.
        assertThat(response.totals().vatZar()).isNotEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.totals().vatZar()).isPositive();
    }

    private static LineItem lineItemFor(RateComputeResponse response, String code) {
        return response.lineItems().stream()
                .filter(li -> li.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No line item with code " + code + " in " + response.lineItems()));
    }

    private static RateComputeRequest crossBorderHazmatRequest() {
        RouteRequest route = new RouteRequest(
                JHB_LOCATION_ID, HARARE_LOCATION_ID, RouteType.CROSS_BORDER, BEIT_BRIDGE_ID, null, null,
                AddressType.DEPOT, AddressType.DOOR_TO_DOOR);
        CargoRequest cargo = new CargoRequest(
                CargoClass.HAZMAT, "2710.12.90", new BigDecimal("5000"), new BigDecimal("10"),
                LoadType.FTL, null, (ContainerType) null, PackageType.BULK, true, (Dimensions) null, null,
                "UN1203", PackingGroup.I, "3", false, null, false, false, false, false);
        return request(route, cargo);
    }

    private static RateComputeRequest crossBorderRequestMissingBorderPost() {
        RouteRequest route = new RouteRequest(
                JHB_LOCATION_ID, HARARE_LOCATION_ID, RouteType.CROSS_BORDER, null, null, null,
                AddressType.DEPOT, AddressType.DOOR_TO_DOOR);
        CargoRequest cargo = generalCargo();
        return request(route, cargo);
    }

    private static RateComputeRequest domesticRequestWithDistanceOverrideAndAbnormalWidth() {
        RouteRequest route = new RouteRequest(
                JHB_LOCATION_ID, BFN_LOCATION_ID, RouteType.DOMESTIC, null,
                new BigDecimal("500.00"), "Customer-confirmed shorter route via toll road",
                AddressType.DEPOT, AddressType.DOOR_TO_DOOR);
        CargoRequest cargo = new CargoRequest(
                CargoClass.GENERAL, "8481.80", new BigDecimal("5000"), new BigDecimal("10"),
                LoadType.FTL, null, (ContainerType) null, PackageType.PALLETISED, true,
                new Dimensions(new BigDecimal("10"), new BigDecimal("2.6"), new BigDecimal("2.5")), null,
                null, null, null, false, null, false, false, false, false);
        return request(route, cargo);
    }

    private static RateComputeRequest domesticGeneralCargoRequest() {
        RouteRequest route = new RouteRequest(
                JHB_LOCATION_ID, BFN_LOCATION_ID, RouteType.DOMESTIC, null, null, null,
                AddressType.DEPOT, AddressType.DOOR_TO_DOOR);
        return request(route, generalCargo());
    }

    private static CargoRequest generalCargo() {
        return new CargoRequest(
                CargoClass.GENERAL, "8481.80", new BigDecimal("5000"), new BigDecimal("10"),
                LoadType.FTL, null, (ContainerType) null, PackageType.PALLETISED, true, (Dimensions) null, null,
                null, null, null, false, null, false, false, false, false);
    }

    private static RateComputeRequest request(RouteRequest route, CargoRequest cargo) {
        ServiceRequest service = new ServiceRequest(
                ServiceLevel.STANDARD, COLLECTION_DATE, null, false, false, false, false, false, false, false, false);
        return new RateComputeRequest(UUID.randomUUID(), COLLECTION_DATE, route, cargo, service);
    }

    private static RateComputeRequest request(RouteRequest route, CargoRequest cargo, boolean dedicatedVehicle) {
        ServiceRequest service = new ServiceRequest(
                ServiceLevel.STANDARD, COLLECTION_DATE, null, false, false, false, false, false, false, dedicatedVehicle, false);
        return new RateComputeRequest(UUID.randomUUID(), COLLECTION_DATE, route, cargo, service);
    }

    private static RateComputeRequest euroBaseFreightCrossBorderRequest() {
        // 9T_EUR_TEST (V24) is the only vehicle rated for flatbed on LIMPOPO_RURAL:HARARE -- a
        // dedicated, otherwise-unused lane/load_type combination (via TEST_BORDER_2, V8) chosen
        // specifically so this fixture's large 9000kg capacity can't leak into any other test's
        // Phase-1 FTL vehicle eligibility (see V24's comment: capacity alone can't isolate this,
        // since it must exceed every other FTL fixture's weight anyway).
        RouteRequest route = new RouteRequest(
                LIMPOPO_RURAL_LOCATION_ID, HARARE_LOCATION_ID, RouteType.CROSS_BORDER, TEST_BORDER_2_ID, null, null,
                AddressType.DEPOT, AddressType.DOOR_TO_DOOR);
        CargoRequest cargo = new CargoRequest(
                CargoClass.GENERAL, "8481.80", new BigDecimal("8500"), new BigDecimal("10"),
                LoadType.FLATBED, null, (ContainerType) null, PackageType.PALLETISED, true, (Dimensions) null, null,
                null, null, null, false, null, false, false, false, false);
        return request(route, cargo);
    }

    private static RateComputeRequest dedicatedVehicleRequest() {
        // LIMPOPO_RURAL:HARARE (via TEST_BORDER_2, same lane as 9T_EUR_TEST's fixture) rather than
        // JHB_METRO:HARARE -- SMALL_EXPENSIVE_TEST/LARGE_CHEAP_TEST are rated only there (see V24's
        // comment for why). weight=1100, volume=2 keeps the volumetric weight (2*333=666) below
        // gross weight, so chargeable stays 1100kg -- within both vehicles' capacity (1200/2000).
        RouteRequest route = new RouteRequest(
                LIMPOPO_RURAL_LOCATION_ID, HARARE_LOCATION_ID, RouteType.CROSS_BORDER, TEST_BORDER_2_ID, null, null,
                AddressType.DEPOT, AddressType.DOOR_TO_DOOR);
        CargoRequest cargo = new CargoRequest(
                CargoClass.GENERAL, "8481.80", new BigDecimal("1100"), new BigDecimal("2"),
                LoadType.FTL, null, (ContainerType) null, PackageType.PALLETISED, true, (Dimensions) null, null,
                null, null, null, false, null, false, false, false, false);
        return request(route, cargo, true);
    }

    private static RateComputeRequest domesticFragileCargoRequest() {
        RouteRequest route = new RouteRequest(
                JHB_LOCATION_ID, BFN_LOCATION_ID, RouteType.DOMESTIC, null, null, null,
                AddressType.DEPOT, AddressType.DOOR_TO_DOOR);
        CargoRequest cargo = new CargoRequest(
                CargoClass.FRAGILE, "8481.80", new BigDecimal("5000"), new BigDecimal("10"),
                LoadType.FTL, null, (ContainerType) null, PackageType.PALLETISED, true, (Dimensions) null, null,
                null, null, null, false, null, false, false, false, false);
        return request(route, cargo);
    }

    private static RateComputeRequest domesticRequestWithDistanceOverrideAndAbnormalHeightAndLength() {
        RouteRequest route = new RouteRequest(
                JHB_LOCATION_ID, BFN_LOCATION_ID, RouteType.DOMESTIC, null,
                new BigDecimal("500.00"), "Customer-confirmed shorter route via toll road",
                AddressType.DEPOT, AddressType.DOOR_TO_DOOR);
        CargoRequest cargo = new CargoRequest(
                CargoClass.GENERAL, "8481.80", new BigDecimal("5000"), new BigDecimal("10"),
                LoadType.FTL, null, (ContainerType) null, PackageType.PALLETISED, true,
                new Dimensions(new BigDecimal("22.01"), new BigDecimal("2"), new BigDecimal("4.31")), null,
                null, null, null, false, null, false, false, false, false);
        return request(route, cargo);
    }

    private static RateComputeRequest ambiguousRateReeferRequest() {
        // dedicated_vehicle=true is required here, not incidental: the default cost-efficient path
        // calls RoadFreightRateRepository.findActiveRate (singular, Optional<T>) per candidate
        // while building comparison costs -- that call would itself throw
        // IncorrectResultSizeDataAccessException the moment it reaches AMBIGUOUS_TEST_VEHICLE's two
        // conflicting rows, before Stage 6's RateRowResolver (which uses the plural
        // findActiveRates and is actually designed to detect and report the ambiguity) ever runs.
        // dedicated_vehicle=true selects by capacity alone (selectMinimumViable), never touching
        // road_freight_rates during selection, so AmbiguousRateConfigurationException is only
        // reachable end-to-end via this path -- matching RateNotFoundException's own Javadoc note
        // that dedicated_vehicle bypasses Stage 5's rate-availability pre-filtering.
        RouteRequest route = new RouteRequest(
                JHB_LOCATION_ID, HARARE_LOCATION_ID, RouteType.CROSS_BORDER, BEIT_BRIDGE_ID, null, null,
                AddressType.DEPOT, AddressType.DOOR_TO_DOOR);
        CargoRequest cargo = new CargoRequest(
                CargoClass.GENERAL, "8481.80", new BigDecimal("1000"), new BigDecimal("5"),
                LoadType.REEFER, null, (ContainerType) null, PackageType.PALLETISED, true, (Dimensions) null,
                new TemperatureRange(new BigDecimal("2"), new BigDecimal("8")),
                null, null, null, false, null, false, false, false, false);
        return request(route, cargo, true);
    }

    private static RateComputeRequest distanceNotFoundRequest() {
        // LIMPOPO_RURAL -> HARARE via Beit Bridge (V8) is a deliberately inactive lane_distances
        // row with no active alternative for this exact key.
        RouteRequest route = new RouteRequest(
                LIMPOPO_RURAL_LOCATION_ID, HARARE_LOCATION_ID, RouteType.CROSS_BORDER, BEIT_BRIDGE_ID, null, null,
                AddressType.DEPOT, AddressType.DOOR_TO_DOOR);
        return request(route, generalCargo());
    }

    private static RateComputeRequest domesticHazmatLiveAnimalsHighValueRequest() {
        // Synthetic combination for testing this stage's own requires_manual_review gap -- Stage 3
        // doesn't cross-validate cargoClass against liveAnimals/highValueDeclared, so this
        // otherwise-unusual combination is a valid Stage-3-passing request, same reasoning as
        // Stage 9's "multiple simultaneous surcharges" test.
        RouteRequest route = new RouteRequest(
                JHB_LOCATION_ID, BFN_LOCATION_ID, RouteType.DOMESTIC, null, null, null,
                AddressType.DEPOT, AddressType.DOOR_TO_DOOR);
        CargoRequest cargo = new CargoRequest(
                CargoClass.HAZMAT, "2710.12.90", new BigDecimal("5000"), new BigDecimal("10"),
                LoadType.FTL, null, (ContainerType) null, PackageType.BULK, true, (Dimensions) null, null,
                "UN1203", PackingGroup.I, "3", true, new BigDecimal("500000.00"), false, false, true, false);
        return request(route, cargo);
    }
}
