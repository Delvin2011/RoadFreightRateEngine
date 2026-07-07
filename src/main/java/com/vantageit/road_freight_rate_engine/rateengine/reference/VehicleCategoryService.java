package com.vantageit.road_freight_rate_engine.rateengine.reference;

import com.vantageit.road_freight_rate_engine.common.exception.ResourceNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.VehicleCategory;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.ZoneRestriction;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.VehicleCategoryRepository;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.VehicleCategoryRequest;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.VehicleCategoryResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleCategoryService {

    private final VehicleCategoryRepository vehicleCategoryRepository;

    public VehicleCategoryService(VehicleCategoryRepository vehicleCategoryRepository) {
        this.vehicleCategoryRepository = vehicleCategoryRepository;
    }

    public VehicleCategoryResponse create(VehicleCategoryRequest request) {
        VehicleCategory vehicleCategory = VehicleCategory.builder()
                .code(request.code())
                .name(request.name())
                .maxWeightKg(request.maxWeightKg())
                .maxVolumeCbm(request.maxVolumeCbm())
                .description(request.description())
                .zoneRestriction(parseZoneRestriction(request.zoneRestriction()))
                .requiresPermit(request.requiresPermit())
                .build();
        return toResponse(vehicleCategoryRepository.save(vehicleCategory));
    }

    @Transactional(readOnly = true)
    public List<VehicleCategoryResponse> getAll() {
        return vehicleCategoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VehicleCategoryResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public VehicleCategoryResponse update(UUID id, VehicleCategoryRequest request) {
        VehicleCategory vehicleCategory = findOrThrow(id);
        vehicleCategory.setCode(request.code());
        vehicleCategory.setName(request.name());
        vehicleCategory.setMaxWeightKg(request.maxWeightKg());
        vehicleCategory.setMaxVolumeCbm(request.maxVolumeCbm());
        vehicleCategory.setDescription(request.description());
        vehicleCategory.setZoneRestriction(parseZoneRestriction(request.zoneRestriction()));
        vehicleCategory.setRequiresPermit(request.requiresPermit());
        return toResponse(vehicleCategoryRepository.save(vehicleCategory));
    }

    public void delete(UUID id) {
        vehicleCategoryRepository.delete(findOrThrow(id));
    }

    private VehicleCategory findOrThrow(UUID id) {
        return vehicleCategoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleCategory", id));
    }

    private ZoneRestriction parseZoneRestriction(String wireValue) {
        return wireValue == null ? null : ZoneRestriction.fromWireValue(wireValue);
    }

    private VehicleCategoryResponse toResponse(VehicleCategory vehicleCategory) {
        ZoneRestriction zoneRestriction = vehicleCategory.getZoneRestriction();
        return new VehicleCategoryResponse(
                vehicleCategory.getId(),
                vehicleCategory.getCode(),
                vehicleCategory.getName(),
                vehicleCategory.getMaxWeightKg(),
                vehicleCategory.getMaxVolumeCbm(),
                vehicleCategory.getDescription(),
                zoneRestriction == null ? null : zoneRestriction.getWireValue(),
                vehicleCategory.isRequiresPermit());
    }
}
