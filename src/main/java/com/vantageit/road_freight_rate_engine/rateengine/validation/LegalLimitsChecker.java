package com.vantageit.road_freight_rate_engine.rateengine.validation;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.Dimensions;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ValidationError;
import com.vantageit.road_freight_rate_engine.rateengine.pipeline.common.ChargeableWeightCalculator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Legal dimension/weight limits, per the Business Rules tab. Dimension breaches (width/height/
 * length) are soft flags — the doc says abnormal loads still get priced, with permit surcharges
 * added later in the pipeline. Overweight is the one exception: it's a hard validation error.
 */
final class LegalLimitsChecker {

    static final BigDecimal MAX_WIDTH_M = new BigDecimal("2.4");
    static final BigDecimal MAX_HEIGHT_M = new BigDecimal("4.3");
    static final BigDecimal MAX_LENGTH_M = new BigDecimal("22");

    /**
     * Coarse sanity ceiling only, not the authoritative overweight check. Stage 3 runs before
     * vehicle selection (Stage 5), so it can't know whether an interlink combination (56,000kg
     * GCM) is eligible for this route vs. a standard combination (34,000kg GCM) — using the
     * standard 34,000kg limit here would falsely reject legitimate interlink-scale shipments
     * before vehicle selection ever runs. Stage 5 must apply its own tighter, vehicle-aware
     * overweight check once the actual vehicle category is known; that check, not this one, is
     * authoritative.
     */
    static final BigDecimal MAX_GCM_KG = new BigDecimal("56000");

    List<String> checkFlags(CargoRequest cargo) {
        Dimensions dimensions = cargo.dimensionsLxwxhM();
        if (dimensions == null) {
            return List.of();
        }

        List<String> flags = new ArrayList<>();
        if (dimensions.width() != null && dimensions.width().compareTo(MAX_WIDTH_M) > 0) {
            flags.add("ABNORMAL_WIDTH");
        }
        if (dimensions.height() != null && dimensions.height().compareTo(MAX_HEIGHT_M) > 0) {
            flags.add("ABNORMAL_HEIGHT");
        }
        if (dimensions.length() != null && dimensions.length().compareTo(MAX_LENGTH_M) > 0) {
            flags.add("ABNORMAL_LENGTH");
        }
        return flags;
    }

    List<ValidationError> checkOverweight(CargoRequest cargo) {
        BigDecimal chargeableWeightKg = ChargeableWeightCalculator.compute(cargo.grossWeightKg(), cargo.volumeCbm());
        if (chargeableWeightKg.compareTo(MAX_GCM_KG) <= 0) {
            return List.of();
        }

        String message = "chargeable_weight_kg %s exceeds the maximum GCM of %s kg — suggest split load"
                .formatted(chargeableWeightKg.toPlainString(), MAX_GCM_KG.toPlainString());
        return List.of(new ValidationError("cargo.gross_weight_kg", "OVERWEIGHT", message));
    }
}
