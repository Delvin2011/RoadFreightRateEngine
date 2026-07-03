package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateComputeRequestSerializationTest {

    private static final String REQUEST_JSON = """
            {
              "quote_context_id": "8f14e45f-ceea-4a44-b1a0-5c1f8e9b2a3d",
              "rate_date": "2025-07-15",
              "route": {
                "origin_location_id": "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed",
                "destination_location_id": "3c2f1a9e-7d4b-4e6a-9c1f-2b8a7d5e6f3c",
                "route_type": "cross_border",
                "border_post_id": "a1b2c3d4-e5f6-4789-a1b2-c3d4e5f6a1b2",
                "distance_km": null,
                "distance_override_reason": null,
                "collection_address_type": "depot",
                "delivery_address_type": "door_to_door"
              },
              "cargo": {
                "cargo_class": "hazmat",
                "commodity_code": "2710.12.90",
                "gross_weight_kg": 18500,
                "volume_cbm": 22.5,
                "load_type": "tanker",
                "package_type": "bulk",
                "hazmat_un_number": "UN1203",
                "hazmat_packing_group": "II",
                "imdg_class": "3",
                "stackable": false,
                "high_value_declared": false,
                "declared_value_zar": null,
                "security_escort_required": false,
                "abnormal_load": false,
                "live_animals": false,
                "project_cargo": false
              },
              "service": {
                "service_level": "standard",
                "collection_date": "2025-07-15",
                "delivery_deadline": null,
                "after_hours_collection": false,
                "after_hours_delivery": false,
                "tail_lift_collection": false,
                "tail_lift_delivery": false,
                "driver_assist_loading": false,
                "driver_assist_offloading": false,
                "dedicated_vehicle": false,
                "security_escort_required": false
              }
            }
            """;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void deserializesKeyFieldsFromContractExample() throws Exception {
        RateComputeRequest request = objectMapper.readValue(REQUEST_JSON, RateComputeRequest.class);

        assertThat(request.quoteContextId()).isEqualTo(UUID.fromString("8f14e45f-ceea-4a44-b1a0-5c1f8e9b2a3d"));
        assertThat(request.rateDate()).isEqualTo(LocalDate.of(2025, 7, 15));

        assertThat(request.route().routeType()).isEqualTo(RouteType.CROSS_BORDER);
        assertThat(request.route().borderPostId()).isEqualTo(UUID.fromString("a1b2c3d4-e5f6-4789-a1b2-c3d4e5f6a1b2"));
        assertThat(request.route().distanceKm()).isNull();
        assertThat(request.route().distanceOverrideReason()).isNull();
        assertThat(request.route().collectionAddressType()).isEqualTo(AddressType.DEPOT);
        assertThat(request.route().deliveryAddressType()).isEqualTo(AddressType.DOOR_TO_DOOR);

        assertThat(request.cargo().cargoClass()).isEqualTo(CargoClass.HAZMAT);
        assertThat(request.cargo().grossWeightKg()).isEqualByComparingTo(new BigDecimal("18500"));
        assertThat(request.cargo().volumeCbm()).isEqualByComparingTo(new BigDecimal("22.5"));
        assertThat(request.cargo().loadType()).isEqualTo(LoadType.TANKER);
        assertThat(request.cargo().packageType()).isEqualTo(PackageType.BULK);
        assertThat(request.cargo().hazmatUnNumber()).isEqualTo("UN1203");
        assertThat(request.cargo().hazmatPackingGroup()).isEqualTo(PackingGroup.II);
        assertThat(request.cargo().imdgClass()).isEqualTo("3");
        // fields absent from the fixture must deserialize to null, not be silently defaulted
        assertThat(request.cargo().declaredValueZar()).isNull();
        assertThat(request.cargo().palletCount()).isNull();
        assertThat(request.cargo().containerType()).isNull();
        assertThat(request.cargo().dimensionsLxwxhM()).isNull();
        assertThat(request.cargo().temperatureRangeC()).isNull();

        assertThat(request.service().serviceLevel()).isEqualTo(ServiceLevel.STANDARD);
        assertThat(request.service().collectionDate()).isEqualTo(LocalDate.of(2025, 7, 15));
        assertThat(request.service().deliveryDeadline()).isNull();
    }

    @Test
    void roundTripsWithoutLosingData() throws Exception {
        RateComputeRequest original = objectMapper.readValue(REQUEST_JSON, RateComputeRequest.class);

        String reserialized = objectMapper.writeValueAsString(original);
        RateComputeRequest roundTripped = objectMapper.readValue(reserialized, RateComputeRequest.class);

        assertThat(roundTripped).isEqualTo(original);
        // explicit null checks: a lossy @JsonInclude(NON_NULL) or a missing-field bug could make
        // these silently vanish from reserialized JSON while still passing a naive equals() check
        assertThat(roundTripped.cargo().declaredValueZar()).isNull();
        assertThat(roundTripped.service().deliveryDeadline()).isNull();
        assertThat(reserialized).contains("\"declared_value_zar\":null");
        assertThat(reserialized).contains("\"delivery_deadline\":null");
    }
}
