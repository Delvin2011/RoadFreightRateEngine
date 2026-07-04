package com.vantageit.road_freight_rate_engine.rateengine.orchestration;

import static org.assertj.core.api.Assertions.assertThat;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ValidationError;
import com.vantageit.road_freight_rate_engine.rateengine.basefreight.AmbiguousRateConfigurationException;
import com.vantageit.road_freight_rate_engine.rateengine.basefreight.RateNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.clearance.ClearingFeeRequiredException;
import com.vantageit.road_freight_rate_engine.rateengine.currencyconversion.ExchangeRateNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.DistanceNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LocationRole;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.UnknownLocationException;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.UnmappedZoneException;
import com.vantageit.road_freight_rate_engine.rateengine.servicelevel.AccessorialCurrencyMismatchException;
import com.vantageit.road_freight_rate_engine.rateengine.servicelevel.AccessorialRateNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.surcharge.SurchargeRateNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.vehicleselection.NoEligibleVehicleException;
import com.vantageit.road_freight_rate_engine.rateengine.vehicleselection.NoRateAvailableForLaneException;
import com.vantageit.road_freight_rate_engine.rateengine.vehicleselection.VehicleCapacityExceededException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pure unit test, no Spring context — {@link PipelineExceptionTranslator} has no dependencies.
 * Each case constructs the exception directly (some, like {@code UnmappedZoneException}, can't
 * realistically occur through a real end-to-end pipeline run, matching Stage 4's own precedent of
 * testing that exception via direct construction rather than an integration scenario) and asserts
 * the exact mapped code, proving the translator isn't falling back to the generic {@code
 * PIPELINE_ERROR} code for any of them.
 */
class PipelineExceptionTranslatorTest {

    private final PipelineExceptionTranslator translator = new PipelineExceptionTranslator();

    @Test
    void unknownLocationExceptionMapsToUnknownLocation() {
        assertCode(new UnknownLocationException(UUID.randomUUID(), LocationRole.ORIGIN), "UNKNOWN_LOCATION");
    }

    @Test
    void unmappedZoneExceptionMapsToUnmappedZone() {
        assertCode(new UnmappedZoneException(UUID.randomUUID(), UUID.randomUUID(), LocationRole.DESTINATION), "UNMAPPED_ZONE");
    }

    @Test
    void distanceNotFoundExceptionMapsToDistanceNotFound() {
        assertCode(new DistanceNotFoundException("JHB_METRO:BFN_METRO", null), "DISTANCE_NOT_FOUND");
    }

    @Test
    void noEligibleVehicleExceptionMapsToNoEligibleVehicle() {
        assertCode(new NoEligibleVehicleException(new BigDecimal("1000"), new BigDecimal("1"), "ftl", "JHB_METRO:BFN_METRO"), "NO_ELIGIBLE_VEHICLE");
    }

    @Test
    void noRateAvailableForLaneExceptionMapsToNoRateAvailableForLane() {
        assertCode(new NoRateAvailableForLaneException("JHB_METRO:BFN_METRO", "ftl", List.of("8T_RIGID")), "NO_RATE_AVAILABLE_FOR_LANE");
    }

    @Test
    void vehicleCapacityExceededExceptionMapsToVehicleCapacityExceeded() {
        assertCode(new VehicleCapacityExceededException("8T_RIGID", new BigDecimal("8000"), new BigDecimal("9000")), "VEHICLE_CAPACITY_EXCEEDED");
    }

    @Test
    void rateNotFoundExceptionMapsToRateNotFound() {
        assertCode(new RateNotFoundException("JHB_METRO:BFN_METRO", "8T_RIGID", "ftl"), "RATE_NOT_FOUND");
    }

    @Test
    void ambiguousRateConfigurationExceptionMapsToAmbiguousRateConfiguration() {
        assertCode(new AmbiguousRateConfigurationException("JHB_METRO:BFN_METRO", "8T_RIGID", "ftl", List.of()), "AMBIGUOUS_RATE_CONFIGURATION");
    }

    @Test
    void surchargeRateNotFoundExceptionMapsToSurchargeRateNotFound() {
        assertCode(new SurchargeRateNotFoundException("FUEL_LEVY", LocalDate.of(2025, 7, 15)), "SURCHARGE_RATE_NOT_FOUND");
    }

    @Test
    void clearingFeeRequiredExceptionMapsToClearingFeeRequired() {
        assertCode(new ClearingFeeRequiredException(LocalDate.of(2025, 7, 15)), "CLEARING_FEE_REQUIRED");
    }

    @Test
    void accessorialRateNotFoundExceptionMapsToAccessorialRateNotFound() {
        assertCode(new AccessorialRateNotFoundException("AFTER_HOURS_COLLECTION", LocalDate.of(2025, 7, 15)), "ACCESSORIAL_RATE_NOT_FOUND");
    }

    @Test
    void accessorialCurrencyMismatchExceptionMapsToAccessorialCurrencyMismatch() {
        assertCode(new AccessorialCurrencyMismatchException("AFTER_HOURS_COLLECTION", "EUR", "ZAR"), "ACCESSORIAL_CURRENCY_MISMATCH");
    }

    @Test
    void exchangeRateNotFoundExceptionMapsToExchangeRateNotFound() {
        assertCode(new ExchangeRateNotFoundException("USD", "ZAR", LocalDate.of(2025, 7, 15)), "EXCHANGE_RATE_NOT_FOUND");
    }

    @Test
    void unrecognizedExceptionFallsBackToPipelineError() {
        assertCode(new IllegalStateException("something genuinely unexpected"), "PIPELINE_ERROR");
    }

    @Test
    void everyMappedErrorUsesThePipelineFieldPlaceholder() {
        ValidationError error = translator.translate(new RateNotFoundException("JHB_METRO:BFN_METRO", "8T_RIGID", "ftl"));

        assertThat(error.field()).isEqualTo("_pipeline");
    }

    private void assertCode(RuntimeException ex, String expectedCode) {
        ValidationError error = translator.translate(ex);

        assertThat(error.code()).isEqualTo(expectedCode);
        assertThat(error.message()).isEqualTo(ex.getMessage());
    }
}
