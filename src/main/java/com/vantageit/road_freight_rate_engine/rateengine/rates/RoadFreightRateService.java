package com.vantageit.road_freight_rate_engine.rateengine.rates;

import com.vantageit.road_freight_rate_engine.common.exception.ResourceNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RateBasis;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RoadFreightRate;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.VehicleCategory;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.RoadFreightRateRepository;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.VehicleCategoryRepository;
import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.ExpireRequest;
import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.RoadFreightRateRequest;
import com.vantageit.road_freight_rate_engine.rateengine.rates.dto.RoadFreightRateResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoadFreightRateService {

    private final RoadFreightRateRepository roadFreightRateRepository;
    private final VehicleCategoryRepository vehicleCategoryRepository;

    public RoadFreightRateService(RoadFreightRateRepository roadFreightRateRepository,
            VehicleCategoryRepository vehicleCategoryRepository) {
        this.roadFreightRateRepository = roadFreightRateRepository;
        this.vehicleCategoryRepository = vehicleCategoryRepository;
    }

    public RoadFreightRateResponse create(RoadFreightRateRequest request) {
        VehicleCategory vehicleCategory = vehicleCategoryRepository.findById(request.vehicleCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("VehicleCategory", request.vehicleCategoryId()));
        RoadFreightRate rate = RoadFreightRate.builder()
                .laneKey(request.laneKey())
                .vehicleCategory(vehicleCategory)
                .loadType(request.loadType())
                .rateBasis(RateBasis.fromWireValue(request.rateBasis()))
                .rateValue(request.rateValue())
                .currency(request.currency())
                .minimumCharge(request.minimumCharge())
                .maximumWeightKg(request.maximumWeightKg())
                .carrierId(request.carrierId())
                .effectiveFrom(request.effectiveFrom())
                .active(true)
                .createdBy(request.createdBy())
                .createdAt(Instant.now())
                .versionTag(request.versionTag())
                .build();
        return toResponse(roadFreightRateRepository.save(rate));
    }

    @Transactional(readOnly = true)
    public List<RoadFreightRateResponse> getAll() {
        return roadFreightRateRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RoadFreightRateResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public RoadFreightRateResponse expire(UUID id, ExpireRequest request) {
        RoadFreightRate rate = findOrThrow(id);
        rate.expire(request.expiryDate());
        return toResponse(roadFreightRateRepository.save(rate));
    }

    public void delete(UUID id) {
        roadFreightRateRepository.delete(findOrThrow(id));
    }

    private RoadFreightRate findOrThrow(UUID id) {
        return roadFreightRateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RoadFreightRate", id));
    }

    private RoadFreightRateResponse toResponse(RoadFreightRate rate) {
        return new RoadFreightRateResponse(
                rate.getId(),
                rate.getLaneKey(),
                rate.getVehicleCategory().getId(),
                rate.getLoadType(),
                rate.getRateBasis().getWireValue(),
                rate.getRateValue(),
                rate.getCurrency(),
                rate.getMinimumCharge(),
                rate.getMaximumWeightKg(),
                rate.getCarrierId(),
                rate.getEffectiveFrom(),
                rate.getEffectiveTo(),
                rate.isActive(),
                rate.getCreatedBy(),
                rate.getCreatedAt(),
                rate.getVersionTag());
    }
}
