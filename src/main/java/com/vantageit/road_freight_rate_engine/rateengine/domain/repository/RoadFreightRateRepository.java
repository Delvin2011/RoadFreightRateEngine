package com.vantageit.road_freight_rate_engine.rateengine.domain.repository;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RoadFreightRate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoadFreightRateRepository extends JpaRepository<RoadFreightRate, UUID> {

    /**
     * The active-rate-as-of-date lookup: exactly one row should satisfy this for a given lane,
     * vehicle category and load type, since active rates for the same key must not overlap.
     */
    default Optional<RoadFreightRate> findActiveRate(
            String laneKey, String vehicleCategoryCode, String loadType, LocalDate asOfDate) {
        return findActiveRate(laneKey, vehicleCategoryCode, loadType, true, asOfDate);
    }

    /**
     * Unlike {@link #findActiveRate}, more than one row can legitimately match — e.g. a lane with
     * both a {@code flat} and a {@code per_km} rate row active simultaneously. Stage 6's
     * {@code RateRowResolver} is what decides which row to use when there's more than one.
     */
    default List<RoadFreightRate> findActiveRates(
            String laneKey, String vehicleCategoryCode, String loadType, LocalDate asOfDate) {
        return findActiveRates(laneKey, vehicleCategoryCode, loadType, true, asOfDate);
    }

    @Query("""
            SELECT r FROM RoadFreightRate r
            WHERE r.laneKey = :laneKey
              AND r.vehicleCategory.code = :vehicleCategoryCode
              AND r.loadType = :loadType
              AND r.active = :active
              AND r.effectiveFrom <= :asOfDate
              AND (r.effectiveTo IS NULL OR r.effectiveTo >= :asOfDate)
            """)
    List<RoadFreightRate> findActiveRates(
            @Param("laneKey") String laneKey,
            @Param("vehicleCategoryCode") String vehicleCategoryCode,
            @Param("loadType") String loadType,
            @Param("active") boolean active,
            @Param("asOfDate") LocalDate asOfDate);

    /**
     * {@code active} is bound as a real JDBC parameter rather than inlined as a JPQL boolean
     * literal: Hibernate's literal rendering for a SQL Server BIT column produces SQL that SQL
     * Server itself rejects (a bare {@code is_active} predicate, or a literal {@code true} token,
     * are both invalid there even though H2's compatibility mode accepts them).
     */
    @Query("""
            SELECT r FROM RoadFreightRate r
            WHERE r.laneKey = :laneKey
              AND r.vehicleCategory.code = :vehicleCategoryCode
              AND r.loadType = :loadType
              AND r.active = :active
              AND r.effectiveFrom <= :asOfDate
              AND (r.effectiveTo IS NULL OR r.effectiveTo >= :asOfDate)
            """)
    Optional<RoadFreightRate> findActiveRate(
            @Param("laneKey") String laneKey,
            @Param("vehicleCategoryCode") String vehicleCategoryCode,
            @Param("loadType") String loadType,
            @Param("active") boolean active,
            @Param("asOfDate") LocalDate asOfDate);
}
