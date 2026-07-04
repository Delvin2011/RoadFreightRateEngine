package com.vantageit.road_freight_rate_engine.rateengine.clearance;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import java.math.BigDecimal;
import java.util.List;

/**
 * @param clearancesTotal sum of {@code sellZar} across every applicable line item — feeds directly
 *                        into Stage 7's {@code PreMultiplierTotals.clearancesTotal}, closing that
 *                        second remaining loop (Stage 9 already closed {@code surchargesTotal}).
 */
public record ClearanceResult(
        List<LineItem> lineItems,
        BigDecimal clearancesTotal
) {
}
