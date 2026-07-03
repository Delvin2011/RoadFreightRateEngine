package com.vantageit.road_freight_rate_engine.rateengine.domain.repository;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RouteApplicability;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SurchargeRateRepository extends JpaRepository<SurchargeRate, UUID> {

    /**
     * The active-surcharge-as-of-date lookup for a single surcharge code: exactly one row should
     * satisfy this, since active rates for the same code must not overlap.
     */
    default Optional<SurchargeRate> findActiveSurcharges(String surchargeCode, LocalDate asOfDate) {
        return findActiveSurcharges(surchargeCode, true, asOfDate);
    }

    /**
     * {@code active} is bound as a real JDBC parameter rather than inlined as a JPQL boolean
     * literal: Hibernate's literal rendering for a SQL Server BIT column produces SQL that SQL
     * Server itself rejects (a bare {@code is_active} predicate, or a literal {@code true} token,
     * are both invalid there even though H2's compatibility mode accepts them).
     */
    @Query("""
            SELECT s FROM SurchargeRate s
            WHERE s.surchargeCode = :surchargeCode
              AND s.active = :active
              AND s.effectiveFrom <= :asOfDate
              AND (s.effectiveTo IS NULL OR s.effectiveTo >= :asOfDate)
            """)
    Optional<SurchargeRate> findActiveSurcharges(
            @Param("surchargeCode") String surchargeCode,
            @Param("active") boolean active,
            @Param("asOfDate") LocalDate asOfDate);

    /**
     * All active surcharges applicable to a route type and cargo class as of a date.
     * {@code appliesToRouteTypes = BOTH} always matches; a null {@code appliesToCargoClasses}
     * means the surcharge applies to every cargo class.
     */
    default List<SurchargeRate> findApplicableSurcharges(
            RouteApplicability routeType, String cargoClass, LocalDate asOfDate) {
        return findApplicableSurcharges(routeType, cargoClass, true, asOfDate);
    }

    /** See {@link #findActiveSurcharges(String, LocalDate)} for why {@code active} is a bound parameter. */
    @Query("""
            SELECT s FROM SurchargeRate s
            WHERE s.active = :active
              AND s.effectiveFrom <= :asOfDate
              AND (s.effectiveTo IS NULL OR s.effectiveTo >= :asOfDate)
              AND (s.appliesToRouteTypes = :routeType OR s.appliesToRouteTypes = com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RouteApplicability.BOTH)
              AND (s.appliesToCargoClasses IS NULL OR s.appliesToCargoClasses LIKE CONCAT('%', :cargoClass, '%'))
            """)
    List<SurchargeRate> findApplicableSurcharges(
            @Param("routeType") RouteApplicability routeType,
            @Param("cargoClass") String cargoClass,
            @Param("active") boolean active,
            @Param("asOfDate") LocalDate asOfDate);
}
