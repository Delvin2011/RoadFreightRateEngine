package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RateComputeResponseSerializationTest {

    private static final String RESPONSE_JSON = """
            {
              "status": "success",
              "quote_context_id": "8f14e45f-ceea-4a44-b1a0-5c1f8e9b2a3d",
              "rate_snapshot_id": "9e8d7c6b-5a4f-4e3d-8c2b-1a0f9e8d7c6b",
              "computed_at": "2025-07-14T09:43:11Z",
              "vehicle_selected": "tanker",
              "distance_km": 1842.5,
              "chargeable_weight_kg": 18500,
              "requires_manual_review": false,
              "flags": ["HAZMAT_CLASS_3", "CROSS_BORDER_ZIM"],
              "line_items": [
                { "code": "BASE_FREIGHT", "description": "Road freight — JHB to Harare", "buy_zar": 14200.00, "sell_zar": 14200.00 },
                { "code": "FUEL_LEVY", "description": "Fuel levy (22%)", "buy_zar": 3124.00, "sell_zar": 3124.00 },
                { "code": "HAZMAT_CL3", "description": "Hazmat surcharge — Class 3", "buy_zar": 5325.00, "sell_zar": 5325.00 },
                { "code": "BORDER_CLEARANCE_ZIM", "description": "Beit Bridge clearing fee", "buy_zar": 2850.00, "sell_zar": 2850.00 },
                { "code": "ZINARA_ROAD_FEE", "description": "ZINARA road access (USD converted)", "buy_zar": 1480.00, "sell_zar": 1480.00 },
                { "code": "COMESA_INSURANCE", "description": "COMESA Yellow Card", "buy_zar": 680.00, "sell_zar": 680.00 },
                { "code": "SERVICE_MULTIPLIER", "description": "Standard service uplift (1.15×)", "buy_zar": 0, "sell_zar": 4099.35 }
              ],
              "totals": {
                "subtotal_buy_zar": 27659.00,
                "subtotal_sell_zar": 31808.35,
                "vat_zar": 4771.25,
                "total_sell_incl_vat_zar": 36579.60,
                "margin_pct": null
              },
              "exchange_rates_used": { "USD_ZAR": 18.52 }
            }
            """;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    void deserializesKeyFieldsFromContractExample() throws Exception {
        RateComputeResponse response = objectMapper.readValue(RESPONSE_JSON, RateComputeResponse.class);

        assertThat(response.status()).isEqualTo("success");
        assertThat(response.quoteContextId()).isEqualTo(UUID.fromString("8f14e45f-ceea-4a44-b1a0-5c1f8e9b2a3d"));
        assertThat(response.rateSnapshotId()).isEqualTo(UUID.fromString("9e8d7c6b-5a4f-4e3d-8c2b-1a0f9e8d7c6b"));
        assertThat(response.computedAt()).isEqualTo(Instant.parse("2025-07-14T09:43:11Z"));
        assertThat(response.vehicleSelected()).isEqualTo("tanker");
        assertThat(response.distanceKm()).isEqualByComparingTo(new BigDecimal("1842.5"));
        assertThat(response.requiresManualReview()).isFalse();
        assertThat(response.flags()).containsExactly("HAZMAT_CLASS_3", "CROSS_BORDER_ZIM");

        assertThat(response.lineItems()).hasSize(7);
        assertThat(response.lineItems().get(0).code()).isEqualTo("BASE_FREIGHT");
        assertThat(response.lineItems().get(6).code()).isEqualTo("SERVICE_MULTIPLIER");
        assertThat(response.lineItems().get(6).buyZar()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.lineItems().get(6).sellZar()).isEqualByComparingTo(new BigDecimal("4099.35"));

        assertThat(response.totals().subtotalBuyZar()).isEqualByComparingTo(new BigDecimal("27659.00"));
        assertThat(response.totals().totalSellInclVatZar()).isEqualByComparingTo(new BigDecimal("36579.60"));
        // the engine never set a margin for this quote — a real business signal, not an omission
        assertThat(response.totals().marginPct()).isNull();

        assertThat(response.exchangeRatesUsed()).containsEntry("USD_ZAR", new BigDecimal("18.52"));
    }

    @Test
    void roundTripsWithoutLosingData() throws Exception {
        RateComputeResponse original = objectMapper.readValue(RESPONSE_JSON, RateComputeResponse.class);

        String reserialized = objectMapper.writeValueAsString(original);
        RateComputeResponse roundTripped = objectMapper.readValue(reserialized, RateComputeResponse.class);

        assertThat(roundTripped).isEqualTo(original);
        assertThat(roundTripped.totals().marginPct()).isNull();
        assertThat(reserialized).contains("\"margin_pct\":null");
    }
}
