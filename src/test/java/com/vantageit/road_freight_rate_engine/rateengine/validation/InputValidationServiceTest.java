package com.vantageit.road_freight_rate_engine.rateengine.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.AddressType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoClass;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ContainerType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.Dimensions;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LoadType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.PackageType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.PackingGroup;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceLevel;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ServiceRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.TemperatureRange;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ValidationError;
import com.vantageit.road_freight_rate_engine.rateengine.pipeline.common.ChargeableWeightCalculator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class InputValidationServiceTest {

    private static final UUID ORIGIN_LOCATION_ID = UUID.fromString("1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed");
    private static final UUID DESTINATION_LOCATION_ID = UUID.fromString("3c2f1a9e-7d4b-4e6a-9c1f-2b8a7d5e6f3c");
    private static final UUID BORDER_POST_ID = UUID.fromString("a1b2c3d4-e5f6-4789-a1b2-c3d4e5f6a1b2");

    private final InputValidationService service = new InputValidationService();

    @Test
    void fullyValidHazmatTankerCrossBorderRequestHasNoErrorsOrFlags() {
        ValidationResult result = service.validate(request(validRoute(), validCargo().build(), validService()));

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.flags()).isEmpty();
    }

    @Test
    void crossBorderWithoutBorderPostFailsWithSingleError() {
        RouteRequest route = routeWithoutBorderPost();

        ValidationResult result = service.validate(request(route, validCargo().build(), validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("REQUIRED_FOR_CROSS_BORDER");
        assertThat(result.errors().get(0).field()).isEqualTo("route.border_post_id");
    }

    @Test
    void distanceOverrideWithoutReasonFailsWithSingleError() {
        RouteRequest route = new RouteRequestBuilder()
                .distanceKm(new BigDecimal("450.00"))
                .distanceOverrideReason(null)
                .build();

        ValidationResult result = service.validate(request(route, validCargo().build(), validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("REQUIRED_FOR_DISTANCE_OVERRIDE");
        assertThat(result.errors().get(0).field()).isEqualTo("route.distance_override_reason");
    }

    @Test
    void distanceOverrideWithReasonStaysValid() {
        RouteRequest route = new RouteRequestBuilder()
                .distanceKm(new BigDecimal("450.00"))
                .distanceOverrideReason("Customer-confirmed shorter route via toll road")
                .build();

        ValidationResult result = service.validate(request(route, validCargo().build(), validService()));

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void hazmatWithLtlLoadTypeFailsWithSingleIncompatibilityError() {
        CargoRequest cargo = validCargo()
                .loadType(LoadType.LTL)
                .palletCount(10)
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("INCOMPATIBLE_CARGO_CLASS");
        assertThat(result.errors().get(0).field()).isEqualTo("cargo.load_type");
    }

    @Test
    void ltlWithoutPalletCountFailsWithSingleError() {
        CargoRequest cargo = generalCargo()
                .loadType(LoadType.LTL)
                .palletCount(null)
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("PALLET_COUNT_REQUIRED_FOR_LTL");
        assertThat(result.errors().get(0).field()).isEqualTo("cargo.pallet_count");
    }

    @Test
    void reeferWithoutTemperatureRangeFailsWithSingleError() {
        CargoRequest cargo = generalCargo()
                .loadType(LoadType.REEFER)
                .temperatureRangeC(null)
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("TEMPERATURE_RANGE_REQUIRED");
        assertThat(result.errors().get(0).field()).isEqualTo("cargo.temperature_range_c");
    }

    @Test
    void hazmatWithoutUnNumberFailsWithSingleError() {
        CargoRequest cargo = validCargo()
                .hazmatUnNumber(null)
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("REQUIRED_FOR_HAZMAT");
        assertThat(result.errors().get(0).field()).isEqualTo("cargo.hazmat_un_number");
    }

    @Test
    void highValueDeclaredWithoutDeclaredValueFailsWithSingleError() {
        CargoRequest cargo = generalCargo()
                .highValueDeclared(true)
                .declaredValueZar(null)
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("REQUIRED_FOR_HIGH_VALUE");
        assertThat(result.errors().get(0).field()).isEqualTo("cargo.declared_value_zar");
    }

    @Test
    void threeSimultaneousFailuresAllAccumulateInOneResult() {
        RouteRequest route = routeWithoutBorderPost();
        CargoRequest cargo = validCargo()
                .loadType(LoadType.LTL)
                .palletCount(5)
                .hazmatUnNumber(null)
                .build();

        ValidationResult result = service.validate(request(route, cargo, validService()));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(ValidationError::code).containsExactlyInAnyOrder(
                "REQUIRED_FOR_CROSS_BORDER",
                "INCOMPATIBLE_CARGO_CLASS",
                "REQUIRED_FOR_HAZMAT");
    }

    @ParameterizedTest
    @MethodSource("chargeableWeightCases")
    void chargeableWeightComputesMaxOfGrossAndVolumetric(BigDecimal grossWeightKg, BigDecimal volumeCbm, BigDecimal expectedKg) {
        BigDecimal actual = ChargeableWeightCalculator.compute(grossWeightKg, volumeCbm);

        assertThat(actual).isEqualByComparingTo(expectedKg);
    }

    static Stream<Arguments> chargeableWeightCases() {
        return Stream.of(
                // gross dominates
                Arguments.of(new BigDecimal("18500"), new BigDecimal("22.5"), new BigDecimal("18500")),
                // volumetric dominates: 30 * 333 = 9990
                Arguments.of(new BigDecimal("5000"), new BigDecimal("30"), new BigDecimal("9990")),
                // exactly equal
                Arguments.of(new BigDecimal("999"), new BigDecimal("3"), new BigDecimal("999")));
    }

    @Test
    void widthExceedingLimitProducesFlagButStaysValid() {
        CargoRequest cargo = generalCargo()
                .dimensionsLxwxhM(new Dimensions(new BigDecimal("10"), new BigDecimal("2.6"), new BigDecimal("2.5")))
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.isValid()).isTrue();
        assertThat(result.flags()).contains("ABNORMAL_WIDTH");
    }

    @Test
    void chargeableWeightAboveGcmCeilingFailsWithOverweightError() {
        CargoRequest cargo = generalCargo()
                .grossWeightKg(new BigDecimal("60000"))
                .volumeCbm(new BigDecimal("10"))
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("OVERWEIGHT");
    }

    @ParameterizedTest(name = "{0} + {1} is incompatible")
    @MethodSource("incompatibleCargoLoadTypeCombinations")
    void remainingIncompatibleCargoClassLoadTypeCombinationsFail(CargoClass cargoClass, LoadType loadType) {
        CargoRequest cargo = cargoFor(cargoClass, loadType).build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("INCOMPATIBLE_CARGO_CLASS");
        assertThat(result.errors().get(0).field()).isEqualTo("cargo.load_type");
    }

    static Stream<Arguments> incompatibleCargoLoadTypeCombinations() {
        return Stream.of(
                Arguments.of(CargoClass.PERISHABLE, LoadType.LTL),
                Arguments.of(CargoClass.LIVE_ANIMALS, LoadType.FLATBED),
                Arguments.of(CargoClass.LIVE_ANIMALS, LoadType.TANKER),
                Arguments.of(CargoClass.OVERSIZED, LoadType.FTL));
    }

    @Test
    void projectCargoOnAGenuinelyCompatiblePairingStaysValid() {
        CargoRequest cargo = cargoFor(CargoClass.GENERAL, LoadType.FTL)
                .projectCargo(true)
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void projectCargoDoesNotBypassTheCompatibilityMatrixForAnIncompatiblePairing() {
        CargoRequest cargo = cargoFor(CargoClass.HAZMAT, LoadType.LTL)
                .projectCargo(true)
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("INCOMPATIBLE_CARGO_CLASS");
    }

    @Test
    void incompatibleCargoClassErrorMessageListsPermittedLoadTypesSortedAndReadable() {
        CargoRequest cargo = validCargo()
                .loadType(LoadType.LTL)
                .palletCount(10)
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).message())
                .isEqualTo("load_type ltl is not permitted for cargo_class hazmat — use flatbed, ftl, tanker");
    }

    @Test
    void oversizedWithoutDimensionsFailsWithSingleError() {
        CargoRequest cargo = generalCargo()
                .cargoClass(CargoClass.OVERSIZED)
                .loadType(LoadType.FLATBED)
                .dimensionsLxwxhM(null)
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("REQUIRED_FOR_OVERSIZED");
        assertThat(result.errors().get(0).field()).isEqualTo("cargo.dimensions_lxwxh_m");
    }

    @Test
    void temperatureRangeWithMinGreaterThanMaxFailsWithSingleError() {
        CargoRequest cargo = generalCargo()
                .loadType(LoadType.REEFER)
                .temperatureRangeC(new TemperatureRange(new BigDecimal("10"), new BigDecimal("2")))
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("TEMPERATURE_RANGE_REQUIRED");
        assertThat(result.errors().get(0).field()).isEqualTo("cargo.temperature_range_c");
    }

    @Test
    void hazmatWithoutPackingGroupFailsWithSingleError() {
        CargoRequest cargo = validCargo()
                .hazmatPackingGroup(null)
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).code()).isEqualTo("REQUIRED_FOR_HAZMAT");
        assertThat(result.errors().get(0).field()).isEqualTo("cargo.hazmat_packing_group");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("dimensionBoundaryCases")
    void dimensionBoundariesFlagOnlyWhenStrictlyExceeded(
            String description, BigDecimal length, BigDecimal width, BigDecimal height, String flagCode, boolean expectFlag) {
        CargoRequest cargo = generalCargo()
                .dimensionsLxwxhM(new Dimensions(length, width, height))
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).isEmpty();
        if (expectFlag) {
            assertThat(result.flags()).contains(flagCode);
        } else {
            assertThat(result.flags()).doesNotContain(flagCode);
        }
    }

    static Stream<Arguments> dimensionBoundaryCases() {
        BigDecimal safeLength = new BigDecimal("10");
        BigDecimal safeWidth = new BigDecimal("2");
        BigDecimal safeHeight = new BigDecimal("2.5");
        return Stream.of(
                Arguments.of("width exactly at 2.4m does not flag", safeLength, new BigDecimal("2.4"), safeHeight, "ABNORMAL_WIDTH", false),
                Arguments.of("width just over 2.4m flags", safeLength, new BigDecimal("2.41"), safeHeight, "ABNORMAL_WIDTH", true),
                Arguments.of("height exactly at 4.3m does not flag", safeLength, safeWidth, new BigDecimal("4.3"), "ABNORMAL_HEIGHT", false),
                Arguments.of("height just over 4.3m flags", safeLength, safeWidth, new BigDecimal("4.31"), "ABNORMAL_HEIGHT", true),
                Arguments.of("length exactly at 22m does not flag", new BigDecimal("22"), safeWidth, safeHeight, "ABNORMAL_LENGTH", false),
                Arguments.of("length just over 22m flags", new BigDecimal("22.01"), safeWidth, safeHeight, "ABNORMAL_LENGTH", true));
    }

    @Test
    void chargeableWeightExactlyAtGcmCeilingDoesNotError() {
        CargoRequest cargo = generalCargo()
                .grossWeightKg(new BigDecimal("56000"))
                .volumeCbm(new BigDecimal("1"))
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).extracting(ValidationError::code).doesNotContain("OVERWEIGHT");
    }

    @Test
    void chargeableWeightJustOverGcmCeilingFailsWithOverweightError() {
        CargoRequest cargo = generalCargo()
                .grossWeightKg(new BigDecimal("56000.01"))
                .volumeCbm(new BigDecimal("1"))
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.errors()).extracting(ValidationError::code).contains("OVERWEIGHT");
    }

    @Test
    void multipleAbnormalDimensionsAllFlagWithoutAnyHardErrors() {
        CargoRequest cargo = generalCargo()
                .dimensionsLxwxhM(new Dimensions(new BigDecimal("25"), new BigDecimal("2.6"), new BigDecimal("4.5")))
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
        assertThat(result.flags()).containsExactlyInAnyOrder("ABNORMAL_WIDTH", "ABNORMAL_HEIGHT", "ABNORMAL_LENGTH");
    }

    @Test
    void flagsAndErrorsRemainSeparateListsWhenBothPresent() {
        CargoRequest cargo = validCargo()
                .hazmatUnNumber(null)
                .dimensionsLxwxhM(new Dimensions(new BigDecimal("10"), new BigDecimal("2.6"), new BigDecimal("2.5")))
                .build();

        ValidationResult result = service.validate(request(validRoute(), cargo, validService()));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).extracting(ValidationError::code).containsExactly("REQUIRED_FOR_HAZMAT");
        assertThat(result.flags()).containsExactly("ABNORMAL_WIDTH");
    }

    private static RateComputeRequest request(RouteRequest route, CargoRequest cargo, ServiceRequest serviceRequest) {
        return new RateComputeRequest(
                UUID.fromString("8f14e45f-ceea-4a44-b1a0-5c1f8e9b2a3d"),
                LocalDate.of(2025, 7, 15),
                route,
                cargo,
                serviceRequest);
    }

    private static RouteRequest validRoute() {
        return new RouteRequestBuilder().build();
    }

    private static RouteRequest routeWithoutBorderPost() {
        return new RouteRequestBuilder().borderPostId(null).build();
    }

    private static ServiceRequest validService() {
        return new ServiceRequest(
                ServiceLevel.STANDARD,
                LocalDate.of(2025, 7, 15),
                null,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false);
    }

    /** Baseline: the valid hazmat tanker cross-border fixture from Stage 1's serialization test. */
    private static CargoRequestBuilder validCargo() {
        return new CargoRequestBuilder()
                .cargoClass(CargoClass.HAZMAT)
                .commodityCode("2710.12.90")
                .grossWeightKg(new BigDecimal("18500"))
                .volumeCbm(new BigDecimal("22.5"))
                .loadType(LoadType.TANKER)
                .packageType(PackageType.BULK)
                .hazmatUnNumber("UN1203")
                .hazmatPackingGroup(PackingGroup.II)
                .imdgClass("3");
    }

    /** Baseline for scenarios that don't need hazmat-specific fields. */
    private static CargoRequestBuilder generalCargo() {
        return new CargoRequestBuilder()
                .cargoClass(CargoClass.GENERAL)
                .commodityCode("8481.80")
                .grossWeightKg(new BigDecimal("1000"))
                .volumeCbm(new BigDecimal("3"))
                .loadType(LoadType.FTL)
                .packageType(PackageType.PALLETISED);
    }

    /**
     * A cargo/load_type pair with just enough supporting fields set (pallet count for LTL,
     * temperature range for reefer/perishable, hazmat fields for hazmat, dimensions for
     * oversized) that only the compatibility check under test can fail — isolates
     * INCOMPATIBLE_CARGO_CLASS from every other check.
     */
    private static CargoRequestBuilder cargoFor(CargoClass cargoClass, LoadType loadType) {
        CargoRequestBuilder builder = new CargoRequestBuilder()
                .cargoClass(cargoClass)
                .loadType(loadType);
        if (loadType == LoadType.LTL) {
            builder.palletCount(10);
        }
        if (loadType == LoadType.REEFER || cargoClass == CargoClass.PERISHABLE) {
            builder.temperatureRangeC(new TemperatureRange(new BigDecimal("2"), new BigDecimal("8")));
        }
        if (cargoClass == CargoClass.HAZMAT) {
            builder.hazmatUnNumber("UN1203").hazmatPackingGroup(PackingGroup.II);
        }
        if (cargoClass == CargoClass.OVERSIZED) {
            builder.dimensionsLxwxhM(new Dimensions(new BigDecimal("10"), new BigDecimal("2"), new BigDecimal("2")));
        }
        return builder;
    }

    /** Mutable test-only builder mirroring {@link RouteRequest}'s components, with a valid cross-border baseline. */
    private static final class RouteRequestBuilder {
        private UUID originLocationId = ORIGIN_LOCATION_ID;
        private UUID destinationLocationId = DESTINATION_LOCATION_ID;
        private RouteType routeType = RouteType.CROSS_BORDER;
        private UUID borderPostId = BORDER_POST_ID;
        private BigDecimal distanceKm = null;
        private String distanceOverrideReason = null;
        private AddressType collectionAddressType = AddressType.DEPOT;
        private AddressType deliveryAddressType = AddressType.DOOR_TO_DOOR;

        RouteRequestBuilder borderPostId(UUID value) {
            this.borderPostId = value;
            return this;
        }

        RouteRequestBuilder distanceKm(BigDecimal value) {
            this.distanceKm = value;
            return this;
        }

        RouteRequestBuilder distanceOverrideReason(String value) {
            this.distanceOverrideReason = value;
            return this;
        }

        RouteRequest build() {
            return new RouteRequest(originLocationId, destinationLocationId, routeType, borderPostId, distanceKm,
                    distanceOverrideReason, collectionAddressType, deliveryAddressType);
        }
    }

    /** Mutable test-only builder mirroring {@link CargoRequest}'s components, with safe FTL/GENERAL defaults. */
    private static final class CargoRequestBuilder {
        private CargoClass cargoClass = CargoClass.GENERAL;
        private String commodityCode = "0000.00";
        private BigDecimal grossWeightKg = new BigDecimal("1000");
        private BigDecimal volumeCbm = new BigDecimal("3");
        private LoadType loadType = LoadType.FTL;
        private Integer palletCount = null;
        private ContainerType containerType = null;
        private PackageType packageType = PackageType.PALLETISED;
        private Boolean stackable = false;
        private Dimensions dimensionsLxwxhM = null;
        private TemperatureRange temperatureRangeC = null;
        private String hazmatUnNumber = null;
        private PackingGroup hazmatPackingGroup = null;
        private String imdgClass = null;
        private Boolean highValueDeclared = false;
        private BigDecimal declaredValueZar = null;
        private Boolean securityEscortRequired = false;
        private Boolean abnormalLoad = false;
        private Boolean liveAnimals = false;
        private Boolean projectCargo = false;

        CargoRequestBuilder cargoClass(CargoClass value) {
            this.cargoClass = value;
            return this;
        }

        CargoRequestBuilder commodityCode(String value) {
            this.commodityCode = value;
            return this;
        }

        CargoRequestBuilder grossWeightKg(BigDecimal value) {
            this.grossWeightKg = value;
            return this;
        }

        CargoRequestBuilder volumeCbm(BigDecimal value) {
            this.volumeCbm = value;
            return this;
        }

        CargoRequestBuilder loadType(LoadType value) {
            this.loadType = value;
            return this;
        }

        CargoRequestBuilder palletCount(Integer value) {
            this.palletCount = value;
            return this;
        }

        CargoRequestBuilder packageType(PackageType value) {
            this.packageType = value;
            return this;
        }

        CargoRequestBuilder dimensionsLxwxhM(Dimensions value) {
            this.dimensionsLxwxhM = value;
            return this;
        }

        CargoRequestBuilder temperatureRangeC(TemperatureRange value) {
            this.temperatureRangeC = value;
            return this;
        }

        CargoRequestBuilder hazmatUnNumber(String value) {
            this.hazmatUnNumber = value;
            return this;
        }

        CargoRequestBuilder hazmatPackingGroup(PackingGroup value) {
            this.hazmatPackingGroup = value;
            return this;
        }

        CargoRequestBuilder imdgClass(String value) {
            this.imdgClass = value;
            return this;
        }

        CargoRequestBuilder highValueDeclared(Boolean value) {
            this.highValueDeclared = value;
            return this;
        }

        CargoRequestBuilder declaredValueZar(BigDecimal value) {
            this.declaredValueZar = value;
            return this;
        }

        CargoRequestBuilder projectCargo(Boolean value) {
            this.projectCargo = value;
            return this;
        }

        CargoRequest build() {
            return new CargoRequest(cargoClass, commodityCode, grossWeightKg, volumeCbm, loadType, palletCount,
                    containerType, packageType, stackable, dimensionsLxwxhM, temperatureRangeC, hazmatUnNumber,
                    hazmatPackingGroup, imdgClass, highValueDeclared, declaredValueZar, securityEscortRequired,
                    abnormalLoad, liveAnimals, projectCargo);
        }
    }
}
