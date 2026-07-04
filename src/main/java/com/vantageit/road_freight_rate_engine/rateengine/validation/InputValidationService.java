package com.vantageit.road_freight_rate_engine.rateengine.validation;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LoadType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoClass;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.TemperatureRange;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ValidationError;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Stage 1 of the rate engine computation pipeline: input validation & normalisation. Pure
 * business-rule validation over the request DTOs — no persistence, no pricing.
 */
@Service
public class InputValidationService {

    private final CargoLoadTypeCompatibility cargoLoadTypeCompatibility = new CargoLoadTypeCompatibility();
    private final LegalLimitsChecker legalLimitsChecker = new LegalLimitsChecker();

    public ValidationResult validate(RateComputeRequest request) {
        List<ValidationError> errors = new ArrayList<>();

        errors.addAll(checkCrossBorderBorderPost(request.route()));
        errors.addAll(checkDistanceOverrideReason(request.route()));
        errors.addAll(cargoLoadTypeCompatibility.check(request.cargo()));
        errors.addAll(checkLtlPalletCount(request.cargo()));
        errors.addAll(checkTemperatureRange(request.cargo()));
        errors.addAll(checkHazmatFields(request.cargo()));
        errors.addAll(checkHighValueDeclaredValue(request.cargo()));
        errors.addAll(checkOversizedDimensions(request.cargo()));
        errors.addAll(legalLimitsChecker.checkOverweight(request.cargo()));

        List<String> flags = legalLimitsChecker.checkFlags(request.cargo());

        return errors.isEmpty() ? ValidationResult.valid(flags) : ValidationResult.invalid(errors, flags);
    }

    List<ValidationError> checkCrossBorderBorderPost(RouteRequest route) {
        if (route.routeType() == RouteType.CROSS_BORDER && route.borderPostId() == null) {
            return List.of(new ValidationError(
                    "route.border_post_id",
                    "REQUIRED_FOR_CROSS_BORDER",
                    "border_post_id is required when route_type is cross_border"));
        }
        return List.of();
    }

    /**
     * The Business Rules tab uses "required" for this field with the same language as
     * {@code REQUIRED_FOR_CROSS_BORDER}/{@code REQUIRED_FOR_HAZMAT}, both hard Stage 3 errors —
     * treated the same way here, not as an optional audit field.
     */
    List<ValidationError> checkDistanceOverrideReason(RouteRequest route) {
        if (route.distanceKm() != null && route.distanceOverrideReason() == null) {
            return List.of(new ValidationError(
                    "route.distance_override_reason",
                    "REQUIRED_FOR_DISTANCE_OVERRIDE",
                    "distance_override_reason is required when route.distance_km is provided"));
        }
        return List.of();
    }

    List<ValidationError> checkLtlPalletCount(CargoRequest cargo) {
        if (cargo.loadType() == LoadType.LTL && (cargo.palletCount() == null || cargo.palletCount() <= 0)) {
            return List.of(new ValidationError(
                    "cargo.pallet_count",
                    "PALLET_COUNT_REQUIRED_FOR_LTL",
                    "pallet_count is required and must be greater than 0 when load_type is ltl"));
        }
        return List.of();
    }

    List<ValidationError> checkTemperatureRange(CargoRequest cargo) {
        boolean requiresRange = cargo.loadType() == LoadType.REEFER || cargo.cargoClass() == CargoClass.PERISHABLE;
        if (!requiresRange) {
            return List.of();
        }

        TemperatureRange range = cargo.temperatureRangeC();
        boolean invalid = range == null
                || range.min() == null
                || range.max() == null
                || range.min().compareTo(range.max()) > 0;
        if (invalid) {
            return List.of(new ValidationError(
                    "cargo.temperature_range_c",
                    "TEMPERATURE_RANGE_REQUIRED",
                    "temperature_range_c is required (min <= max) when load_type is reefer or cargo_class is perishable"));
        }
        return List.of();
    }

    List<ValidationError> checkHazmatFields(CargoRequest cargo) {
        if (cargo.cargoClass() != CargoClass.HAZMAT) {
            return List.of();
        }

        List<ValidationError> errors = new ArrayList<>();
        if (cargo.hazmatUnNumber() == null) {
            errors.add(new ValidationError(
                    "cargo.hazmat_un_number",
                    "REQUIRED_FOR_HAZMAT",
                    "hazmat_un_number is required when cargo_class is hazmat"));
        }
        if (cargo.hazmatPackingGroup() == null) {
            errors.add(new ValidationError(
                    "cargo.hazmat_packing_group",
                    "REQUIRED_FOR_HAZMAT",
                    "hazmat_packing_group is required when cargo_class is hazmat"));
        }
        return errors;
    }

    List<ValidationError> checkHighValueDeclaredValue(CargoRequest cargo) {
        if (!Boolean.TRUE.equals(cargo.highValueDeclared())) {
            return List.of();
        }

        BigDecimal declaredValue = cargo.declaredValueZar();
        if (declaredValue == null || declaredValue.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of(new ValidationError(
                    "cargo.declared_value_zar",
                    "REQUIRED_FOR_HIGH_VALUE",
                    "declared_value_zar is required and must be greater than 0 when high_value_declared is true"));
        }
        return List.of();
    }

    List<ValidationError> checkOversizedDimensions(CargoRequest cargo) {
        if (cargo.cargoClass() == CargoClass.OVERSIZED && cargo.dimensionsLxwxhM() == null) {
            return List.of(new ValidationError(
                    "cargo.dimensions_lxwxh_m",
                    "REQUIRED_FOR_OVERSIZED",
                    "dimensions_lxwxh_m is required when cargo_class is oversized"));
        }
        return List.of();
    }
}
