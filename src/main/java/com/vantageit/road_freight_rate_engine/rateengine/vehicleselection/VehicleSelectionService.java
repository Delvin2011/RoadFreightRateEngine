package com.vantageit.road_freight_rate_engine.rateengine.vehicleselection;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.CargoRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RoadFreightRate;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.VehicleCategory;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.Zone;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.ZoneRestriction;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.RoadFreightRateRepository;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.VehicleCategoryLoadTypeRepository;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.ZoneRepository;
import com.vantageit.road_freight_rate_engine.rateengine.laneresolution.LaneResolutionResult;
import com.vantageit.road_freight_rate_engine.rateengine.pipeline.common.ChargeableWeightCalculator;
import com.vantageit.road_freight_rate_engine.rateengine.pipeline.common.ShipmentCostEstimator;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pipeline Stage 3: vehicle type selection.
 *
 * <p><b>Precondition: the request must have already passed Stage 3 validation and Stage 4 lane
 * resolution.</b> This service does not re-validate cargo/load-type compatibility — that's
 * {@code InputValidationService}'s job and is already guaranteed by the time this runs.
 */
@Service
@Transactional(readOnly = true)
public class VehicleSelectionService {

    private static final int TIER_1 = 1;

    private final VehicleCategoryLoadTypeRepository vehicleCategoryLoadTypeRepository;
    private final RoadFreightRateRepository roadFreightRateRepository;
    private final ZoneRepository zoneRepository;

    public VehicleSelectionService(
            VehicleCategoryLoadTypeRepository vehicleCategoryLoadTypeRepository,
            RoadFreightRateRepository roadFreightRateRepository,
            ZoneRepository zoneRepository) {
        this.vehicleCategoryLoadTypeRepository = vehicleCategoryLoadTypeRepository;
        this.roadFreightRateRepository = roadFreightRateRepository;
        this.zoneRepository = zoneRepository;
    }

    /**
     * Phase 1: filters {@code vehicle_categories} down to the eligible set (load type, capacity,
     * zone restriction). Throws {@link NoEligibleVehicleException} if nothing qualifies — never
     * falls back to the largest available vehicle.
     */
    public List<VehicleCategory> findEligibleVehicles(RateComputeRequest request, LaneResolutionResult lane) {
        CargoRequest cargo = request.cargo();
        BigDecimal chargeableWeightKg = ChargeableWeightCalculator.compute(cargo.grossWeightKg(), cargo.volumeCbm());
        String loadType = cargo.loadType().getWireValue();

        boolean zoneRestrictionSatisfied = isBothZonesTier1(lane.originZoneId(), lane.destinationZoneId());

        List<VehicleCategory> eligible = vehicleCategoryLoadTypeRepository.findVehicleCategoriesByLoadType(loadType).stream()
                .filter(vc -> vc.getMaxWeightKg().compareTo(chargeableWeightKg) >= 0)
                .filter(vc -> vc.getMaxVolumeCbm().compareTo(cargo.volumeCbm()) >= 0)
                .filter(vc -> vc.getZoneRestriction() != ZoneRestriction.METRO_ONLY || zoneRestrictionSatisfied)
                .toList();

        if (eligible.isEmpty()) {
            throw new NoEligibleVehicleException(chargeableWeightKg, cargo.volumeCbm(), loadType, lane.laneKey());
        }
        return eligible;
    }

    /**
     * Phase 2: selects one vehicle from the Phase 1 eligible set. {@code dedicated_vehicle = true}
     * picks the smallest-capacity viable vehicle, ignoring cost entirely; otherwise picks the
     * cheapest vehicle with an active rate for this lane. Then runs the Phase 3 capacity check on
     * whichever vehicle was selected.
     */
    public VehicleSelectionResult selectVehicle(RateComputeRequest request, LaneResolutionResult lane) {
        List<VehicleCategory> eligible = findEligibleVehicles(request, lane);
        CargoRequest cargo = request.cargo();
        BigDecimal chargeableWeightKg = ChargeableWeightCalculator.compute(cargo.grossWeightKg(), cargo.volumeCbm());

        VehicleCategory selected;
        SelectionReason reason;
        if (Boolean.TRUE.equals(request.service().dedicatedVehicle())) {
            selected = selectMinimumViable(eligible);
            reason = SelectionReason.DEDICATED_MINIMUM_VIABLE;
        } else {
            selected = selectCostEfficient(eligible, request, lane, chargeableWeightKg);
            reason = SelectionReason.COST_EFFICIENT;
        }

        checkVehicleCapacity(selected, chargeableWeightKg);

        return new VehicleSelectionResult(selected.getId(), selected.getCode(), reason, selected.isRequiresPermit(), eligible.size());
    }

    /**
     * Phase 3: defensive final check that the selected vehicle can actually carry the chargeable
     * weight. Should be unreachable given Phase 1's filtering — public so it can be exercised
     * directly against a deliberately-inconsistent input, proving the check itself works rather
     * than merely proving it's unreachable.
     */
    public void checkVehicleCapacity(VehicleCategory vehicle, BigDecimal chargeableWeightKg) {
        if (vehicle.getMaxWeightKg().compareTo(chargeableWeightKg) < 0) {
            throw new VehicleCapacityExceededException(vehicle.getCode(), vehicle.getMaxWeightKg(), chargeableWeightKg);
        }
    }

    private VehicleCategory selectMinimumViable(List<VehicleCategory> eligible) {
        // Tie-broken by code as a last resort so the pick is deterministic even when two vehicles
        // have identical weight and volume capacity (DB row order is not guaranteed otherwise).
        return eligible.stream()
                .min(Comparator.comparing(VehicleCategory::getMaxWeightKg)
                        .thenComparing(VehicleCategory::getMaxVolumeCbm)
                        .thenComparing(VehicleCategory::getCode))
                .orElseThrow(); // unreachable: eligible is never empty here (findEligibleVehicles already guarantees it)
    }

    private VehicleCategory selectCostEfficient(
            List<VehicleCategory> eligible, RateComputeRequest request, LaneResolutionResult lane, BigDecimal chargeableWeightKg) {
        CargoRequest cargo = request.cargo();
        String loadType = cargo.loadType().getWireValue();

        List<Candidate> candidates = eligible.stream()
                .map(vc -> roadFreightRateRepository
                        .findActiveRate(lane.laneKey(), vc.getCode(), loadType, request.rateDate())
                        .map(rate -> toCandidate(vc, rate, chargeableWeightKg, cargo, lane))
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();

        if (candidates.isEmpty()) {
            throw new NoRateAvailableForLaneException(lane.laneKey(), loadType, eligible.stream().map(VehicleCategory::getCode).toList());
        }

        // Tie-broken by vehicle code so the pick is deterministic even when two candidates have
        // identical computed cost (DB row order is not guaranteed otherwise).
        return candidates.stream()
                .min(Comparator.comparing(Candidate::cost).thenComparing(c -> c.vehicle().getCode()))
                .orElseThrow() // unreachable: candidates is never empty here
                .vehicle();
    }

    private Candidate toCandidate(VehicleCategory vehicle, RoadFreightRate rate, BigDecimal chargeableWeightKg, CargoRequest cargo, LaneResolutionResult lane) {
        BigDecimal cost = ShipmentCostEstimator.estimate(rate, chargeableWeightKg, cargo.volumeCbm(), lane.distanceKm(), cargo.palletCount());
        return new Candidate(vehicle, cost);
    }

    private boolean isBothZonesTier1(UUID originZoneId, UUID destinationZoneId) {
        Zone origin = zoneRepository.findById(originZoneId).orElseThrow();
        Zone destination = zoneRepository.findById(destinationZoneId).orElseThrow();
        return origin.getTier() == TIER_1 && destination.getTier() == TIER_1;
    }

    private record Candidate(VehicleCategory vehicle, BigDecimal cost) {
    }
}
