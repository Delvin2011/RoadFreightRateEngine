package com.vantageit.road_freight_rate_engine.rateengine.laneresolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.AddressType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteType;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.Location;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.Zone;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.LaneDistanceRepository;
import com.vantageit.road_freight_rate_engine.rateengine.domain.repository.LocationRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Covers the one path {@link LaneResolutionServiceTest} (a real DB integration test) can't reach:
 * a {@code Location} whose {@code zone_id} doesn't resolve to a known {@code Zone}. The real FK
 * constraint on {@code locations.zone_id} makes this unreachable with genuine seed data, so it's
 * exercised here with a mocked {@link Zone} standing in for a Hibernate lazy proxy backed by a
 * dangling reference.
 */
@ExtendWith(MockitoExtension.class)
class LaneResolutionServiceUnitTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private LaneDistanceRepository laneDistanceRepository;

    @Mock
    private Zone zone;

    @Test
    void danglingZoneReferenceThrowsUnmappedZoneException() {
        LaneResolutionService service = new LaneResolutionService(locationRepository, laneDistanceRepository);

        UUID locationId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();

        // getId() is available on a Hibernate lazy proxy without triggering a DB round trip;
        // getCode() forces initialization, which is where a dangling FK would actually surface.
        when(zone.getId()).thenReturn(zoneId);
        when(zone.getCode()).thenThrow(new EntityNotFoundException("no such zone: " + zoneId));

        Location location = Location.builder()
                .id(locationId)
                .zone(zone)
                .name("Orphaned Location")
                .build();
        when(locationRepository.findById(locationId)).thenReturn(Optional.of(location));

        RouteRequest route = new RouteRequest(
                locationId, UUID.randomUUID(), RouteType.DOMESTIC, null, null, AddressType.DEPOT, AddressType.DEPOT);
        RateComputeRequest request = new RateComputeRequest(UUID.randomUUID(), LocalDate.of(2025, 7, 15), route, null, null);

        assertThatThrownBy(() -> service.resolve(request))
                .isInstanceOf(UnmappedZoneException.class)
                .satisfies(e -> {
                    UnmappedZoneException ex = (UnmappedZoneException) e;
                    assertThat(ex.getLocationId()).isEqualTo(locationId);
                    assertThat(ex.getZoneId()).isEqualTo(zoneId);
                    assertThat(ex.getRole()).isEqualTo(LocationRole.ORIGIN);
                });
    }
}
