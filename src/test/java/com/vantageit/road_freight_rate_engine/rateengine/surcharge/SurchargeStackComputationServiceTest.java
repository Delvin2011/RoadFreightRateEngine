package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoClass;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ContainerType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.Dimensions;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LoadType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.PackageType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.PackingGroup;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceLevel;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.TemperatureRange;
import com.vantageit.road_freight_rate_engine.rateengine.basefreight.BaseFreightResult;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RateBasis;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies Stage 9 against the V22 seed fixtures (8 new surcharge_rates rows, effective from
 * 2025-01-01) plus FUEL_LEVY (V5, pre-dates this stage). No road_freight_rates fixtures needed —
 * {@link BaseFreightResult}/{@link LaneResolutionResult} are constructed directly, same as
 * {@code BaseFreightComputationServiceTest} constructs its own upstream inputs.
 *
 * <p>{@code baseFreightAmount = 10000.00}, {@code distanceKm = 500.00} is the fixed baseline
 * unless a case needs different values. FUEL_LEVY (22%) always applies, so it appears in every
 * scenario's line items alongside whatever else is triggered.
 */
@SpringBootTest
class SurchargeStackComputationServiceTest {

    private static final LocalDate COLLECTION_DATE = LocalDate.of(2025, 7, 15);
    private static final BigDecimal BASE_FREIGHT_AMOUNT = new BigDecimal("10000.00");
    private static final BigDecimal DISTANCE_KM = new BigDecimal("500.00");

    @Autowired
    private SurchargeStackComputationService surchargeStackComputationService;

    @Test
    void fuelLevyAlwaysAppliesAsPercentOfBaseFreight() {
        SurchargeStackResult result = compute(generalCargo().build());

        assertThat(result.lineItems()).extracting(LineItem::code).contains("FUEL_LEVY");
        assertThat(lineItemFor(result, "FUEL_LEVY").sellZar()).isEqualByComparingTo(new BigDecimal("2200.00"));
    }

    @Test
    void hazmatPg1UpliftAppliesOnlyForPackingGroupI() {
        SurchargeStackResult resultPg1 = compute(generalCargo().hazmatPackingGroup(PackingGroup.I).build());
        assertThat(lineItemFor(resultPg1, "HAZMAT_PG1_UPLIFT").sellZar()).isEqualByComparingTo(new BigDecimal("1500.00"));

        SurchargeStackResult resultPg2 = compute(generalCargo().hazmatPackingGroup(PackingGroup.II).build());
        assertThat(resultPg2.lineItems()).extracting(LineItem::code).doesNotContain("HAZMAT_PG1_UPLIFT");
    }

    @Test
    void reeferRunningTriggersOnLoadTypeReeferAloneWithNoTemperatureSet() {
        SurchargeStackResult result = compute(generalCargo().loadType(LoadType.REEFER).temperatureRangeC(null).build());

        // distance 500.00 * 8.5000 = 4250.00
        assertThat(lineItemFor(result, "REEFER_RUNNING").sellZar()).isEqualByComparingTo(new BigDecimal("4250.00"));
    }

    @Test
    void reeferRunningTriggersOnTemperatureRangeAloneForNonReeferLoadType() {
        SurchargeStackResult result = compute(generalCargo()
                .loadType(LoadType.FTL)
                .temperatureRangeC(new TemperatureRange(new BigDecimal("2"), new BigDecimal("8")))
                .build());

        assertThat(lineItemFor(result, "REEFER_RUNNING").sellZar()).isEqualByComparingTo(new BigDecimal("4250.00"));
    }

    @Test
    void frozenGoodsUpliftComputesAgainstReeferRunningAmountNotBaseFreight() {
        // temperature min=-5 (< 0) triggers frozen goods. Correct: 20% of REEFER_RUNNING's
        // 4250.00 = 850.00. Wrong-if-against-BASE_FREIGHT: 20% of 10000.00 = 2000.00.
        SurchargeStackResult result = compute(generalCargo()
                .loadType(LoadType.REEFER)
                .temperatureRangeC(new TemperatureRange(new BigDecimal("-5"), new BigDecimal("-2")))
                .build());

        assertThat(lineItemFor(result, "FROZEN_GOODS_UPLIFT").sellZar()).isEqualByComparingTo(new BigDecimal("850.00"));
        assertThat(lineItemFor(result, "FROZEN_GOODS_UPLIFT").sellZar()).isNotEqualByComparingTo(new BigDecimal("2000.00"));
    }

    @Test
    void highValueInsuranceLevyComputesAgainstDeclaredValueNotBaseFreight() {
        // 1% of declared_value_zar=500000.00 = 5000.00. Wrong-if-against-BASE_FREIGHT: 1% of
        // 10000.00 = 100.00.
        SurchargeStackResult result = compute(generalCargo()
                .highValueDeclared(true)
                .declaredValueZar(new BigDecimal("500000.00"))
                .build());

        assertThat(lineItemFor(result, "HIGH_VALUE_INSURANCE_LEVY").sellZar()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void liveAnimalWelfareAndLivestockCertBothApplyAsSeparateLineItems() {
        SurchargeStackResult result = compute(generalCargo().liveAnimals(true).build());

        assertThat(result.lineItems()).extracting(LineItem::code).contains("LIVE_ANIMAL_WELFARE", "LIVESTOCK_VEHICLE_CERT");
        assertThat(lineItemFor(result, "LIVE_ANIMAL_WELFARE").sellZar()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(lineItemFor(result, "LIVESTOCK_VEHICLE_CERT").sellZar()).isEqualByComparingTo(new BigDecimal("1200.00"));
    }

    @Test
    void fragileHandlingTriggersOnCargoClassFragileAlone() {
        SurchargeStackResult result = compute(generalCargo().cargoClass(CargoClass.FRAGILE).packageType(PackageType.PALLETISED).build());

        assertThat(lineItemFor(result, "FRAGILE_HANDLING").sellZar()).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    void fragileHandlingTriggersOnPackageTypeCratesAloneForNonFragileCargoClass() {
        SurchargeStackResult result = compute(generalCargo().cargoClass(CargoClass.GENERAL).packageType(PackageType.CRATES).build());

        assertThat(lineItemFor(result, "FRAGILE_HANDLING").sellZar()).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    void nonStackableSpaceFactorAppliesForNonStackableLtlShipment() {
        SurchargeStackResult result = compute(generalCargo().loadType(LoadType.LTL).stackable(false).build());

        // 50% of BASE_FREIGHT (10000.00) = 5000.00 -- the incremental half beyond the 1x already
        // represented by BASE_FREIGHT itself.
        assertThat(lineItemFor(result, "NON_STACKABLE_SPACE_FACTOR").sellZar()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void nonStackableSpaceFactorDoesNotApplyForNonLtlLoadTypeEvenWhenNonStackable() {
        SurchargeStackResult result = compute(generalCargo().loadType(LoadType.FTL).stackable(false).build());

        assertThat(result.lineItems()).extracting(LineItem::code).doesNotContain("NON_STACKABLE_SPACE_FACTOR");
    }

    @Test
    void zeroSurchargesBeyondFuelLevyProducesExactlyOneLineItem() {
        SurchargeStackResult result = compute(generalCargo().build());

        assertThat(result.lineItems()).hasSize(1);
        assertThat(result.lineItems().get(0).code()).isEqualTo("FUEL_LEVY");
        assertThat(result.surchargesTotal()).isEqualByComparingTo(new BigDecimal("2200.00"));
    }

    @Test
    void missingActiveSurchargeRateThrows() {
        // 2024-01-01 is before every seeded surcharge_rates row's effective_from (2025-01-01).
        // General cargo triggers only FUEL_LEVY (the always-applicable rule), so it's
        // deterministically the one rule encountered here, regardless of the registered rules'
        // iteration order.
        RateComputeRequest request = request(generalCargo().build(), LocalDate.of(2024, 1, 1));

        assertThatThrownBy(() -> surchargeStackComputationService.compute(request, baseFreightResult(), lane()))
                .isInstanceOf(SurchargeRateNotFoundException.class)
                .satisfies(e -> {
                    SurchargeRateNotFoundException ex = (SurchargeRateNotFoundException) e;
                    assertThat(ex.getSurchargeCode()).isEqualTo("FUEL_LEVY");
                });
    }

    @Test
    void multipleSimultaneousSurchargesAllAppearAndSumCorrectly() {
        // Synthetic combination for testing this stage's own aggregation logic in isolation --
        // Stage 9 doesn't re-validate Stage 3's rules (same precondition assumption as every prior
        // stage), so this fixture isn't necessarily a fully Stage-3-valid request on its own.
        SurchargeStackResult result = compute(generalCargo()
                .cargoClass(CargoClass.FRAGILE)
                .hazmatPackingGroup(PackingGroup.I)
                .highValueDeclared(true)
                .declaredValueZar(new BigDecimal("500000.00"))
                .build());

        assertThat(result.lineItems()).extracting(LineItem::code).containsExactlyInAnyOrder(
                "FUEL_LEVY", "HAZMAT_PG1_UPLIFT", "HIGH_VALUE_INSURANCE_LEVY", "FRAGILE_HANDLING");
        // 2200.00 + 1500.00 + 5000.00 + 800.00 = 9500.00
        assertThat(result.surchargesTotal()).isEqualByComparingTo(new BigDecimal("9500.00"));
    }

    private SurchargeStackResult compute(CargoRequest cargo) {
        RateComputeRequest request = request(cargo, COLLECTION_DATE);
        return surchargeStackComputationService.compute(request, baseFreightResult(), lane());
    }

    private static LineItem lineItemFor(SurchargeStackResult result, String code) {
        return result.lineItems().stream()
                .filter(li -> li.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No line item with code " + code + " in " + result.lineItems()));
    }

    private static BaseFreightResult baseFreightResult() {
        return new BaseFreightResult(BASE_FREIGHT_AMOUNT, "ZAR", RateBasis.PER_KM, false, null);
    }

    private static LaneResolutionResult lane() {
        return new LaneResolutionResult("TEST_LANE", DISTANCE_KM, UUID.randomUUID(), UUID.randomUUID(), false);
    }

    private static RateComputeRequest request(CargoRequest cargo, LocalDate collectionDate) {
        ServiceRequest service = new ServiceRequest(
                ServiceLevel.STANDARD, collectionDate, null, false, false, false, false, false, false, false, false);
        return new RateComputeRequest(UUID.randomUUID(), collectionDate, null, cargo, service);
    }

    private static CargoRequestBuilder generalCargo() {
        return new CargoRequestBuilder();
    }

    /** Mutable test-only builder mirroring {@link CargoRequest}'s components, GENERAL/FTL defaults. */
    private static final class CargoRequestBuilder {
        private CargoClass cargoClass = CargoClass.GENERAL;
        private LoadType loadType = LoadType.FTL;
        private PackageType packageType = PackageType.PALLETISED;
        private Boolean stackable = true;
        private TemperatureRange temperatureRangeC = null;
        private PackingGroup hazmatPackingGroup = null;
        private Boolean highValueDeclared = false;
        private BigDecimal declaredValueZar = null;
        private Boolean liveAnimals = false;

        CargoRequestBuilder cargoClass(CargoClass value) {
            this.cargoClass = value;
            return this;
        }

        CargoRequestBuilder loadType(LoadType value) {
            this.loadType = value;
            return this;
        }

        CargoRequestBuilder packageType(PackageType value) {
            this.packageType = value;
            return this;
        }

        CargoRequestBuilder stackable(boolean value) {
            this.stackable = value;
            return this;
        }

        CargoRequestBuilder temperatureRangeC(TemperatureRange value) {
            this.temperatureRangeC = value;
            return this;
        }

        CargoRequestBuilder hazmatPackingGroup(PackingGroup value) {
            this.hazmatPackingGroup = value;
            return this;
        }

        CargoRequestBuilder highValueDeclared(boolean value) {
            this.highValueDeclared = value;
            return this;
        }

        CargoRequestBuilder declaredValueZar(BigDecimal value) {
            this.declaredValueZar = value;
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
                    loadType,
                    null,
                    (ContainerType) null,
                    packageType,
                    stackable,
                    (Dimensions) null,
                    temperatureRangeC,
                    null,
                    hazmatPackingGroup,
                    null,
                    highValueDeclared,
                    declaredValueZar,
                    false,
                    false,
                    liveAnimals,
                    false);
        }
    }
}
