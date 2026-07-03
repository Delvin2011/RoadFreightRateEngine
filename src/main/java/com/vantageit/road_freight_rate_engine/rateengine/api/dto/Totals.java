package com.vantageit.road_freight_rate_engine.rateengine.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * @param marginPct nullable — omitted when margin isn't disclosed for this quote
 */
public record Totals(
        @JsonProperty("subtotal_buy_zar") BigDecimal subtotalBuyZar,
        @JsonProperty("subtotal_sell_zar") BigDecimal subtotalSellZar,
        @JsonProperty("vat_zar") BigDecimal vatZar,
        @JsonProperty("total_sell_incl_vat_zar") BigDecimal totalSellInclVatZar,
        @JsonProperty("margin_pct") BigDecimal marginPct
) {
}
