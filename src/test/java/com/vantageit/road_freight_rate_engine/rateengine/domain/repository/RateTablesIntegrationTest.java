package com.vantageit.road_freight_rate_engine.rateengine.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.RoadFreightRate;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies Stage 2 end to end: the application context loads (which requires Flyway to run
 * V1-V5 without error) and the seeded rows in V5 are reachable through the date-bounded active
 * rate/surcharge lookups.
 */
@SpringBootTest
class RateTablesIntegrationTest {

    private static final String SEEDED_LANE_KEY = "JHB_METRO:BFN_METRO";
    private static final String SEEDED_VEHICLE_CATEGORY_CODE = "34T_SEMI";
    private static final String SEEDED_LOAD_TYPE = "ftl";
    private static final LocalDate SEEDED_EFFECTIVE_FROM = LocalDate.of(2025, 1, 1);

    @Autowired
    private RoadFreightRateRepository roadFreightRateRepository;

    @Autowired
    private SurchargeRateRepository surchargeRateRepository;

    @Test
    void findsActiveRateForSeededLane() {
        RoadFreightRate rate = roadFreightRateRepository
                .findActiveRate(SEEDED_LANE_KEY, SEEDED_VEHICLE_CATEGORY_CODE, SEEDED_LOAD_TYPE, LocalDate.of(2025, 6, 1))
                .orElseThrow(() -> new AssertionError("Expected an active rate for the seeded lane"));

        assertThat(rate.getRateValue()).isEqualByComparingTo(new BigDecimal("18.5000"));
        assertThat(rate.getCurrency()).isEqualTo("ZAR");
        assertThat(rate.isActive()).isTrue();
    }

    @Test
    void findsActiveFuelLevySurcharge() {
        SurchargeRate surcharge = surchargeRateRepository
                .findActiveSurcharges("FUEL_LEVY", LocalDate.of(2025, 6, 1))
                .orElseThrow(() -> new AssertionError("Expected an active FUEL_LEVY surcharge"));

        assertThat(surcharge.getRateValue()).isEqualByComparingTo(new BigDecimal("0.2200"));
        assertThat(surcharge.getSurchargeCode()).isEqualTo("FUEL_LEVY");
    }

    @Test
    void findsNoActiveRateBeforeEffectiveFrom() {
        var result = roadFreightRateRepository.findActiveRate(
                SEEDED_LANE_KEY,
                SEEDED_VEHICLE_CATEGORY_CODE,
                SEEDED_LOAD_TYPE,
                SEEDED_EFFECTIVE_FROM.minusDays(1));

        assertThat(result).isEmpty();
    }

    @Test
    void findsNoActiveSurchargeBeforeEffectiveFrom() {
        var result = surchargeRateRepository.findActiveSurcharges("FUEL_LEVY", SEEDED_EFFECTIVE_FROM.minusDays(1));

        assertThat(result).isEmpty();
    }
}
