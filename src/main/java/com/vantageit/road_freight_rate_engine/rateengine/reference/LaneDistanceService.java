package com.vantageit.road_freight_rate_engine.rateengine.reference;

import com.vantageit.road_freight_rate_engine.common.exception.ResourceNotFoundException;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.BorderPost;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.LaneDistance;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.Zone;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.BorderPostRepository;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.LaneDistanceRepository;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.ZoneRepository;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.LaneDistanceRequest;
import com.vantageit.road_freight_rate_engine.rateengine.reference.dto.LaneDistanceResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LaneDistanceService {

    private final LaneDistanceRepository laneDistanceRepository;
    private final ZoneRepository zoneRepository;
    private final BorderPostRepository borderPostRepository;

    public LaneDistanceService(LaneDistanceRepository laneDistanceRepository, ZoneRepository zoneRepository,
            BorderPostRepository borderPostRepository) {
        this.laneDistanceRepository = laneDistanceRepository;
        this.zoneRepository = zoneRepository;
        this.borderPostRepository = borderPostRepository;
    }

    public LaneDistanceResponse create(LaneDistanceRequest request) {
        LaneDistance laneDistance = LaneDistance.builder()
                .originZone(findZoneOrThrow(request.originZoneId()))
                .destinationZone(findZoneOrThrow(request.destinationZoneId()))
                .borderPost(findBorderPostOrNull(request.borderPostId()))
                .distanceKm(request.distanceKm())
                .active(request.active())
                .createdAt(Instant.now())
                .build();
        return toResponse(laneDistanceRepository.save(laneDistance));
    }

    @Transactional(readOnly = true)
    public List<LaneDistanceResponse> getAll() {
        return laneDistanceRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public LaneDistanceResponse getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public LaneDistanceResponse update(UUID id, LaneDistanceRequest request) {
        LaneDistance laneDistance = findOrThrow(id);
        laneDistance.setOriginZone(findZoneOrThrow(request.originZoneId()));
        laneDistance.setDestinationZone(findZoneOrThrow(request.destinationZoneId()));
        laneDistance.setBorderPost(findBorderPostOrNull(request.borderPostId()));
        laneDistance.setDistanceKm(request.distanceKm());
        laneDistance.setActive(request.active());
        return toResponse(laneDistanceRepository.save(laneDistance));
    }

    public void delete(UUID id) {
        laneDistanceRepository.delete(findOrThrow(id));
    }

    private LaneDistance findOrThrow(UUID id) {
        return laneDistanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LaneDistance", id));
    }

    private Zone findZoneOrThrow(UUID zoneId) {
        return zoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone", zoneId));
    }

    private BorderPost findBorderPostOrNull(UUID borderPostId) {
        if (borderPostId == null) {
            return null;
        }
        return borderPostRepository.findById(borderPostId)
                .orElseThrow(() -> new ResourceNotFoundException("BorderPost", borderPostId));
    }

    private LaneDistanceResponse toResponse(LaneDistance laneDistance) {
        BorderPost borderPost = laneDistance.getBorderPost();
        return new LaneDistanceResponse(
                laneDistance.getId(),
                laneDistance.getOriginZone().getId(),
                laneDistance.getDestinationZone().getId(),
                borderPost == null ? null : borderPost.getId(),
                laneDistance.getDistanceKm(),
                laneDistance.isActive(),
                laneDistance.getCreatedAt());
    }
}
