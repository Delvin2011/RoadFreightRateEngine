package com.vantageit.road_freight_rate_engine.rateengine.laneresolution;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteRequest;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.Location;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.Zone;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.LaneDistanceRepository;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.LocationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pipeline Stage 2: lane resolution & distance computation.
 *
 * <p><b>Precondition: the request must have already passed Stage 3 ({@code InputValidationService})
 * validation.</b> This service does not re-check anything Stage 3 already guarantees — e.g. it
 * assumes {@code route.border_post_id} is non-null whenever {@code route.route_type} is
 * {@code cross_border}, and does not re-verify that itself.
 */
@Service
@Transactional(readOnly = true)
public class LaneResolutionService {

    private final LocationRepository locationRepository;
    private final LaneDistanceRepository laneDistanceRepository;

    public LaneResolutionService(LocationRepository locationRepository, LaneDistanceRepository laneDistanceRepository) {
        this.locationRepository = locationRepository;
        this.laneDistanceRepository = laneDistanceRepository;
    }

    public LaneResolutionResult resolve(RateComputeRequest request) {
        RouteRequest route = request.route();

        Zone originZone = resolveZone(route.originLocationId(), LocationRole.ORIGIN);
        Zone destinationZone = resolveZone(route.destinationLocationId(), LocationRole.DESTINATION);

        String laneKey = "%s:%s".formatted(originZone.getCode(), destinationZone.getCode());

        BigDecimal distanceKm;
        boolean distanceOverrideApplied;
        if (route.distanceKm() != null) {
            // Operator-supplied override, used verbatim; bypasses the lane_distances lookup
            // entirely, including for border-post-specific distances. The Business Rules tab's
            // "reason field required" note is deliberately NOT enforced here: RouteRequest has no
            // distance_override_reason field, and adding one properly means a Stage 1 DTO change
            // plus an unspecified Stage 3 validation rule (when is it required? what error code?)
            // — real cross-stage scope beyond this pipeline stage. Decision: deferred, tracked as
            // a project memory rather than left as an open-ended comment.
            distanceKm = route.distanceKm();
            distanceOverrideApplied = true;
        } else {
            distanceKm = laneDistanceRepository
                    .findActiveDistance(originZone.getId(), destinationZone.getId(), route.borderPostId())
                    .orElseThrow(() -> new DistanceNotFoundException(laneKey, route.borderPostId()))
                    .getDistanceKm();
            distanceOverrideApplied = false;
        }

        return new LaneResolutionResult(laneKey, distanceKm, originZone.getId(), destinationZone.getId(), distanceOverrideApplied);
    }

    private Zone resolveZone(UUID locationId, LocationRole role) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new UnknownLocationException(locationId, role));

        Zone zone = location.getZone();
        UUID zoneId = zone.getId(); // available on the lazy proxy without triggering initialization
        try {
            zone.getCode(); // force initialization of the lazy proxy so a dangling FK surfaces here
            return zone;
        } catch (EntityNotFoundException e) {
            throw new UnmappedZoneException(locationId, zoneId, role);
        }
    }
}
