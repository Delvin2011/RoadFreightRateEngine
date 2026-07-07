package com.vantageit.road_freight_rate_engine.rateengine.reference;

import com.vantageit.road_freight_rate_engine.common.exception.ResourceNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.Zone;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.ZoneRepository;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.ZoneRequest;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.ZoneResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ZoneService {

    private final ZoneRepository zoneRepository;

    public ZoneService(ZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    public ZoneResponse create(ZoneRequest request) {
        Zone zone = Zone.builder()
                .code(request.code())
                .name(request.name())
                .tier(request.tier())
                .countryCode(request.countryCode())
                .build();
        return toResponse(zoneRepository.save(zone));
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getAll() {
        return zoneRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ZoneResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public ZoneResponse update(UUID id, ZoneRequest request) {
        Zone zone = findOrThrow(id);
        zone.setCode(request.code());
        zone.setName(request.name());
        zone.setTier(request.tier());
        zone.setCountryCode(request.countryCode());
        return toResponse(zoneRepository.save(zone));
    }

    public void delete(UUID id) {
        zoneRepository.delete(findOrThrow(id));
    }

    private Zone findOrThrow(UUID id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", id));
    }

    private ZoneResponse toResponse(Zone zone) {
        return new ZoneResponse(zone.getId(), zone.getCode(), zone.getName(), zone.getTier(), zone.getCountryCode());
    }
}
