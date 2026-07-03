package com.vantageit.road_freight_rate_engine.rateengine.vehicleselection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoClass;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ContainerType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.Dimensions;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LoadType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.PackageType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.PackingGroup;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceLevel;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.TemperatureRange;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.VehicleCategory;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.VehicleCategoryRepository;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies Stage 5 end to end against the V5/V7/V10 seed fixtures. Lane resolution results are
 * constructed directly from known seed UUIDs rather than via {@code LaneResolutionService} (Stage
 * 4, already tested separately), to keep this test focused on vehicle selection specifically.
 */
@SpringBootTest
class VehicleSelectionServiceTest {

    private static final UUID JHB_METRO_ZONE_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID BFN_METRO_ZONE_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID HARARE_ZONE_ID = UUID.fromString("10000000-0000-0000-0000-000000000004");

    private static final LaneResolutionResult JHB_BFN_DOMESTIC =
            new LaneResolutionResult("JHB_METRO:BFN_METRO", new BigDecimal("398.00"), JHB_METRO_ZONE_ID, BFN_METRO_ZONE_ID, false);
    private static final LaneResolutionResult JHB_HARARE_CROSS_BORDER =
            new LaneResolutionResult("JHB_METRO:HARARE", new BigDecimal("1225.00"), JHB_METRO_ZONE_ID, HARARE_ZONE_ID, false);

    @Autowired
    private VehicleSelectionService vehicleSelectionService;

    @Autowired
    private VehicleCategoryRepository vehicleCategoryRepository;

    @Test
    void simpleCaseSelectsTheOnlyEligibleVehicle() {
        // 20,000kg exceeds 4T_RIGID (4000) and 8T_RIGID (8000), fits only 34T_SEMI (34000).
        CargoRequest cargo = cargo(CargoClass.GENERAL, LoadType.FTL, new BigDecimal("20000"), new BigDecimal("50"));
        RateComputeRequest request = request(cargo, false);

        VehicleSelectionResult result = vehicleSelectionService.selectVehicle(request, JHB_BFN_DOMESTIC);

        assertThat(result.selectedVehicleCategoryCode()).isEqualTo("34T_SEMI");
        assertThat(result.selectionReason()).isEqualTo(SelectionReason.COST_EFFICIENT);
        assertThat(result.eligibleVehicleCount()).isEqualTo(1);
        assertThat(result.requiresPermit()).isFalse(); // 34T_SEMI defaults to requires_permit=0
    }

    @Test
    void costEfficientSelectionPicksTheCheapestEligibleVehicle() {
        // gross=3000, volume=10 -> chargeable weight = max(3000, 10*333) = 3330kg, within three
        // vehicles' capacity (4T_RIGID/8T_RIGID/34T_SEMI); 7T_RIGID_LOWRATE's capacity (3200kg) is
        // deliberately just below this, so it's excluded here and has its own isolated fixture in
        // minimumChargeFlooringChangesTheWinner instead. Seeded costs on JHB_METRO:BFN_METRO:
        // 8T_RIGID=4776.00, 34T_SEMI=7363.00, 4T_RIGID=7960.00.
        CargoRequest cargo = cargo(CargoClass.GENERAL, LoadType.FTL, new BigDecimal("3000"), new BigDecimal("10"));
        RateComputeRequest request = request(cargo, false);

        VehicleSelectionResult result = vehicleSelectionService.selectVehicle(request, JHB_BFN_DOMESTIC);

        assertThat(result.selectedVehicleCategoryCode()).isEqualTo("8T_RIGID");
        assertThat(result.selectionReason()).isEqualTo(SelectionReason.COST_EFFICIENT);
        assertThat(result.eligibleVehicleCount()).isEqualTo(3);
    }

    @Test
    void dedicatedVehiclePicksSmallestCapacityNotCheapest() {
        // Same eligible set as the cost-efficient test, but dedicated_vehicle=true. 4T_RIGID is
        // the smallest by max_weight_kg (4000 < 8000 < 34000) despite being the most expensive
        // (7960.00, vs. 8T_RIGID's 4776.00) — proves cost is ignored on this path.
        CargoRequest cargo = cargo(CargoClass.GENERAL, LoadType.FTL, new BigDecimal("3000"), new BigDecimal("10"));
        RateComputeRequest request = request(cargo, true);

        VehicleSelectionResult result = vehicleSelectionService.selectVehicle(request, JHB_BFN_DOMESTIC);

        assertThat(result.selectedVehicleCategoryCode()).isEqualTo("4T_RIGID");
        assertThat(result.selectionReason()).isEqualTo(SelectionReason.DEDICATED_MINIMUM_VIABLE);
        assertThat(result.eligibleVehicleCount()).isEqualTo(3);
    }

    @Test
    void minimumChargeFlooringChangesTheWinner() {
        // Isolated fixture: gross=1500, volume=5 -> chargeable weight = max(1500, 1665) = 1665kg,
        // within 7T_RIGID_LOWRATE's capacity (3200kg) and 8T_RIGID's, but excludes 4T_RIGID/
        // 34T_SEMI from mattering here since neither can beat 8T_RIGID's cost regardless (this test
        // only asserts on the winner, not eligibleVehicleCount, so their presence is immaterial).
        // Without minimum_charge, 7T_RIGID_LOWRATE's raw cost (1.00 * 398km = 398.00) would beat
        // every other vehicle by a wide margin — but its minimum_charge=10000.00 floors it above
        // all of them, so 8T_RIGID (4776.00, no flooring effect since its raw cost already exceeds
        // its own minimum_charge) wins instead.
        CargoRequest cargo = cargo(CargoClass.GENERAL, LoadType.FTL, new BigDecimal("1500"), new BigDecimal("5"));
        RateComputeRequest request = request(cargo, false);

        VehicleSelectionResult result = vehicleSelectionService.selectVehicle(request, JHB_BFN_DOMESTIC);

        assertThat(result.selectedVehicleCategoryCode())
                .as("7T_RIGID_LOWRATE's floored cost (10000.00) loses to 8T_RIGID (4776.00), even though its raw per-km cost (398.00) would otherwise win")
                .isEqualTo("8T_RIGID");
    }

    @Test
    void costEfficientTieBrokenDeterministicallyByVehicleCode() {
        // 5T_RIGID_A and 5T_RIGID_B have identical capacity and an identical flat rate (3000.00)
        // on JHB_METRO:HARARE — a real cost tie. Without an explicit secondary sort key, which one
        // wins would depend on incidental DB row order rather than being guaranteed. "_A" sorts
        // before "_B", so it must win consistently.
        CargoRequest cargo = cargo(CargoClass.GENERAL, LoadType.FTL, new BigDecimal("1500"), new BigDecimal("5"));
        RateComputeRequest request = request(cargo, false);

        VehicleSelectionResult result = vehicleSelectionService.selectVehicle(request, JHB_HARARE_CROSS_BORDER);

        assertThat(result.selectedVehicleCategoryCode()).isEqualTo("5T_RIGID_A");
    }

    @Test
    void mixedCurrencyRatesComparedNumericallyWithoutFxConversion() {
        // KNOWN GAP, pinned rather than fixed — see the vehicle_selection_currency_comparison_deferred
        // project memory. gross=1900, volume=3 -> chargeable weight = max(1900, 999) = 1900kg,
        // which excludes 5T_RIGID_A/B (capacity 1800kg) so this test is isolated from the tie-
        // breaking fixture. 8T_RIGID's ZAR rate costs 12250.00; 2T_RIGID's USD rate costs a
        // numerically smaller 6125.00, so it wins this comparison even though no currency
        // conversion has been applied — 6125 USD is not actually cheaper than 12250 ZAR in real
        // terms. If this test ever starts failing because FX conversion was implemented, update it
        // (and the memory) together.
        CargoRequest cargo = cargo(CargoClass.GENERAL, LoadType.FTL, new BigDecimal("1900"), new BigDecimal("3"));
        RateComputeRequest request = request(cargo, false);

        VehicleSelectionResult result = vehicleSelectionService.selectVehicle(request, JHB_HARARE_CROSS_BORDER);

        assertThat(result.selectedVehicleCategoryCode())
                .as("KNOWN GAP (see vehicle_selection_currency_comparison_deferred memory): 2T_RIGID's "
                        + "raw USD cost (6125.00) is numerically lower than 8T_RIGID's ZAR cost (12250.00) "
                        + "and wins with no FX conversion applied, even though 6125 USD is actually far more "
                        + "expensive than 12250 ZAR in real terms. If this assertion starts failing because "
                        + "FX conversion was implemented, update this test and the memory together.")
                .isEqualTo("2T_RIGID");
    }

    @Test
    void dedicatedVehicleWithOnlyOneEligibleVehicleSelectsIt() {
        // Same fixture as simpleCaseSelectsTheOnlyEligibleVehicle, but dedicated_vehicle=true —
        // confirms the single-candidate path works identically regardless of selection strategy.
        CargoRequest cargo = cargo(CargoClass.GENERAL, LoadType.FTL, new BigDecimal("20000"), new BigDecimal("50"));
        RateComputeRequest request = request(cargo, true);

        VehicleSelectionResult result = vehicleSelectionService.selectVehicle(request, JHB_BFN_DOMESTIC);

        assertThat(result.selectedVehicleCategoryCode()).isEqualTo("34T_SEMI");
        assertThat(result.selectionReason()).isEqualTo(SelectionReason.DEDICATED_MINIMUM_VIABLE);
        assertThat(result.eligibleVehicleCount()).isEqualTo(1);
    }

    @Test
    void selectsAVehicleOnACrossBorderLane() {
        // All prior happy-path tests use the domestic JHB_METRO:BFN_METRO lane; this proves
        // selection also works on a cross-border lane. Weight/volume deliberately excludes
        // 2T_RIGID (capacity 2000kg/10cbm) so this test is isolated from the mixed-currency fixture.
        CargoRequest cargo = cargo(CargoClass.GENERAL, LoadType.FTL, new BigDecimal("5000"), new BigDecimal("20"));
        RateComputeRequest request = request(cargo, false);

        VehicleSelectionResult result = vehicleSelectionService.selectVehicle(request, JHB_HARARE_CROSS_BORDER);

        assertThat(result.selectedVehicleCategoryCode()).isEqualTo("8T_RIGID");
        assertThat(result.selectionReason()).isEqualTo(SelectionReason.COST_EFFICIENT);
    }

    @Test
    void metroOnlyVehicleExcludedWhenDestinationIsNotTier1() {
        // BFN_METRO is tier 2, so 1T_BAKKIE (zone_restriction = METRO_ONLY) must be excluded even
        // though its capacity (1000kg/6cbm) comfortably covers this shipment.
        CargoRequest cargo = cargo(CargoClass.GENERAL, LoadType.FTL, new BigDecimal("500"), new BigDecimal("3"));
        RateComputeRequest request = request(cargo, false);

        List<VehicleCategory> eligible = vehicleSelectionService.findEligibleVehicles(request, JHB_BFN_DOMESTIC);

        assertThat(eligible).extracting(VehicleCategory::getCode).doesNotContain("1T_BAKKIE");
        assertThat(eligible).isNotEmpty();
    }

    @Test
    void noVehicleCapacityExceedsWeightThrowsNoEligibleVehicle() {
        // 40,000kg exceeds every seeded vehicle's capacity, including 34T_SEMI's 34,000kg ceiling.
        CargoRequest cargo = cargo(CargoClass.GENERAL, LoadType.FTL, new BigDecimal("40000"), new BigDecimal("10"));
        RateComputeRequest request = request(cargo, false);

        assertThatThrownBy(() -> vehicleSelectionService.selectVehicle(request, JHB_BFN_DOMESTIC))
                .isInstanceOf(NoEligibleVehicleException.class)
                .satisfies(e -> {
                    NoEligibleVehicleException ex = (NoEligibleVehicleException) e;
                    assertThat(ex.getLoadType()).isEqualTo("ftl");
                    assertThat(ex.getLaneKey()).isEqualTo("JHB_METRO:BFN_METRO");
                });
    }

    @Test
    void eligibleVehicleWithNoRateForLaneThrowsNoRateAvailable() {
        // TANKER is the only tanker-eligible vehicle and easily has capacity, but no
        // road_freight_rates row exists for JHB_METRO:HARARE at all.
        CargoRequest cargo = cargo(CargoClass.GENERAL, LoadType.TANKER, new BigDecimal("5000"), new BigDecimal("10"));
        RateComputeRequest request = request(cargo, false);

        assertThatThrownBy(() -> vehicleSelectionService.selectVehicle(request, JHB_HARARE_CROSS_BORDER))
                .isInstanceOf(NoRateAvailableForLaneException.class)
                .satisfies(e -> {
                    NoRateAvailableForLaneException ex = (NoRateAvailableForLaneException) e;
                    assertThat(ex.getLaneKey()).isEqualTo("JHB_METRO:HARARE");
                    assertThat(ex.getVehicleCategoryCodesConsidered()).contains("TANKER");
                });
    }

    @Test
    void vehicleCapacityCheckThrowsWhenCalledDirectlyWithInconsistentInput() {
        VehicleCategory eightTonRigid = vehicleCategoryRepository.findByCode("8T_RIGID").orElseThrow();

        assertThatThrownBy(() -> vehicleSelectionService.checkVehicleCapacity(eightTonRigid, new BigDecimal("9000")))
                .isInstanceOf(VehicleCapacityExceededException.class)
                .satisfies(e -> {
                    VehicleCapacityExceededException ex = (VehicleCapacityExceededException) e;
                    assertThat(ex.getVehicleCategoryCode()).isEqualTo("8T_RIGID");
                    assertThat(ex.getMaxWeightKg()).isEqualByComparingTo(new BigDecimal("8000.00"));
                    assertThat(ex.getChargeableWeightKg()).isEqualByComparingTo(new BigDecimal("9000"));
                });
    }

    @Test
    void reeferCargoSelectsTheReeferCapableVehicle() {
        CargoRequest cargo = cargo(CargoClass.PERISHABLE, LoadType.REEFER, new BigDecimal("3000"), new BigDecimal("10"));
        RateComputeRequest request = request(cargo, false);

        VehicleSelectionResult result = vehicleSelectionService.selectVehicle(request, JHB_BFN_DOMESTIC);

        assertThat(result.selectedVehicleCategoryCode()).isEqualTo("REEFER_8T");
        assertThat(result.eligibleVehicleCount()).isEqualTo(1);
        assertThat(result.requiresPermit()).isTrue(); // REEFER_8T is seeded with requires_permit=1
    }

    private static CargoRequest cargo(CargoClass cargoClass, LoadType loadType, BigDecimal grossWeightKg, BigDecimal volumeCbm) {
        TemperatureRange temperatureRange = loadType == LoadType.REEFER || cargoClass == CargoClass.PERISHABLE
                ? new TemperatureRange(new BigDecimal("2"), new BigDecimal("8"))
                : null;
        return new CargoRequest(
                cargoClass,
                "0000.00",
                grossWeightKg,
                volumeCbm,
                loadType,
                null,
                (ContainerType) null,
                PackageType.PALLETISED,
                false,
                (Dimensions) null,
                temperatureRange,
                null,
                (PackingGroup) null,
                null,
                false,
                null,
                false,
                false,
                false,
                false);
    }

    private static RateComputeRequest request(CargoRequest cargo, boolean dedicatedVehicle) {
        ServiceRequest service = new ServiceRequest(
                ServiceLevel.STANDARD, LocalDate.of(2025, 7, 15), null, false, false, false, false, dedicatedVehicle, false);
        return new RateComputeRequest(UUID.randomUUID(), LocalDate.of(2025, 7, 15), null, cargo, service);
    }
}
