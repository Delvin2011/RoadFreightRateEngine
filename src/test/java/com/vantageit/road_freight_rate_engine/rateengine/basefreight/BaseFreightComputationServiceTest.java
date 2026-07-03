package com.vantageit.road_freight_rate_engine.rateengine.basefreight;

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
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RateBasis;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionResult;
import com.vantageit.road_freight_rate_engine.rateengine.vehicleselection.SelectionReason;
import com.vantageit.road_freight_rate_engine.rateengine.vehicleselection.VehicleSelectionResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies Stage 6 end to end against the V13/V14 seed fixtures (a dedicated
 * {@code BF_TEST_VEHICLE}/{@code BF_TEST_VEHICLE_2} with per-scenario synthetic {@code lane_key}
 * values, isolated from every other stage's fixtures). This stage doesn't re-run Stage 4/5, so
 * {@link LaneResolutionResult} and {@link VehicleSelectionResult} are constructed directly rather
 * than via those services.
 */
@SpringBootTest
class BaseFreightComputationServiceTest {

    private static final String VEHICLE_CODE = "BF_TEST_VEHICLE";
    private static final LocalDate RATE_DATE = LocalDate.of(2025, 7, 15);

    @Autowired
    private BaseFreightComputationService baseFreightComputationService;

    @Test
    void perKmBasisNoFlooring() {
        // distance=100.00, gross=1000, volume=1 -> chargeable=max(1000, 333)=1000.
        // raw = 10.0000 * 100.00 * 1000 / 1000 = 1000.00, min_charge=500.00 doesn't floor it.
        BaseFreightResult result = compute("BF_TEST_PERKM_NOFLOOR", LoadType.FTL, new BigDecimal("100.00"),
                new BigDecimal("1000"), new BigDecimal("1"), null);

        assertThat(result.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(result.rateBasisUsed()).isEqualTo(RateBasis.PER_KM);
        assertThat(result.minimumChargeApplied()).isFalse();
        assertThat(result.lineItemComment()).isNull();
    }

    @Test
    void perKmBasisFlooringApplies() {
        // Same raw cost as above (1000.00), but this lane's min_charge=5000.00 floors it.
        BaseFreightResult result = compute("BF_TEST_PERKM_FLOOR", LoadType.FTL, new BigDecimal("100.00"),
                new BigDecimal("1000"), new BigDecimal("1"), null);

        assertThat(result.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.minimumChargeApplied()).isTrue();
        assertThat(result.lineItemComment()).isEqualTo("Minimum charge applied.");
    }

    @Test
    void exactEqualityBoundaryOnMinimumChargeDoesNotFloor() {
        // Same raw cost as perKmBasisNoFlooring (1000.0000), but minimum_charge is exactly
        // 1000.00 too. compareTo == 0 is not "< 0" in the flooring check, so this must NOT be
        // treated as floored — pinning that exact-equality behavior explicitly.
        BaseFreightResult result = compute("BF_TEST_PERKM_EXACT_MIN", LoadType.FTL, new BigDecimal("100.00"),
                new BigDecimal("1000"), new BigDecimal("1"), null);

        assertThat(result.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("1000.0000"));
        assertThat(result.minimumChargeApplied()).isFalse();
        assertThat(result.lineItemComment()).isNull();
    }

    @Test
    void nullMinimumChargeNeverFloors() {
        // Distinct from "raw exceeds a non-null floor": minimum_charge is genuinely NULL on this
        // row, not just a low value that happens not to trigger flooring.
        BaseFreightResult result = compute("BF_TEST_PERKM_NULL_MIN", LoadType.FTL, new BigDecimal("100.00"),
                new BigDecimal("1000"), new BigDecimal("1"), null);

        assertThat(result.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("1000.0000"));
        assertThat(result.minimumChargeApplied()).isFalse();
    }

    @Test
    void nonZarCurrencyPropagatesUnconverted() {
        // Distinct from flatBasisIgnoresDistanceAndWeight's USD/FLAT case: this is EUR on a
        // PER_KM row, proving currency pass-through isn't tied to one particular rate_basis.
        BaseFreightResult result = compute("BF_TEST_PERKM_EUR", LoadType.FTL, new BigDecimal("100.00"),
                new BigDecimal("1000"), new BigDecimal("1"), null);

        assertThat(result.currency()).isEqualTo("EUR");
        assertThat(result.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("1000.0000"));
    }

    @Test
    void singleBasisFlatOnlyFtlLaneDoesNotMisfirePrecedenceLogic() {
        // BF_TEST_FLAT (used by flatBasisIgnoresDistanceAndWeight) has exactly one active row —
        // confirms the precedence/ambiguity resolution path isn't triggered when there's nothing
        // to resolve, and the single row is returned directly.
        BaseFreightResult result = compute("BF_TEST_FLAT", LoadType.FTL, new BigDecimal("500.00"),
                new BigDecimal("5000"), new BigDecimal("10"), null);

        assertThat(result.rateBasisUsed()).isEqualTo(RateBasis.FLAT);
    }

    @Test
    void singleBasisPerKmOnlyFtlLaneDoesNotMisfirePrecedenceLogic() {
        // BF_TEST_PERKM_NOFLOOR (used by perKmBasisNoFlooring) has exactly one active row — same
        // confirmation as above, for the other basis.
        BaseFreightResult result = compute("BF_TEST_PERKM_NOFLOOR", LoadType.FTL, new BigDecimal("100.00"),
                new BigDecimal("1000"), new BigDecimal("1"), null);

        assertThat(result.rateBasisUsed()).isEqualTo(RateBasis.PER_KM);
    }

    @Test
    void flatBasisIgnoresDistanceAndWeight() {
        // Distance/weight deliberately large and nonsensical to prove they're ignored for FLAT.
        // Currency is USD on this fixture, proving it's carried through, not hardcoded to ZAR.
        BaseFreightResult result = compute("BF_TEST_FLAT", LoadType.FTL, new BigDecimal("9999.00"),
                new BigDecimal("99999"), new BigDecimal("500"), null);

        assertThat(result.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("8000.0000"));
        assertThat(result.currency()).isEqualTo("USD");
        assertThat(result.rateBasisUsed()).isEqualTo(RateBasis.FLAT);
        assertThat(result.minimumChargeApplied()).isFalse();
    }

    @Test
    void flatTakesPrecedenceOverPerKmForFtl() {
        // Two active rows on the same FTL lane: flat (6000.00) and per_km (99.0000, which at any
        // nontrivial distance/weight would compute far more than 6000.00) — flat must win.
        BaseFreightResult result = compute("BF_TEST_PRECEDENCE", LoadType.FTL, new BigDecimal("500.00"),
                new BigDecimal("5000"), new BigDecimal("10"), null);

        assertThat(result.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("6000.0000"));
        assertThat(result.rateBasisUsed()).isEqualTo(RateBasis.FLAT);
    }

    @Test
    void ambiguousNonFtlMultiRowThrows() {
        // Two conflicting rows (per_ton + per_cbm) on a reefer lane — the flat-vs-per-km FTL
        // exception doesn't apply to non-FTL load types, so this must be rejected outright.
        RateComputeRequest request = request(cargo(LoadType.REEFER, new BigDecimal("5000"), new BigDecimal("10")));
        LaneResolutionResult lane = lane("BF_TEST_AMBIGUOUS", new BigDecimal("500.00"));
        VehicleSelectionResult vehicle = vehicle();

        assertThatThrownBy(() -> baseFreightComputationService.compute(request, lane, vehicle))
                .isInstanceOf(AmbiguousRateConfigurationException.class)
                .satisfies(e -> {
                    AmbiguousRateConfigurationException ex = (AmbiguousRateConfigurationException) e;
                    assertThat(ex.getLaneKey()).isEqualTo("BF_TEST_AMBIGUOUS");
                    assertThat(ex.getConflictingRows()).extracting(AmbiguousRateConfigurationException.ConflictingRow::rateBasis)
                            .containsExactlyInAnyOrder(RateBasis.PER_TON, RateBasis.PER_CBM);
                });
    }

    @Test
    void zeroMatchingRowsThrowsRateNotFound() {
        RateComputeRequest request = request(cargo(LoadType.FTL, new BigDecimal("5000"), new BigDecimal("10")));
        LaneResolutionResult lane = lane("BF_TEST_NO_SUCH_LANE", new BigDecimal("500.00"));
        VehicleSelectionResult vehicle = vehicle();

        assertThatThrownBy(() -> baseFreightComputationService.compute(request, lane, vehicle))
                .isInstanceOf(RateNotFoundException.class)
                .satisfies(e -> {
                    RateNotFoundException ex = (RateNotFoundException) e;
                    assertThat(ex.getLaneKey()).isEqualTo("BF_TEST_NO_SUCH_LANE");
                    assertThat(ex.getVehicleCategoryCode()).isEqualTo(VEHICLE_CODE);
                });
    }

    @Test
    void flatVersusPerKmConflictOnNonFtlLoadTypeThrowsAmbiguous() {
        // Same flat+per_km rate_basis pair as flatTakesPrecedenceOverPerKmForFtl, but on a reefer
        // lane instead of FTL — proves the flat-wins rule is scoped to FTL specifically in code,
        // not just documented as such, since this exact pairing must NOT auto-resolve here.
        RateComputeRequest request = request(cargo(LoadType.REEFER, new BigDecimal("5000"), new BigDecimal("10")));
        LaneResolutionResult lane = lane("BF_TEST_FLAT_PERKM_NON_FTL", new BigDecimal("500.00"));
        VehicleSelectionResult vehicle = vehicle();

        assertThatThrownBy(() -> baseFreightComputationService.compute(request, lane, vehicle))
                .isInstanceOf(AmbiguousRateConfigurationException.class)
                .satisfies(e -> {
                    AmbiguousRateConfigurationException ex = (AmbiguousRateConfigurationException) e;
                    assertThat(ex.getLaneKey()).isEqualTo("BF_TEST_FLAT_PERKM_NON_FTL");
                    assertThat(ex.getConflictingRows()).extracting(AmbiguousRateConfigurationException.ConflictingRow::rateBasis)
                            .containsExactlyInAnyOrder(RateBasis.FLAT, RateBasis.PER_KM);
                });
    }

    @Test
    void inactiveOnlyRowThrowsRateNotFound() {
        // The only row for this lane/vehicle/load_type is is_active=0 — must not be silently
        // returned as if it were a valid active rate.
        RateComputeRequest request = request(cargo(LoadType.FTL, new BigDecimal("1000"), new BigDecimal("1")));
        LaneResolutionResult lane = lane("BF_TEST_INACTIVE_ONLY", new BigDecimal("100.00"));
        VehicleSelectionResult vehicle = vehicle();

        assertThatThrownBy(() -> baseFreightComputationService.compute(request, lane, vehicle))
                .isInstanceOf(RateNotFoundException.class);
    }

    @Test
    void collectionDateAtEffectiveFromBoundaryResolvesInclusively() {
        // Row is bounded effective_from=2025-06-01, effective_to=2025-06-30. Requesting exactly
        // the start date must succeed (effective_from <= asOfDate is inclusive).
        BaseFreightResult result = computeAsOf("BF_TEST_DATE_BOUNDARY", LocalDate.of(2025, 6, 1));

        assertThat(result.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("1000.0000"));
    }

    @Test
    void collectionDateAtEffectiveToBoundaryResolvesInclusively() {
        // Same row, requesting exactly the end date must also succeed (effective_to >= asOfDate
        // is inclusive).
        BaseFreightResult result = computeAsOf("BF_TEST_DATE_BOUNDARY", LocalDate.of(2025, 6, 30));

        assertThat(result.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("1000.0000"));
    }

    @Test
    void vehicleCategoryScopingIsEnforcedAcrossRowsSharingLaneAndLoadType() {
        // Two rows share lane_key="BF_TEST_VEHICLE_SCOPING" + load_type=ftl, but belong to
        // different vehicle categories (rate 10.0000 vs. 99.0000) — resolving for one vehicle must
        // return only that vehicle's own row, not treat the other vehicle's row as a conflict.
        LaneResolutionResult lane = lane("BF_TEST_VEHICLE_SCOPING", new BigDecimal("100.00"));
        RateComputeRequest request = request(cargo(LoadType.FTL, new BigDecimal("1000"), new BigDecimal("1")));

        BaseFreightResult forVehicle1 = baseFreightComputationService.compute(request, lane, vehicle("BF_TEST_VEHICLE"));
        BaseFreightResult forVehicle2 = baseFreightComputationService.compute(request, lane, vehicle("BF_TEST_VEHICLE_2"));

        assertThat(forVehicle1.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("1000.0000"));
        assertThat(forVehicle2.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("9900.0000"));
    }

    @Test
    void lineItemCommentIsStrictlyNullNotEmptyStringWhenNoFlooringOccurs() {
        // isNull() fails on an empty string too, so this already pins "null, not empty string" —
        // but kept as its own labeled case since a future refactor could plausibly introduce an
        // empty-string default instead of null without any of the other tests catching it.
        BaseFreightResult result = compute("BF_TEST_PERKM_NOFLOOR", LoadType.FTL, new BigDecimal("100.00"),
                new BigDecimal("1000"), new BigDecimal("1"), null);

        assertThat(result.lineItemComment()).isNull();
    }

    @Test
    void ltlNoFlooring() {
        // 5 pallets * 500.0000 = 2500.00, min_charge=1000.00 doesn't floor it.
        BaseFreightResult result = computeLtl("BF_TEST_LTL_NOFLOOR", 5);

        assertThat(result.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("2500.0000"));
        assertThat(result.rateBasisUsed()).isEqualTo(RateBasis.PER_PALLET);
        assertThat(result.minimumChargeApplied()).isFalse();
        assertThat(result.lineItemComment()).isNull();
    }

    @Test
    void ltlFlooringApplies() {
        // 3 pallets * 100.0000 = 300.00, min_charge=5000.00 floors it — confirms the doc's literal
        // "min(...)" wording is treated as a documentation error and max (floor) semantics apply
        // to LTL exactly as they do everywhere else, not a cap.
        BaseFreightResult result = computeLtl("BF_TEST_LTL_FLOOR", 3);

        assertThat(result.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.minimumChargeApplied()).isTrue();
        assertThat(result.lineItemComment()).isEqualTo("Minimum charge applied.");
    }

    @Test
    void perTonBasisUnaffectedByStep0Fix() {
        // gross=5000, volume=1 -> chargeable=max(5000, 333)=5000. raw = 200.0000 * 5000 / 1000 =
        // 1000.0000, min_charge=50.00 doesn't floor it. PER_TON was already correct pre-fix.
        BaseFreightResult result = compute("BF_TEST_PERTON", LoadType.BULK_TIPPER, new BigDecimal("500.00"),
                new BigDecimal("5000"), new BigDecimal("1"), null);

        assertThat(result.baseFreightAmount()).isEqualByComparingTo(new BigDecimal("1000.0000"));
        assertThat(result.rateBasisUsed()).isEqualTo(RateBasis.PER_TON);
        assertThat(result.minimumChargeApplied()).isFalse();
    }

    private BaseFreightResult compute(String laneKey, LoadType loadType, BigDecimal distanceKm, BigDecimal grossWeightKg, BigDecimal volumeCbm, Integer palletCount) {
        RateComputeRequest request = request(cargo(loadType, grossWeightKg, volumeCbm, palletCount));
        LaneResolutionResult lane = lane(laneKey, distanceKm);
        VehicleSelectionResult vehicle = vehicle();
        return baseFreightComputationService.compute(request, lane, vehicle);
    }

    private BaseFreightResult computeAsOf(String laneKey, LocalDate rateDate) {
        RateComputeRequest request = request(cargo(LoadType.FTL, new BigDecimal("1000"), new BigDecimal("1")), rateDate);
        LaneResolutionResult lane = lane(laneKey, new BigDecimal("100.00"));
        return baseFreightComputationService.compute(request, lane, vehicle());
    }

    private BaseFreightResult computeLtl(String laneKey, int palletCount) {
        return compute(laneKey, LoadType.LTL, new BigDecimal("500.00"), new BigDecimal("5000"), new BigDecimal("10"), palletCount);
    }

    private static CargoRequest cargo(LoadType loadType, BigDecimal grossWeightKg, BigDecimal volumeCbm) {
        return cargo(loadType, grossWeightKg, volumeCbm, null);
    }

    private static CargoRequest cargo(LoadType loadType, BigDecimal grossWeightKg, BigDecimal volumeCbm, Integer palletCount) {
        TemperatureRange temperatureRange = loadType == LoadType.REEFER
                ? new TemperatureRange(new BigDecimal("2"), new BigDecimal("8"))
                : null;
        return new CargoRequest(
                CargoClass.GENERAL,
                "0000.00",
                grossWeightKg,
                volumeCbm,
                loadType,
                palletCount,
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

    private static RateComputeRequest request(CargoRequest cargo) {
        return request(cargo, RATE_DATE);
    }

    private static RateComputeRequest request(CargoRequest cargo, LocalDate rateDate) {
        ServiceRequest service = new ServiceRequest(
                ServiceLevel.STANDARD, rateDate, null, false, false, false, false, false, false);
        return new RateComputeRequest(UUID.randomUUID(), rateDate, null, cargo, service);
    }

    private static LaneResolutionResult lane(String laneKey, BigDecimal distanceKm) {
        return new LaneResolutionResult(laneKey, distanceKm, UUID.randomUUID(), UUID.randomUUID(), false);
    }

    private static VehicleSelectionResult vehicle() {
        return vehicle(VEHICLE_CODE);
    }

    private static VehicleSelectionResult vehicle(String vehicleCategoryCode) {
        return new VehicleSelectionResult(UUID.randomUUID(), vehicleCategoryCode, SelectionReason.COST_EFFICIENT, false, 1);
    }
}
