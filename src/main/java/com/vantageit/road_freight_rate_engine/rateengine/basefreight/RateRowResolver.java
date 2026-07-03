package com.vantageit.road_freight_rate_engine.rateengine.basefreight;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LoadType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RateBasis;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RoadFreightRate;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.RoadFreightRateRepository;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionResult;
import com.vantageit.road_freight_rate_engine.rateengine.vehicleselection.VehicleSelectionResult;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves which single {@code RoadFreightRate} row applies for a lane/vehicle/load type, when
 * more than one active row can legitimately exist (e.g. both a {@code flat} and a {@code per_km}
 * row for the same FTL lane).
 */
@Component
public class RateRowResolver {

    private final RoadFreightRateRepository roadFreightRateRepository;

    public RateRowResolver(RoadFreightRateRepository roadFreightRateRepository) {
        this.roadFreightRateRepository = roadFreightRateRepository;
    }

    public RoadFreightRate resolve(RateComputeRequest request, LaneResolutionResult lane, VehicleSelectionResult vehicle) {
        LoadType loadType = request.cargo().loadType();
        String loadTypeWireValue = loadType.getWireValue();
        String vehicleCategoryCode = vehicle.selectedVehicleCategoryCode();

        List<RoadFreightRate> rows = roadFreightRateRepository.findActiveRates(
                lane.laneKey(), vehicleCategoryCode, loadTypeWireValue, request.rateDate());

        if (rows.isEmpty()) {
            throw new RateNotFoundException(lane.laneKey(), vehicleCategoryCode, loadTypeWireValue);
        }
        if (rows.size() == 1) {
            return rows.get(0);
        }

        Optional<RoadFreightRate> resolvedByPrecedence = resolveFlatVersusPerKmPrecedence(rows, loadType);
        if (resolvedByPrecedence.isPresent()) {
            return resolvedByPrecedence.get();
        }

        List<AmbiguousRateConfigurationException.ConflictingRow> conflicting = rows.stream()
                .map(r -> new AmbiguousRateConfigurationException.ConflictingRow(r.getId(), r.getRateBasis()))
                .toList();
        throw new AmbiguousRateConfigurationException(lane.laneKey(), vehicleCategoryCode, loadTypeWireValue, conflicting);
    }

    /**
     * The one documented multi-row resolution: exactly a {@code flat} + {@code per_km} pair on an
     * FTL lane, where {@code flat} takes precedence. Any other multi-row combination — including a
     * flat/per_km pair plus a third row, or a flat/per_km pair on a non-FTL lane — is still
     * ambiguous and falls through to {@link AmbiguousRateConfigurationException}.
     */
    private Optional<RoadFreightRate> resolveFlatVersusPerKmPrecedence(List<RoadFreightRate> rows, LoadType loadType) {
        if (loadType != LoadType.FTL || rows.size() != 2) {
            return Optional.empty();
        }

        Optional<RoadFreightRate> flat = rows.stream().filter(r -> r.getRateBasis() == RateBasis.FLAT).findFirst();
        Optional<RoadFreightRate> perKm = rows.stream().filter(r -> r.getRateBasis() == RateBasis.PER_KM).findFirst();
        if (flat.isPresent() && perKm.isPresent()) {
            return flat;
        }
        return Optional.empty();
    }
}
