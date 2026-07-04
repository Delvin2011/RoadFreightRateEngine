package com.vantageit.road_freight_rate_engine.rateengine.clearance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.AddressType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoClass;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ContainerType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.Dimensions;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LoadType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.PackageType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceLevel;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceRequest;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies pipeline Stage 6 against the V23 seed fixtures: 7 new surcharge_rates rows (effective
 * from 2025-01-01), plus BEIT_BRIDGE (ZA->ZW, qualifies for ZINARA/carbon tax) and
 * CLEARANCE_TEST_NON_ZIM_MOZ (ZA->BW, does not qualify) as border_posts fixtures.
 *
 * <p><b>Known gap, pinned not fixed</b>: {@code clearancesTotal} is a raw sum across line items
 * whose underlying {@code surcharge_rates} rows are a genuine mix of ZAR and USD (per the doc) —
 * this sums them as if they were the same unit, the same class of issue
 * {@code AccessorialCurrencyMismatchException} addressed in Stage 7. Stage 9 never actually
 * exercised this (all its surcharges were seeded ZAR), so this is the first catalogue-driven stage
 * to genuinely hit it. Flagged explicitly rather than silently fixed or silently ignored — the
 * totals below assert the *current* raw-sum behavior on purpose, so a future currency-consistency
 * fix can't silently change this behavior unnoticed.
 */
@SpringBootTest
class ClearanceComputationServiceTest {

    private static final LocalDate COLLECTION_DATE = LocalDate.of(2025, 7, 15);
    private static final UUID BEIT_BRIDGE_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID NON_ZIM_MOZ_BORDER_ID = UUID.fromString("20000000-0000-0000-0000-000000000003");

    @Autowired
    private ClearanceComputationService clearanceComputationService;

    @Test
    void domesticShipmentProducesZeroClearanceLineItems() {
        ClearanceResult result = compute(route(RouteType.DOMESTIC, null), generalCargo().build());

        assertThat(result.lineItems()).isEmpty();
        assertThat(result.clearancesTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void crossBorderShipmentToZimMozProducesAllApplicableFees() {
        ClearanceResult result = compute(route(RouteType.CROSS_BORDER, BEIT_BRIDGE_ID), generalCargo().build());

        assertThat(result.lineItems()).extracting(LineItem::code).containsExactlyInAnyOrder(
                "BORDER_CLEARING_AGENT_FEE", "COMESA_LIABILITY_INSURANCE", "SARS_CPF",
                "ZINARA_ROAD_ACCESS_FEE", "CARBON_TAX_LEVY");
        assertThat(lineItemFor(result, "BORDER_CLEARING_AGENT_FEE").sellZar()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(lineItemFor(result, "COMESA_LIABILITY_INSURANCE").sellZar()).isEqualByComparingTo(new BigDecimal("45.00"));
        assertThat(lineItemFor(result, "SARS_CPF").sellZar()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(lineItemFor(result, "ZINARA_ROAD_ACCESS_FEE").sellZar()).isEqualByComparingTo(new BigDecimal("30.00"));
        assertThat(lineItemFor(result, "CARBON_TAX_LEVY").sellZar()).isEqualByComparingTo(new BigDecimal("15.00"));
        // 1500 + 45 + 250 + 30 + 15 -- raw sum across ZAR and USD line items, see class Javadoc.
        assertThat(result.clearancesTotal()).isEqualByComparingTo(new BigDecimal("1840.00"));
    }

    @Test
    void crossBorderShipmentToNonZimMozCountryOmitsCountryGatedFees() {
        ClearanceResult result = compute(route(RouteType.CROSS_BORDER, NON_ZIM_MOZ_BORDER_ID), generalCargo().build());

        assertThat(result.lineItems()).extracting(LineItem::code).containsExactlyInAnyOrder(
                "BORDER_CLEARING_AGENT_FEE", "COMESA_LIABILITY_INSURANCE", "SARS_CPF");
        assertThat(result.lineItems()).extracting(LineItem::code)
                .doesNotContain("ZINARA_ROAD_ACCESS_FEE", "CARBON_TAX_LEVY");
        // 1500 + 45 + 250
        assertThat(result.clearancesTotal()).isEqualByComparingTo(new BigDecimal("1795.00"));
    }

    @Test
    void hazmatCargoProducesAdgTransportPermitRegardlessOfRouteType() {
        ClearanceResult result = compute(route(RouteType.DOMESTIC, null), generalCargo().cargoClass(CargoClass.HAZMAT).build());

        assertThat(lineItemFor(result, "ADG_TRANSPORT_PERMIT").sellZar()).isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    void liveAnimalsProducesVetHealthCertificateRegardlessOfRouteType() {
        ClearanceResult result = compute(route(RouteType.DOMESTIC, null), generalCargo().liveAnimals(true).build());

        assertThat(lineItemFor(result, "VET_HEALTH_CERTIFICATE").sellZar()).isEqualByComparingTo(new BigDecimal("350.00"));
    }

    @Test
    void crossBorderMissingBorderClearingAgentFeeRateThrowsClearingFeeRequired() {
        // BORDER_CLEARING_AGENT_FEE's row is bounded (effective_to=2025-12-31), unlike the other
        // cross-border fees (open-ended) -- 2026-01-15 is after its expiry but well within
        // everything else's active range, isolating this specific rate as the missing one.
        RateComputeRequest request = request(route(RouteType.CROSS_BORDER, BEIT_BRIDGE_ID), generalCargo().build(), LocalDate.of(2026, 1, 15));

        assertThatThrownBy(() -> clearanceComputationService.compute(request, lane()))
                .isInstanceOf(ClearingFeeRequiredException.class);
    }

    @Test
    void domesticShipmentWithHazmatAndLiveAnimalsAppliesComplianceChargesButNotCrossBorderOnes() {
        CargoRequest cargo = generalCargo().cargoClass(CargoClass.HAZMAT).liveAnimals(true).build();

        ClearanceResult result = compute(route(RouteType.DOMESTIC, null), cargo);

        assertThat(result.lineItems()).extracting(LineItem::code)
                .containsExactlyInAnyOrder("ADG_TRANSPORT_PERMIT", "VET_HEALTH_CERTIFICATE");
        assertThat(result.lineItems()).extracting(LineItem::code)
                .doesNotContain("BORDER_CLEARING_AGENT_FEE", "COMESA_LIABILITY_INSURANCE", "SARS_CPF",
                        "ZINARA_ROAD_ACCESS_FEE", "CARBON_TAX_LEVY");
    }

    private ClearanceResult compute(RouteRequest route, CargoRequest cargo) {
        RateComputeRequest request = request(route, cargo, COLLECTION_DATE);
        return clearanceComputationService.compute(request, lane());
    }

    private static LineItem lineItemFor(ClearanceResult result, String code) {
        return result.lineItems().stream()
                .filter(li -> li.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No line item with code " + code + " in " + result.lineItems()));
    }

    private static LaneResolutionResult lane() {
        return new LaneResolutionResult("TEST_LANE", new BigDecimal("500.00"), UUID.randomUUID(), UUID.randomUUID(), false);
    }

    private static RateComputeRequest request(RouteRequest route, CargoRequest cargo, LocalDate collectionDate) {
        ServiceRequest service = new ServiceRequest(
                ServiceLevel.STANDARD, collectionDate, null, false, false, false, false, false, false, false, false);
        return new RateComputeRequest(UUID.randomUUID(), collectionDate, route, cargo, service);
    }

    private static RouteRequest route(RouteType routeType, UUID borderPostId) {
        return new RouteRequest(
                UUID.randomUUID(), UUID.randomUUID(), routeType, borderPostId, null, null,
                AddressType.DEPOT, AddressType.DOOR_TO_DOOR);
    }

    private static CargoRequestBuilder generalCargo() {
        return new CargoRequestBuilder();
    }

    /** Mutable test-only builder mirroring {@link CargoRequest}'s components, GENERAL/FTL defaults. */
    private static final class CargoRequestBuilder {
        private CargoClass cargoClass = CargoClass.GENERAL;
        private Boolean liveAnimals = false;

        CargoRequestBuilder cargoClass(CargoClass value) {
            this.cargoClass = value;
            return this;
        }

        CargoRequestBuilder liveAnimals(boolean value) {
            this.liveAnimals = value;
            return this;
        }

        CargoRequest build() {
            return new CargoRequest(
                    cargoClass,
                    "0000.00",
                    new BigDecimal("1000"),
                    new BigDecimal("3"),
                    LoadType.FTL,
                    null,
                    (ContainerType) null,
                    PackageType.PALLETISED,
                    true,
                    (Dimensions) null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    null,
                    false,
                    false,
                    liveAnimals,
                    false);
        }
    }
}
