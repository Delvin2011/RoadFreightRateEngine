package com.vantageit.road_freight_rate_engine.rateengine.basefreight;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RateBasis;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

/**
 * More than one active {@code road_freight_rates} row matches the lane/vehicle/load type, and
 * it's not the one documented, resolvable case (a {@code flat} + {@code per_km} pair for an FTL
 * lane, where {@code flat} wins). Never silently picks one.
 */
@Getter
public class AmbiguousRateConfigurationException extends RuntimeException {

    /** @param id the conflicting {@code road_freight_rates} row's id */
    public record ConflictingRow(UUID id, RateBasis rateBasis) {
    }

    private final String laneKey;
    private final String vehicleCategoryCode;
    private final String loadType;
    private final List<ConflictingRow> conflictingRows;

    public AmbiguousRateConfigurationException(String laneKey, String vehicleCategoryCode, String loadType, List<ConflictingRow> conflictingRows) {
        super("Ambiguous rate configuration for lane %s, vehicle_category=%s, load_type=%s: %s"
                .formatted(laneKey, vehicleCategoryCode, loadType, conflictingRows));
        this.laneKey = laneKey;
        this.vehicleCategoryCode = vehicleCategoryCode;
        this.loadType = loadType;
        this.conflictingRows = conflictingRows;
    }
}
