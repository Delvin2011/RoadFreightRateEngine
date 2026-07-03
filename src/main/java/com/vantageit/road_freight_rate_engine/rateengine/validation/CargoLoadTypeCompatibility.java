package com.vantageit.road_freight_rate_engine.rateengine.validation;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoClass;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LoadType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.ValidationError;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cargo class / load type compatibility, per the Business Rules tab. A cargo class with no entry
 * here has no load_type restriction.
 *
 * <p>{@code project_cargo} is an independent Group D flag, not a cargo class — it does not bypass
 * this check. (An earlier version of this class incorrectly treated it as a bypass condition,
 * because the source table listed it in the same row structure as the actual cargo-class
 * restrictions; that was a documentation error, not a real rule.)
 *
 * <p>The table's "liquid bulk" row has no corresponding {@link CargoClass} value in Stage 1's
 * model (no is-liquid flag exists) — GENERAL is deliberately left unrestricted rather than
 * guessing at a narrower permitted set that this DTO shape can't actually enforce correctly.
 */
final class CargoLoadTypeCompatibility {

    private static final Map<CargoClass, Set<LoadType>> PERMITTED_LOAD_TYPES = new EnumMap<>(CargoClass.class);

    static {
        PERMITTED_LOAD_TYPES.put(CargoClass.HAZMAT, Set.of(LoadType.FTL, LoadType.TANKER, LoadType.FLATBED));
        PERMITTED_LOAD_TYPES.put(CargoClass.PERISHABLE, Set.of(LoadType.REEFER));
        PERMITTED_LOAD_TYPES.put(CargoClass.LIVE_ANIMALS, Set.of(LoadType.FTL));
        PERMITTED_LOAD_TYPES.put(CargoClass.OVERSIZED, Set.of(LoadType.FLATBED, LoadType.LOWBED));
    }

    List<ValidationError> check(CargoRequest cargo) {
        Set<LoadType> permitted = PERMITTED_LOAD_TYPES.get(cargo.cargoClass());
        if (permitted == null || permitted.contains(cargo.loadType())) {
            return List.of();
        }

        // Sorted for deterministic, testable message text — Set.of()'s iteration order is unspecified.
        String options = permitted.stream().map(LoadType::getWireValue).sorted().collect(Collectors.joining(", "));
        String message = "load_type %s is not permitted for cargo_class %s — use %s".formatted(
                cargo.loadType().getWireValue(), cargo.cargoClass().getWireValue(), options);
        return List.of(new ValidationError("cargo.load_type", "INCOMPATIBLE_CARGO_CLASS", message));
    }
}
