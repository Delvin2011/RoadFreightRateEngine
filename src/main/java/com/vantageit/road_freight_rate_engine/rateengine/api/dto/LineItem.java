package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * A single cost line, reused across every pipeline stage (base freight, surcharges, clearances,
 * accessorials).
 *
 * @param code identifies which pipeline stage/rule produced this line
 */
public record LineItem(
        String code,
        String description,
        @JsonProperty("buy_zar") BigDecimal buyZar,
        @JsonProperty("sell_zar") BigDecimal sellZar
) {
}
