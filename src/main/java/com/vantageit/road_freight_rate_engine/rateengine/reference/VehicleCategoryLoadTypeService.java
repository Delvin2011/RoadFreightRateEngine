package com.vantageit.road_freight_rate_engine.rateengine.reference;

import com.vantageit.road_freight_rate_engine.common.exception.ResourceNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.VehicleCategory;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.VehicleCategoryLoadType;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.VehicleCategoryLoadTypeRepository;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.VehicleCategoryRepository;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.VehicleCategoryLoadTypeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.VehicleCategoryLoadTypeResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehicleCategoryLoadTypeService {

    private final VehicleCategoryLoadTypeRepository vehicleCategoryLoadTypeRepository;
    private final VehicleCategoryRepository vehicleCategoryRepository;

    public VehicleCategoryLoadTypeService(VehicleCategoryLoadTypeRepository vehicleCategoryLoadTypeRepository,
            VehicleCategoryRepository vehicleCategoryRepository) {
        this.vehicleCategoryLoadTypeRepository = vehicleCategoryLoadTypeRepository;
        this.vehicleCategoryRepository = vehicleCategoryRepository;
    }

    public VehicleCategoryLoadTypeResponse create(VehicleCategoryLoadTypeRequest request) {
        VehicleCategoryLoadType entity = VehicleCategoryLoadType.builder()
                .vehicleCategory(findVehicleCategoryOrThrow(request.vehicleCategoryId()))
                .loadType(request.loadType())
                .build();
        return toResponse(vehicleCategoryLoadTypeRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<VehicleCategoryLoadTypeResponse> getAll() {
        return vehicleCategoryLoadTypeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VehicleCategoryLoadTypeResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public VehicleCategoryLoadTypeResponse update(UUID id, VehicleCategoryLoadTypeRequest request) {
        VehicleCategoryLoadType entity = findOrThrow(id);
        entity.setVehicleCategory(findVehicleCategoryOrThrow(request.vehicleCategoryId()));
        entity.setLoadType(request.loadType());
        return toResponse(vehicleCategoryLoadTypeRepository.save(entity));
    }

    public void delete(UUID id) {
        vehicleCategoryLoadTypeRepository.delete(findOrThrow(id));
    }

    private VehicleCategoryLoadType findOrThrow(UUID id) {
        return vehicleCategoryLoadTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleCategoryLoadType", id));
    }

    private VehicleCategory findVehicleCategoryOrThrow(UUID vehicleCategoryId) {
        return vehicleCategoryRepository.findById(vehicleCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("VehicleCategory", vehicleCategoryId));
    }

    private VehicleCategoryLoadTypeResponse toResponse(VehicleCategoryLoadType entity) {
        return new VehicleCategoryLoadTypeResponse(entity.getId(), entity.getVehicleCategory().getId(), entity.getLoadType());
    }
}
