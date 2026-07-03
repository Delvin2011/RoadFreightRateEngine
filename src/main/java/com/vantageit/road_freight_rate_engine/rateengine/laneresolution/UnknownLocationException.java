package com.vantageit.road_freight_rate_engine.rateengine.laneresolution;

import java.util.UUID;
import lombok.Getter;

/** {@code origin_location_id}/{@code destination_location_id} did not resolve to any {@code Location} row. */
@Getter
public class UnknownLocationException extends RuntimeException {

    private final UUID locationId;
    private final LocationRole role;

    public UnknownLocationException(UUID locationId, LocationRole role) {
        super("Unknown %s location_id: %s".formatted(role.name().toLowerCase(), locationId));
        this.locationId = locationId;
        this.role = role;
    }
}
