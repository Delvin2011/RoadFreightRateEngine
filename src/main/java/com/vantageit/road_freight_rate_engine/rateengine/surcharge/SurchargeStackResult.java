package com.vantageit.road_freight_rate_engine.rateengine.surcharge;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import java.math.BigDecimal;
import java.util.List;

/**
 * @param surchargesTotal sum of {@code sellZar} across every applicable line item — feeds directly
 *                        into Stage 7's {@code PreMultiplierTotals.surchargesTotal}, closing that
 *                        loop (Stage 7 previously only ever saw zero here, since this stage didn't
 *                        exist yet)
 */
public record SurchargeStackResult(
        List<LineItem> lineItems,
        BigDecimal surchargesTotal
) {
}
