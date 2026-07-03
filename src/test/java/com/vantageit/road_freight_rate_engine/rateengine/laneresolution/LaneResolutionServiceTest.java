package com.vantageit.road_freight_rate_engine.rateengine.laneresolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.AddressType;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RateComputeRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteRequest;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies Stage 4 end to end against the V7/V8 seed fixtures: JHB Depot / Bloemfontein Depot /
 * Harare Client Site / Limpopo Rural Depot locations, and their lane_distances rows (domestic,
 * cross-border via Beit Bridge, a same-zone-pair-no-border-post fixture, a same-zone-pair-second-
 * border-post fixture, and an inactive-with-no-active-alternative fixture).
 *
 * <p>cargo/service are irrelevant to {@link LaneResolutionService} (it only reads
 * {@code request.route()}) and are left null throughout.
 */
@SpringBootTest
class LaneResolutionServiceTest {

    private static final UUID JHB_DEPOT_LOCATION_ID = UUID.fromString("60000000-0000-0000-0000-000000000001");
    private static final UUID BFN_DEPOT_LOCATION_ID = UUID.fromString("60000000-0000-0000-0000-000000000002");
    private static final UUID HARARE_CLIENT_SITE_LOCATION_ID = UUID.fromString("60000000-0000-0000-0000-000000000003");
    private static final UUID LIMPOPO_RURAL_DEPOT_LOCATION_ID = UUID.fromString("60000000-0000-0000-0000-000000000004");
    private static final UUID BEIT_BRIDGE_BORDER_POST_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID TEST_BORDER_2_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Autowired
    private LaneResolutionService laneResolutionService;

    @Test
    void resolvesDomesticLaneFromDistanceMatrix() {
        RateComputeRequest request = request(route(JHB_DEPOT_LOCATION_ID, BFN_DEPOT_LOCATION_ID, RouteType.DOMESTIC, null, null));

        LaneResolutionResult result = laneResolutionService.resolve(request);

        assertThat(result.laneKey()).isEqualTo("JHB_METRO:BFN_METRO");
        assertThat(result.distanceKm()).isEqualByComparingTo(new BigDecimal("398.00"));
        assertThat(result.distanceOverrideApplied()).isFalse();
    }

    @Test
    void resolvesCrossBorderLaneFromDistanceMatrix() {
        RateComputeRequest request = request(route(
                JHB_DEPOT_LOCATION_ID, HARARE_CLIENT_SITE_LOCATION_ID, RouteType.CROSS_BORDER, BEIT_BRIDGE_BORDER_POST_ID, null));

        LaneResolutionResult result = laneResolutionService.resolve(request);

        assertThat(result.laneKey()).isEqualTo("JHB_METRO:HARARE");
        assertThat(result.distanceKm()).isEqualByComparingTo(new BigDecimal("1225.00"));
        assertThat(result.distanceOverrideApplied()).isFalse();
    }

    @Test
    void operatorOverrideIsUsedVerbatimEvenWhenADbDistanceExists() {
        BigDecimal override = new BigDecimal("350.00");
        RateComputeRequest request = request(route(JHB_DEPOT_LOCATION_ID, BFN_DEPOT_LOCATION_ID, RouteType.DOMESTIC, null, override));

        LaneResolutionResult result = laneResolutionService.resolve(request);

        assertThat(result.distanceKm()).isEqualByComparingTo(override);
        assertThat(result.distanceOverrideApplied()).isTrue();
    }

    @Test
    void unknownOriginLocationIdThrows() {
        UUID unknownId = UUID.randomUUID();
        RateComputeRequest request = request(route(unknownId, BFN_DEPOT_LOCATION_ID, RouteType.DOMESTIC, null, null));

        assertThatThrownBy(() -> laneResolutionService.resolve(request))
                .isInstanceOf(UnknownLocationException.class)
                .satisfies(e -> {
                    UnknownLocationException ex = (UnknownLocationException) e;
                    assertThat(ex.getLocationId()).isEqualTo(unknownId);
                    assertThat(ex.getRole()).isEqualTo(LocationRole.ORIGIN);
                });
    }

    @Test
    void unknownDestinationLocationIdThrows() {
        UUID unknownId = UUID.randomUUID();
        RateComputeRequest request = request(route(JHB_DEPOT_LOCATION_ID, unknownId, RouteType.DOMESTIC, null, null));

        assertThatThrownBy(() -> laneResolutionService.resolve(request))
                .isInstanceOf(UnknownLocationException.class)
                .satisfies(e -> {
                    UnknownLocationException ex = (UnknownLocationException) e;
                    assertThat(ex.getLocationId()).isEqualTo(unknownId);
                    assertThat(ex.getRole()).isEqualTo(LocationRole.DESTINATION);
                });
    }

    @Test
    void validZonesWithNoMatchingDistanceRowThrows() {
        // BFN_METRO -> HARARE has no lane_distances row seeded (only JHB_METRO -> HARARE does).
        RateComputeRequest request = request(route(
                BFN_DEPOT_LOCATION_ID, HARARE_CLIENT_SITE_LOCATION_ID, RouteType.CROSS_BORDER, BEIT_BRIDGE_BORDER_POST_ID, null));

        assertThatThrownBy(() -> laneResolutionService.resolve(request))
                .isInstanceOf(DistanceNotFoundException.class)
                .satisfies(e -> {
                    DistanceNotFoundException ex = (DistanceNotFoundException) e;
                    assertThat(ex.getLaneKey()).isEqualTo("BFN_METRO:HARARE");
                    assertThat(ex.getBorderPostId()).isEqualTo(BEIT_BRIDGE_BORDER_POST_ID);
                });
    }

    @Test
    void sameZonePairDifferentBorderPostsResolveToDifferentDistances() {
        RateComputeRequest viaBeitBridge = request(route(
                JHB_DEPOT_LOCATION_ID, HARARE_CLIENT_SITE_LOCATION_ID, RouteType.CROSS_BORDER, BEIT_BRIDGE_BORDER_POST_ID, null));
        RateComputeRequest viaNoBorderPost = request(route(
                JHB_DEPOT_LOCATION_ID, HARARE_CLIENT_SITE_LOCATION_ID, RouteType.DOMESTIC, null, null));

        LaneResolutionResult beitBridgeResult = laneResolutionService.resolve(viaBeitBridge);
        LaneResolutionResult noBorderPostResult = laneResolutionService.resolve(viaNoBorderPost);

        assertThat(beitBridgeResult.laneKey()).isEqualTo(noBorderPostResult.laneKey());
        assertThat(beitBridgeResult.distanceKm()).isEqualByComparingTo(new BigDecimal("1225.00"));
        assertThat(noBorderPostResult.distanceKm()).isEqualByComparingTo(new BigDecimal("1600.00"));
    }

    @Test
    void crossBorderOverrideBypassesLaneDistancesLookupEntirely() {
        // JHB_METRO -> HARARE via Beit Bridge has a seeded distance (1225.00) — the override must
        // win outright, not merely take precedence after a lookup also runs.
        BigDecimal override = new BigDecimal("5000.00");
        RateComputeRequest request = request(route(
                JHB_DEPOT_LOCATION_ID, HARARE_CLIENT_SITE_LOCATION_ID, RouteType.CROSS_BORDER, BEIT_BRIDGE_BORDER_POST_ID, override));

        LaneResolutionResult result = laneResolutionService.resolve(request);

        assertThat(result.laneKey()).isEqualTo("JHB_METRO:HARARE");
        assertThat(result.distanceKm()).isEqualByComparingTo(override);
        assertThat(result.distanceOverrideApplied()).isTrue();
    }

    @Test
    void secondBorderPostForSameZonePairResolvesToItsOwnDistinctDistance() {
        // Same JHB_METRO -> HARARE zone pair as the Beit Bridge (1225.00) and no-border-post
        // (1600.00) fixtures, but via a third, different border post — proves resolution is
        // genuinely keyed per border post, not just "a border post vs. none".
        RateComputeRequest request = request(route(
                JHB_DEPOT_LOCATION_ID, HARARE_CLIENT_SITE_LOCATION_ID, RouteType.CROSS_BORDER, TEST_BORDER_2_ID, null));

        LaneResolutionResult result = laneResolutionService.resolve(request);

        assertThat(result.laneKey()).isEqualTo("JHB_METRO:HARARE");
        assertThat(result.distanceKm()).isEqualByComparingTo(new BigDecimal("1400.00"));
    }

    @Test
    void inactiveDistanceRowWithNoActiveAlternativeThrowsDistanceNotFound() {
        // LIMPOPO_RURAL -> HARARE via Beit Bridge only has an is_active = 0 row seeded — must not
        // be silently returned.
        RateComputeRequest request = request(route(
                LIMPOPO_RURAL_DEPOT_LOCATION_ID, HARARE_CLIENT_SITE_LOCATION_ID, RouteType.CROSS_BORDER, BEIT_BRIDGE_BORDER_POST_ID, null));

        assertThatThrownBy(() -> laneResolutionService.resolve(request))
                .isInstanceOf(DistanceNotFoundException.class)
                .satisfies(e -> {
                    DistanceNotFoundException ex = (DistanceNotFoundException) e;
                    assertThat(ex.getLaneKey()).isEqualTo("LIMPOPO_RURAL:HARARE");
                    assertThat(ex.getBorderPostId()).isEqualTo(BEIT_BRIDGE_BORDER_POST_ID);
                });
    }

    @Test
    void laneKeyReflectsOriginDestinationOrderAndIsNotSymmetric() {
        // No lane_distances row exists for BFN_METRO -> JHB_METRO (only the reverse is seeded);
        // an override sidesteps that so this test isolates lane key construction specifically.
        RateComputeRequest forward = request(route(JHB_DEPOT_LOCATION_ID, BFN_DEPOT_LOCATION_ID, RouteType.DOMESTIC, null, null));
        RateComputeRequest reversed = request(route(
                BFN_DEPOT_LOCATION_ID, JHB_DEPOT_LOCATION_ID, RouteType.DOMESTIC, null, new BigDecimal("398.00")));

        LaneResolutionResult forwardResult = laneResolutionService.resolve(forward);
        LaneResolutionResult reversedResult = laneResolutionService.resolve(reversed);

        assertThat(forwardResult.laneKey()).isEqualTo("JHB_METRO:BFN_METRO");
        assertThat(reversedResult.laneKey()).isEqualTo("BFN_METRO:JHB_METRO");
        assertThat(forwardResult.laneKey()).isNotEqualTo(reversedResult.laneKey());
    }

    @Test
    void bothOriginAndDestinationInvalidReportsOriginFirst() {
        UUID unknownOrigin = UUID.randomUUID();
        UUID unknownDestination = UUID.randomUUID();
        RateComputeRequest request = request(route(unknownOrigin, unknownDestination, RouteType.DOMESTIC, null, null));

        assertThatThrownBy(() -> laneResolutionService.resolve(request))
                .isInstanceOf(UnknownLocationException.class)
                .satisfies(e -> {
                    UnknownLocationException ex = (UnknownLocationException) e;
                    assertThat(ex.getLocationId()).isEqualTo(unknownOrigin);
                    assertThat(ex.getLocationId()).isNotEqualTo(unknownDestination);
                    assertThat(ex.getRole()).isEqualTo(LocationRole.ORIGIN);
                });
    }

    private static RouteRequest route(UUID originLocationId, UUID destinationLocationId, RouteType routeType, UUID borderPostId, BigDecimal distanceKm) {
        return new RouteRequest(
                originLocationId,
                destinationLocationId,
                routeType,
                borderPostId,
                distanceKm,
                distanceKm != null ? "Test override reason" : null,
                AddressType.DEPOT,
                AddressType.DEPOT);
    }

    private static RateComputeRequest request(RouteRequest route) {
        return new RateComputeRequest(
                UUID.randomUUID(),
                LocalDate.of(2025, 7, 15),
                route,
                null,
                null);
    }
}
