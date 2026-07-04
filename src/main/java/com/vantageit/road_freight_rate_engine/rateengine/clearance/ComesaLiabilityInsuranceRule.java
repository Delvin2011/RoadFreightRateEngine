package com.vantageit.road_freight_rate_engine.rateengine.clearance;

import com.vantageit.road_freight_rate_engine.rateengine.api.dto.LineItem;
import com.vantageit.road_freight_rate_engine.rateengine.api.dto.RouteType;
import com.vantageit.road_freight_rate_engine.rateengine.domain.entity.SurchargeRate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Third-party liability insurance (COMESA Yellow Card) — always applies for cross-border. */
@Component
public class ComesaLiabilityInsuranceRule implements ClearanceRule {

    @Override
    public String surchargeCode() {
        return "COMESA_LIABILITY_INSURANCE";
    }

    @Override
    public boolean isApplicable(ClearanceContext context) {
        return context.request().route().routeType() == RouteType.CROSS_BORDER;
    }

    @Override
    public LineItem compute(ClearanceContext context, SurchargeRate rateRow) {
        BigDecimal amount = rateRow.getRateValue().setScale(2, RoundingMode.HALF_UP);
        return new LineItem("COMESA_LIABILITY_INSURANCE", "Third-party liability insurance (COMESA Yellow Card)", amount, amount);
    }
}
