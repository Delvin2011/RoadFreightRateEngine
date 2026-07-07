package com.vantageit.road_freight_rate_engine.rateengine.reference;

import com.vantageit.road_freight_rate_engine.common.exception.ResourceNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.Location;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.Zone;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.LocationRepository;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.ZoneRepository;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.LocationRequest;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.LocationResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LocationService {

    private final LocationRepository locationRepository;
    private final ZoneRepository zoneRepository;

    public LocationService(LocationRepository locationRepository, ZoneRepository zoneRepository) {
        this.locationRepository = locationRepository;
        this.zoneRepository = zoneRepository;
    }

    public LocationResponse create(LocationRequest request) {
        Location location = Location.builder()
                .zone(findZoneOrThrow(request.zoneId()))
                .name(request.name())
                .address(request.address())
                .locationType(request.locationType())
                .createdAt(Instant.now())
                .build();
        return toResponse(locationRepository.save(location));
    }

    @Transactional(readOnly = true)
    public List<LocationResponse> getAll() {
        return locationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LocationResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public LocationResponse update(UUID id, LocationRequest request) {
        Location location = findOrThrow(id);
        location.setZone(findZoneOrThrow(request.zoneId()));
        location.setName(request.name());
        location.setAddress(request.address());
        location.setLocationType(request.locationType());
        return toResponse(locationRepository.save(location));
    }

    public void delete(UUID id) {
        locationRepository.delete(findOrThrow(id));
    }

    private Location findOrThrow(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", id));
    }

    private Zone findZoneOrThrow(UUID zoneId) {
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", zoneId));
    }

    private LocationResponse toResponse(Location location) {
        return new LocationResponse(
                location.getId(),
                location.getZone().getId(),
                location.getName(),
                location.getAddress(),
                location.getLocationType(),
                location.getCreatedAt());
    }
}
