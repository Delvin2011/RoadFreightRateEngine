package com.vantageit.road_freight_rate_engine.rateengine.orchestration;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ValidationError;
import com.vantageit.road_freight_rate_engine.rateengine.basefreight.AmbiguousRateConfigurationException;
import com.vantageit.road_freight_rate_engine.rateengine.basefreight.RateNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.clearance.ClearingFeeRequiredException;
import com.vantageit.road_freight_rate_engine.rateengine.currencyconversion.ExchangeRateNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.DistanceNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.UnknownLocationException;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.UnmappedZoneException;
import com.vantageit.road_freight_rate_engine.rateengine.servicelevel.AccessorialCurrencyMismatchException;
import com.vantageit.road_freight_rate_engine.rateengine.servicelevel.AccessorialRateNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.surcharge.SurchargeRateNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.vehicleselection.NoEligibleVehicleException;
import com.vantageit.road_freight_rate_engine.rateengine.vehicleselection.NoRateAvailableForLaneException;
import com.vantageit.road_freight_rate_engine.rateengine.vehicleselection.VehicleCapacityExceededException;
import org.springframework.stereotype.Component;

/**
 * Maps every stage's exception type to a {@link ValidationError} — reusing the API contract's
 * existing {@code field}/{@code code}/{@code message} error shape, even though none of these are
 * per-field request-validation errors. {@code field} is fixed to {@code "_pipeline"} for all of
 * them: a sensible placeholder for "this failure isn't about one specific request field" (a
 * downstream data gap or business-rule violation instead), chosen over inventing a different shape
 * just for this case.
 *
 * <p>Deliberately a plain component, not a {@code @ControllerAdvice}: no REST controller exists
 * yet for the rate-compute endpoint (only the DTOs and services have been built so far), so there
 * is nothing for a {@code @ControllerAdvice} to advise. This translator is used directly by {@link
 * PipelineOrchestrationService}; a future controller can reuse it the same way once built.
 */
@Component
public class PipelineExceptionTranslator {

    public ValidationError translate(RuntimeException ex) {
        return new ValidationError("_pipeline", codeFor(ex), ex.getMessage());
    }

    // instanceof chain rather than a pattern-matching switch: this project's compiler release is
    // 17 (see pom.xml), where switch patterns are still a preview feature, not the Java 21 the
    // prompt described.
    private static String codeFor(RuntimeException ex) {
        if (ex instanceof UnknownLocationException) {
            return "UNKNOWN_LOCATION";
        }
        if (ex instanceof UnmappedZoneException) {
            return "UNMAPPED_ZONE";
        }
        if (ex instanceof DistanceNotFoundException) {
            return "DISTANCE_NOT_FOUND";
        }
        if (ex instanceof NoEligibleVehicleException) {
            return "NO_ELIGIBLE_VEHICLE";
        }
        if (ex instanceof NoRateAvailableForLaneException) {
            return "NO_RATE_AVAILABLE_FOR_LANE";
        }
        if (ex instanceof VehicleCapacityExceededException) {
            return "VEHICLE_CAPACITY_EXCEEDED";
        }
        if (ex instanceof RateNotFoundException) {
            return "RATE_NOT_FOUND";
        }
        if (ex instanceof AmbiguousRateConfigurationException) {
            return "AMBIGUOUS_RATE_CONFIGURATION";
        }
        if (ex instanceof SurchargeRateNotFoundException) {
            return "SURCHARGE_RATE_NOT_FOUND";
        }
        if (ex instanceof ClearingFeeRequiredException) {
            return "CLEARING_FEE_REQUIRED";
        }
        if (ex instanceof AccessorialRateNotFoundException) {
            return "ACCESSORIAL_RATE_NOT_FOUND";
        }
        if (ex instanceof AccessorialCurrencyMismatchException) {
            return "ACCESSORIAL_CURRENCY_MISMATCH";
        }
        if (ex instanceof ExchangeRateNotFoundException) {
            return "EXCHANGE_RATE_NOT_FOUND";
        }
        return "PIPELINE_ERROR";
    }
}
