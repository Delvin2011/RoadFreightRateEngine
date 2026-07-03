package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response payload for {@code POST /api/v1/rate-engine/compute}.
 *
 * @param rateSnapshotId       identifies the pinned rate/exchange-rate data used for this computation,
 *                             for audit and reproducibility
 * @param requiresManualReview true when the pipeline flagged this quote for human review before it can be sent
 * @param exchangeRatesUsed    currency code to ZAR rate, for every foreign-currency rate applied
 */
public record RateComputeResponse(
        String status,
        @JsonProperty("quote_context_id") UUID quoteContextId,
        @JsonProperty("rate_snapshot_id") UUID rateSnapshotId,
        @JsonProperty("computed_at") Instant computedAt,
        @JsonProperty("vehicle_selected") String vehicleSelected,
        @JsonProperty("distance_km") BigDecimal distanceKm,
        @JsonProperty("chargeable_weight_kg") BigDecimal chargeableWeightKg,
        @JsonProperty("requires_manual_review") boolean requiresManualReview,
        List<String> flags,
        @JsonProperty("line_items") List<LineItem> lineItems,
        Totals totals,
        @JsonProperty("exchange_rates_used") Map<String, BigDecimal> exchangeRatesUsed
) {
}
