package com.vantageit.road_freight_rate_engine.rateengine.domain.repository;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.LaneDistance;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LaneDistanceRepository extends JpaRepository<LaneDistance, UUID> {

    /**
     * {@code borderPostId} is deliberately part of the lookup key, not just the zone pair: the
     * same origin/destination zones can have a domestic distance (null border post) and one or
     * more cross-border distances (different border posts) as separate rows.
     */
    default Optional<LaneDistance> findActiveDistance(UUID originZoneId, UUID destinationZoneId, UUID borderPostId) {
        return findActiveDistance(originZoneId, destinationZoneId, borderPostId, true);
    }

    /**
     * Uses an explicit {@code LEFT JOIN} rather than the implicit path navigation
     * {@code ld.borderPost.id} — an implicit path join compiles to an INNER JOIN, which would
     * silently exclude every domestic (null border post) row from ever matching, regardless of
     * the {@code :borderPostId IS NULL} branch below. {@code active} is bound as a real parameter
     * rather than inlined, per the same SQL Server BIT-literal issue documented on
     * {@link RoadFreightRateRepository}.
     */
    @Query("""
            SELECT ld FROM LaneDistance ld
            LEFT JOIN ld.borderPost bp
            WHERE ld.originZone.id = :originZoneId
              AND ld.destinationZone.id = :destinationZoneId
              AND ((:borderPostId IS NULL AND bp IS NULL) OR bp.id = :borderPostId)
              AND ld.active = :active
            """)
    Optional<LaneDistance> findActiveDistance(
            @Param("originZoneId") UUID originZoneId,
            @Param("destinationZoneId") UUID destinationZoneId,
            @Param("borderPostId") UUID borderPostId,
            @Param("active") boolean active);
}
