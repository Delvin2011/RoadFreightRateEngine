package com.vantageit.road_freight_rate_engine.rateengine.laneresolution;

import java.util.UUID;
import lombok.Getter;

/**
 * A {@code Location} row exists but its {@code zone_id} doesn't resolve to a known {@code Zone}.
 * Shouldn't happen given the FK constraint — defends against orphaned data or migration issues.
 */
@Getter
public class UnmappedZoneException extends RuntimeException {

    private final UUID locationId;
    private final UUID zoneId;
    private final LocationRole role;

    public UnmappedZoneException(UUID locationId, UUID zoneId, LocationRole role) {
        super("Location %s (%s) references unknown zone_id: %s".formatted(locationId, role.name().toLowerCase(), zoneId));
        this.locationId = locationId;
        this.zoneId = zoneId;
        this.role = role;
    }
}
